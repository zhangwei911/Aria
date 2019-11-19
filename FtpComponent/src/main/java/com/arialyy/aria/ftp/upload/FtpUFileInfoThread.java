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
import aria.apache.commons.net.ftp.FTPFile;
import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.ftp.AbsFtpInfoThread;
import com.arialyy.aria.core.processor.FtpInterceptHandler;
import com.arialyy.aria.core.processor.IFtpUploadInterceptor;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.DbDataHelper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aria.Lao on 2017/9/26.
 * 单任务上传远程服务器文件信息
 */
class FtpUFileInfoThread extends AbsFtpInfoThread<UploadEntity, UTaskWrapper> {
  private static final String TAG = "FtpUploadFileInfoThread";
  static final int CODE_COMPLETE = 0xab1;
  private boolean isComplete = false;
  private String remotePath;
  /**
   * true 使用拦截器，false 不使用拦截器
   */
  private boolean useInterceptor = false;

  FtpUFileInfoThread(UTaskWrapper taskEntity, OnFileInfoCallback callback) {
    super(taskEntity, callback);
  }

  @Override protected String getRemotePath() {
    return remotePath == null ?
        mTaskOption.getUrlEntity().remotePath + "/" + mEntity.getFileName() : remotePath;
  }

  @Override protected boolean onInterceptor(FTPClient client, FTPFile[] ftpFiles) {
    // 旧任务将不做处理，否则断点续传上传将失效
    if (!mTaskWrapper.isNewTask()) {
      ALog.d(TAG, "任务是旧任务，忽略该拦截器");
      return true;
    }
    try {
      IFtpUploadInterceptor interceptor = mTaskOption.getUploadInterceptor();
      if (interceptor != null) {
        useInterceptor = true;
        List<String> files = new ArrayList<>();
        for (FTPFile ftpFile : ftpFiles) {
          if (ftpFile.isDirectory()) {
            continue;
          }
          files.add(ftpFile.getName());
        }

        FtpInterceptHandler interceptHandler = interceptor.onIntercept(mEntity, files);

        /*
          处理远端有同名文件的情况
         */
        if (files.contains(mEntity.getFileName())) {
          if (interceptHandler.isCoverServerFile()) {
            ALog.i(TAG, String.format("远端已拥有同名文件，将覆盖该文件，文件名：%s", mEntity.getFileName()));
            boolean b = client.deleteFile(CommonUtil.convertFtpChar(charSet, getRemotePath()));
            ALog.d(TAG,
                String.format("删除文件%s，code: %s， msg: %s", b ? "成功" : "失败", client.getReplyCode(),
                    client.getReplyString()));
          } else if (!TextUtils.isEmpty(interceptHandler.getNewFileName())) {
            ALog.i(TAG, String.format("远端已拥有同名文件，将修改remotePath，原文件名：%s，新文件名：%s",
                mEntity.getFileName(), interceptHandler.getNewFileName()));
            remotePath = mTaskOption.getUrlEntity().remotePath
                + "/"
                + interceptHandler.getNewFileName();
            mTaskOption.setNewFileName(interceptHandler.getNewFileName());
            closeClient(client);
            run();
            return false;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * 如果服务器的文件长度和本地上传文件的文件长度一致，则任务任务已完成。
   * 否则重新修改保存的停止位置，这是因为outputStream是读不到服务器是否成功写入的。
   * 而threadTask的保存的停止位置是File的InputStream的，所有就会导致两端停止位置不一致
   *
   * @param remotePath ftp服务器文件夹路径
   * @param ftpFile ftp服务器上对应的文件
   */
  @Override protected void handleFile(String remotePath, FTPFile ftpFile) {
    super.handleFile(remotePath, ftpFile);
    if (ftpFile != null && !useInterceptor) {
      //远程文件已完成
      if (ftpFile.getSize() == mEntity.getFileSize()) {
        isComplete = true;
        ALog.d(TAG, "FTP服务器上已存在该文件【" + ftpFile.getName() + "】");
      } else {
        ALog.w(TAG, "FTP服务器已存在未完成的文件【"
            + ftpFile.getName()
            + "，size: "
            + ftpFile.getSize()
            + "】"
            + "尝试从位置："
            + (ftpFile.getSize() - 1)
            + "开始上传");
        mTaskWrapper.setNewTask(false);

        // 修改记录
        TaskRecord record = DbDataHelper.getTaskRecord(mTaskWrapper.getKey());
        if (record == null) {
          record = new TaskRecord();
          record.fileName = mEntity.getFileName();
          record.filePath = mTaskWrapper.getKey();
          record.threadRecords = new ArrayList<>();
        }
        ThreadRecord threadRecord;
        if (record.threadRecords == null || record.threadRecords.isEmpty()) {
          threadRecord = new ThreadRecord();
          threadRecord.taskKey = record.filePath;
        } else {
          threadRecord = record.threadRecords.get(0);
        }
        //修改本地保存的停止地址为服务器上对应文件的大小
        threadRecord.startLocation = ftpFile.getSize() - 1;
        record.save();
        threadRecord.save();
      }
    }
  }

  @Override protected void onPreComplete(int code) {
    super.onPreComplete(code);
    mCallback.onComplete(mEntity.getKey(),
        new CompleteInfo(isComplete ? CODE_COMPLETE : code, mTaskWrapper));
  }
}
