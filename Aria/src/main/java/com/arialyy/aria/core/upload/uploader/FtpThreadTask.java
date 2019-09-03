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
package com.arialyy.aria.core.upload.uploader;

import android.text.TextUtils;
import aria.apache.commons.net.ftp.FTPClient;
import aria.apache.commons.net.ftp.FTPReply;
import aria.apache.commons.net.ftp.OnFtpInputStreamListener;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.common.ftp.AbsFtpThreadTask;
import com.arialyy.aria.core.common.ftp.FtpTaskConfig;
import com.arialyy.aria.core.config.UploadConfig;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.BufferedRandomAccessFile;
import com.arialyy.aria.util.CommonUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Aria.Lao on 2017/7/28. D_FTP 单线程上传任务，需要FTP 服务器给用户打开append和write的权限
 */
class FtpThreadTask extends AbsFtpThreadTask<UploadEntity, UTaskWrapper> {
  private final String TAG = "FtpThreadTask";
  private String dir, remotePath;

  FtpThreadTask(SubThreadConfig<UTaskWrapper> config) {
    super(config);
  }

  @Override public int getMaxSpeed() {
    return getTaskConfig().getMaxSpeed();
  }

  @Override protected UploadConfig getTaskConfig() {
    return getTaskWrapper().getConfig();
  }

  @Override public FtpThreadTask call() throws Exception {
    super.call();
    FTPClient client = null;
    BufferedRandomAccessFile file = null;
    try {
      ALog.d(TAG,
          String.format("任务【%s】线程__%s__开始上传【开始位置 : %s，结束位置：%s】", getFileName(),
              mRecord.threadId, mRecord.startLocation, mRecord.endLocation));
      client = createClient();
      if (client == null) {
        return this;
      }
      initPath();
      client.makeDirectory(dir);
      client.changeWorkingDirectory(dir);
      client.setRestartOffset(mRecord.startLocation);
      int reply = client.getReplyCode();
      if (!FTPReply.isPositivePreliminary(reply) && reply != FTPReply.FILE_ACTION_OK) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply,
                    client.getReplyString(), getEntity().getFilePath())));
        client.disconnect();
        return this;
      }

      file =
          new BufferedRandomAccessFile(getConfig().tempFile, "rwd", getTaskConfig().getBuffSize());
      if (mRecord.startLocation != 0) {
        //file.skipBytes((int) getConfig().START_LOCATION);
        file.seek(mRecord.startLocation);
      }
      boolean complete = upload(client, file);
      if (!complete || isBreak()) {
        return this;
      }
      ALog.i(TAG, String.format("任务【%s】线程__%s__上传完毕", getFileName(), mRecord.threadId));
      writeConfig(true, mRecord.endLocation);
      sendCompleteMsg();
    } catch (IOException e) {
      fail(mChildCurrentLocation, new AriaIOException(TAG,
          String.format("上传失败，filePath: %s, uploadUrl: %s", getEntity().getFilePath(),
              getConfig().url)));
    } catch (Exception e) {
      fail(mChildCurrentLocation, new AriaIOException(TAG, null, e));
    } finally {
      try {
        if (file != null) {
          file.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      closeClient(client);
      onThreadComplete();
    }
    return this;
  }

  private void initPath() throws UnsupportedEncodingException {
    FtpTaskConfig delegate = getTaskWrapper().asFtp();
    dir = CommonUtil.convertFtpChar(charSet, delegate.getUrlEntity().remotePath);

    String fileName =
        TextUtils.isEmpty(delegate.getNewFileName()) ? CommonUtil.convertFtpChar(charSet,
            getEntity().getFileName())
            : CommonUtil.convertFtpChar(charSet, delegate.getNewFileName());

    remotePath =
        CommonUtil.convertFtpChar(charSet,
            String.format("%s/%s", delegate.getUrlEntity().remotePath, fileName));
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
            if (isBreak() && !isStoped) {
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
        fail(mChildCurrentLocation,
            new AriaIOException(TAG, msg, e));
      }
      return false;
    }

    int reply = client.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      if (reply != FTPReply.TRANSFER_ABORTED) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply,
                    client.getReplyString(), getEntity().getFilePath())), false);
      }
      if (client.isConnected()) {
        client.disconnect();
      }
      return false;
    }
    return true;
  }
}
