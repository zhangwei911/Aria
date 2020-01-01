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
package com.arialyy.aria.ftp.download;

import aria.apache.commons.net.ftp.FTPFile;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.ftp.AbsFtpInfoTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.FileUtil;

/**
 * Created by Aria.Lao on 2017/7/25.
 * 获取ftp文件信息
 */
final class FtpDFileInfoTask extends AbsFtpInfoTask<DownloadEntity, DTaskWrapper> {

  FtpDFileInfoTask(DTaskWrapper taskEntity) {
    super(taskEntity);
  }

  @Override protected void handleFile(String remotePath, FTPFile ftpFile) {
    super.handleFile(remotePath, ftpFile);
    if (!FileUtil.checkMemorySpace(mEntity.getFilePath(), ftpFile.getSize())) {
      callback.onFail(mEntity, new AriaIOException(TAG,
              String.format("获取ftp文件信息失败，内存空间不足, filePath: %s", mEntity.getFilePath())),
          false);
    }
  }

  @Override protected String getRemotePath() {
    return mTaskOption.getUrlEntity().remotePath;
  }

  @Override protected void onPreComplete(int code) {
    ALog.i(TAG, "FTP下载预处理完成");
    super.onPreComplete(code);
    if (mSize != mTaskWrapper.getEntity().getFileSize()) {
      mTaskWrapper.setNewTask(true);
    }
    mEntity.setFileSize(mSize);
    callback.onSucceed(mEntity.getUrl(), new CompleteInfo(code, mTaskWrapper));
  }
}
