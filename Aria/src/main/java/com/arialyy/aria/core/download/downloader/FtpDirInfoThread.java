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
package com.arialyy.aria.core.download.downloader;

import aria.apache.commons.net.ftp.FTPFile;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.common.ftp.AbsFtpInfoThread;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.util.CommonUtil;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Created by Aria.Lao on 2017/7/25.
 * 获取ftp文件夹信息
 */
class FtpDirInfoThread extends AbsFtpInfoThread<DownloadGroupEntity, DownloadGroupTaskEntity> {

  FtpDirInfoThread(DownloadGroupTaskEntity taskEntity, OnFileInfoCallback callback) {
    super(taskEntity, callback);
  }

  @Override protected String setRemotePath() {
    return mTaskEntity.getUrlEntity().remotePath;
  }

  @Override protected void handleFile(String remotePath, FTPFile ftpFile) {
    super.handleFile(remotePath, ftpFile);
    addEntity(remotePath, ftpFile);
  }

  @Override protected void onPreComplete(int code) {
    super.onPreComplete(code);
    mEntity.setFileSize(mSize);
    mCallback.onComplete(mEntity.getKey(), new CompleteInfo(code));
  }

  /**
   * FTP文件夹的子任务实体 在这生成
   */
  private void addEntity(String remotePath, FTPFile ftpFile) {
    final FtpUrlEntity urlEntity = mTaskEntity.getUrlEntity().clone();
    DownloadEntity entity = new DownloadEntity();
    entity.setUrl(
        urlEntity.scheme + "://" + urlEntity.hostName + ":" + urlEntity.port + "/" + remotePath);
    entity.setDownloadPath(mEntity.getDirPath() + "/" + remotePath);
    int lastIndex = remotePath.lastIndexOf("/");
    String fileName = lastIndex < 0 ? CommonUtil.keyToHashKey(remotePath)
        : remotePath.substring(lastIndex + 1, remotePath.length());
    entity.setFileName(new String(fileName.getBytes(), Charset.forName(mTaskEntity.getCharSet())));
    entity.setGroupName(mEntity.getGroupName());
    entity.setGroupChild(true);
    entity.setFileSize(ftpFile.getSize());
    entity.insert();

    DownloadTaskEntity taskEntity = new DownloadTaskEntity();
    taskEntity.setKey(entity.getDownloadPath());
    taskEntity.setUrl(entity.getUrl());
    taskEntity.setEntity(entity);
    taskEntity.setGroupTask(true);
    taskEntity.setGroupName(mEntity.getGroupName());
    taskEntity.setRequestType(AbsTaskEntity.D_FTP);
    urlEntity.url = entity.getUrl();
    urlEntity.remotePath = remotePath;
    taskEntity.setUrlEntity(urlEntity);
    taskEntity.insert();

    if (mEntity.getUrls() == null) {
      mEntity.setUrls(new ArrayList<String>());
    }
    mEntity.getSubEntities().add(entity);
    mTaskEntity.getSubTaskEntities().add(taskEntity);
  }
}
