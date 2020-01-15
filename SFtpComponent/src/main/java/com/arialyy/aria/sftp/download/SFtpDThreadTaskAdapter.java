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
package com.arialyy.aria.sftp.download;

import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.task.AbsThreadTaskAdapter;
import com.arialyy.aria.exception.AriaException;
import com.arialyy.aria.ftp.FtpTaskOption;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.FileUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * sftp 线程任务适配器
 *
 * @author lyy
 */
final class SFtpDThreadTaskAdapter extends AbsThreadTaskAdapter {
  private ChannelSftp channelSftp;
  private Session session;
  private FtpTaskOption option;

  SFtpDThreadTaskAdapter(SubThreadConfig config) {
    super(config);
    session = (Session) config.obj;
    option = (FtpTaskOption) getTaskWrapper().getTaskOption();
  }

  @Override protected void handlerThreadTask() {
    if (session == null) {
      fail(new AriaException(TAG, "session 为空"), false);
      return;
    }
    FileOutputStream fos;
    try {
      int timeout = getTaskConfig().getConnectTimeOut();
      if (!session.isConnected()) {
        session.connect(timeout);
      }
      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect(timeout);
      fos = new FileOutputStream(getThreadConfig().tempFile, true);
      if (channelSftp.isClosed() || !channelSftp.isConnected()) {
        channelSftp.connect();
      }
      ALog.d(TAG,
          String.format("任务【%s】线程__%s__开始下载【开始位置 : %s，结束位置：%s】", getTaskWrapper().getKey(),
              getThreadRecord().threadId, getThreadRecord().startLocation,
              getThreadRecord().endLocation));

      // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码
      String charSet = option.getCharSet();
      String remotePath =
          CommonUtil.convertFtpChar(charSet, option.getUrlEntity().remotePath);
      if (getThreadRecord().startLocation > 0) {
        channelSftp.get(remotePath, fos, new Monitor(true), ChannelSftp.RESUME,
            getThreadRecord().startLocation);
      } else {
        channelSftp.get(remotePath, fos, new Monitor(false));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      channelSftp.disconnect();
    }
  }

  private class Monitor implements SftpProgressMonitor {

    private boolean isResume;

    private Monitor(boolean isResume) {
      this.isResume = isResume;
    }

    @Override public void init(int op, String src, String dest, long max) {
      ALog.d(TAG, String.format("op = %s; src = %s; dest = %s; max = %s", op, src, dest, max));
    }

    /**
     * @param count 已传输的数据
     * @return false 取消任务
     */
    @Override public boolean count(long count) {

      if (mSpeedBandUtil != null) {
        mSpeedBandUtil.limitNextBytes((int) count);
      }

      /*
       * jsch 如果是恢复任务，第一次回调count会将已下载的长度返回，后面才是新增的文件长度。
       * 所以恢复任务的话，需要忽略一次回调
       */
      if (!isResume) {
        progress(count);
      }
      isResume = false;
      //return !getThreadTask().isBreak() && getRangeProgress() < getThreadRecord().endLocation;
      if (getRangeProgress() > getThreadRecord().endLocation) {
        return false;
      }
      return !getThreadTask().isBreak();
    }

    @Override public void end() {
      if (getThreadTask().isBreak()) {
        return;
      }

      complete();

      //boolean isSuccess = true;
      //// 剪裁文件
      //if (getRangeProgress() > getThreadRecord().endLocation) {
      //  isSuccess = clipFile();
      //}
      //if (isSuccess) {
      //  complete();
      //} else {
      //  fail(new AriaException(TAG, "剪切文件失败"), false);
      //}
    }

    /**
     * 文件超出内容，剪切文件
     *
     * @return true 剪切文件成功
     */
    private boolean clipFile() {
      FileInputStream fis = null;
      FileOutputStream fos = null;
      long stime = System.currentTimeMillis();
      try {
        String destPath = getThreadConfig().tempFile.getPath();
        ALog.d(TAG, "oldSize = " + getThreadConfig().tempFile.length());
        String tempPath = destPath + "_temp";
        fis = new FileInputStream(getThreadConfig().tempFile);
        fos = new FileOutputStream(tempPath);
        FileChannel inChannel = fis.getChannel();
        FileChannel outChannel = fos.getChannel();
        inChannel.transferTo(0, getThreadRecord().endLocation, outChannel);

        FileUtil.deleteFile(getThreadConfig().tempFile);
        File oldF = new File(tempPath);
        File newF = new File(destPath);
        boolean b = oldF.renameTo(newF);
        ALog.d(TAG, String.format("剪裁文件消耗：%sms，fileSize：%s，threadId：%s",
            (System.currentTimeMillis() - stime), newF.length(),
            getThreadConfig().record.threadId));
        return b;
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {

          if (fis != null) {
            fis.close();
          }
          if (fos != null) {
            fos.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return false;
    }
  }
}
