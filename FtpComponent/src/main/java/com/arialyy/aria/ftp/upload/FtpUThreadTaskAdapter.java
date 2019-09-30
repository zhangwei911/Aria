/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.ftp.upload;

import android.text.TextUtils;
import aria.apache.commons.net.ftp.FTPClient;
import aria.apache.commons.net.ftp.FTPReply;
import aria.apache.commons.net.ftp.OnFtpInputStreamListener;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.ftp.BaseFtpThreadTaskAdapter;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.BufferedRandomAccessFile;
import com.arialyy.aria.util.CommonUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Aria.Lao on 2017/7/28. D_FTP 单线程上传任务，需要FTP 服务器给用户打开append和write的权限
 */
class FtpUThreadTaskAdapter extends BaseFtpThreadTaskAdapter {
  private final String TAG = "FtpThreadTask";
  private String dir, remotePath;

  FtpUThreadTaskAdapter(SubThreadConfig config) {
    super(config);
  }

  @Override protected void handlerThreadTask() {
    FTPClient client = null;
    BufferedRandomAccessFile file = null;
    try {
      ALog.d(TAG,
          String.format("任务【%s】线程__%s__开始上传【开始位置 : %s，结束位置：%s】", getEntity().getKey(),
              getThreadRecord().threadId, getThreadRecord().startLocation,
              getThreadRecord().endLocation));
      client = createClient();
      if (client == null) {
        return;
      }
      initPath();
      client.makeDirectory(dir);
      client.changeWorkingDirectory(dir);
      client.setRestartOffset(getThreadRecord().startLocation);
      int reply = client.getReplyCode();
      if (!FTPReply.isPositivePreliminary(reply) && reply != FTPReply.FILE_ACTION_OK) {
        fail(new AriaIOException(TAG,
            String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply,
                client.getReplyString(), getEntity().getFilePath())), false);
        client.disconnect();
        return;
      }

      file =
          new BufferedRandomAccessFile(getThreadConfig().tempFile, "rwd", getTaskConfig().getBuffSize());
      if (getThreadRecord().startLocation != 0) {
        //file.skipBytes((int) getThreadConfig().START_LOCATION);
        file.seek(getThreadRecord().startLocation);
      }
      boolean complete = upload(client, file);
      if (!complete || getThreadTask().isBreak()) {
        return;
      }
      ALog.i(TAG,
          String.format("任务【%s】线程__%s__上传完毕", getEntity().getKey(), getThreadRecord().threadId));
      complete();
    } catch (IOException e) {
      fail(new AriaIOException(TAG,
          String.format("上传失败，filePath: %s, uploadUrl: %s", getEntity().getFilePath(),
              getThreadConfig().url)), true);
    } catch (Exception e) {
      fail(new AriaIOException(TAG, null, e), false);
    } finally {
      try {
        if (file != null) {
          file.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      closeClient(client);
    }
  }

  private UploadEntity getEntity() {
    return (UploadEntity) getTaskWrapper().getEntity();
  }

  private void initPath() throws UnsupportedEncodingException {
    dir = CommonUtil.convertFtpChar(charSet, mTaskOption.getUrlEntity().remotePath);

    String fileName =
        TextUtils.isEmpty(mTaskOption.getNewFileName()) ? CommonUtil.convertFtpChar(charSet,
            getEntity().getFileName())
            : CommonUtil.convertFtpChar(charSet, mTaskOption.getNewFileName());

    remotePath =
        CommonUtil.convertFtpChar(charSet,
            String.format("%s/%s", mTaskOption.getUrlEntity().remotePath, fileName));
  }

  /**
   * 上传
   *
   * @return {@code true}上传成功、{@code false} 上传失败
   */
  private boolean upload(final FTPClient client, final BufferedRandomAccessFile bis)
      throws IOException {

    try {
      ALog.d(TAG, String.format("remotePath: %s", remotePath));
      client.storeFile(remotePath, new FtpFISAdapter(bis), new OnFtpInputStreamListener() {
        boolean isStoped = false;

        @Override public void onFtpInputStream(FTPClient client, long totalBytesTransferred,
            int bytesTransferred, long streamSize) {
          try {
            if (getThreadTask().isBreak() && !isStoped) {
              isStoped = true;
              client.abor();
            }
            if (mSpeedBandUtil != null) {
              mSpeedBandUtil.limitNextBytes(bytesTransferred);
            }
            progress(bytesTransferred);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    } catch (IOException e) {
      String msg = String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", client.getReplyCode(),
          client.getReplyString(), getEntity().getFilePath());
      if (client.isConnected()) {
        client.disconnect();
      }
      if (e.getMessage().contains("AriaIOException caught while copying")) {
        e.printStackTrace();
      } else {
        fail(new AriaIOException(TAG, msg, e), true);
      }
      return false;
    }

    int reply = client.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      if (reply != FTPReply.TRANSFER_ABORTED) {
        fail(new AriaIOException(TAG,
            String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply, client.getReplyString(),
                getEntity().getFilePath())), false);
      }
      if (client.isConnected()) {
        client.disconnect();
      }
      return false;
    }
    return true;
  }
}
