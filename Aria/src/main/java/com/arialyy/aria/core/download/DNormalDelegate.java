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
package com.arialyy.aria.core.download;

import android.text.TextUtils;
import com.arialyy.aria.core.inf.ITargetHandler;
import com.arialyy.aria.core.inf.ITargetNormal;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * Created by AriaL on 2019/4/5.
 * 普通下载任务通用功能处理
 */
class DNormalDelegate<TARGET extends AbsDownloadTarget> implements ITargetNormal<TARGET> {
  private final String TAG = "DNormalDelegate";
  private DownloadEntity mEntity;

  private TARGET target;

  DNormalDelegate(TARGET target, String url, String targetName) {
    this.target = target;
    initTarget(url, targetName);
  }

  @Override public void initTarget(String url, String targetName) {
    DTaskWrapper taskWrapper = TaskWrapperManager.getInstance().getHttpTaskWrapper(DTaskWrapper.class, url);
    mEntity = taskWrapper.getEntity();

    target.setUrl(url);
    target.setTargetName(targetName);
    target.setTaskWrapper(taskWrapper);
    target.setEntity(mEntity);
    if (mEntity != null) {
      target.setTempFilePath(mEntity.getDownloadPath());
    }
  }

  @Override public TARGET updateUrl(String newUrl) {
    if (TextUtils.isEmpty(newUrl)) {
      ALog.e(TAG, "下载url更新失败，newUrl为null");
      return target;
    }
    if (target.getUrl().equals(newUrl)) {
      ALog.e(TAG, "下载url更新失败，新的下载url和旧的url一致");
      return target;
    }
    target.setNewUrl(newUrl);
    target.getTaskWrapper().setRefreshInfo(true);
    return target;
  }

  @Override public DownloadEntity getEntity() {
    return target.getEntity();
  }

  @Override public boolean taskExists() {
    return DbEntity.checkDataExist(DownloadEntity.class, "url=?", target.getUrl());
  }

  @Override public boolean isRunning() {
    DownloadTask task = DownloadTaskQueue.getInstance().getTask(mEntity.getKey());
    return task != null && task.isRunning();
  }

  @Override public boolean checkEntity() {
    boolean b = checkUrl() && checkFilePath();
    if (b) {
      mEntity.save();
    }
    return b;
  }

  @Override public boolean checkFilePath() {
    String filePath = target.getTempFilePath();
    if (TextUtils.isEmpty(filePath)) {
      ALog.e(TAG, "下载失败，文件保存路径为null");
      return false;
    } else if (!filePath.startsWith("/")) {
      ALog.e(TAG, "下载失败，文件保存路径【" + filePath + "】错误");
      return false;
    }
    File file = new File(filePath);
    if (file.isDirectory()) {
      if (target.getTargetType() == ITargetHandler.HTTP) {
        ALog.e(TAG, "下载失败，保存路径【" + filePath + "】不能为文件夹，路径需要是完整的文件路径，如：/mnt/sdcard/game.zip");
        return false;
      } else if (target.getTargetType() == ITargetHandler.FTP) {
        filePath += mEntity.getFileName();
      }
    } else {
      // http文件名设置
      if (TextUtils.isEmpty(mEntity.getFileName())) {
        mEntity.setFileName(file.getName());
      }
    }

    //设置文件保存路径，如果新文件路径和旧文件路径不同，则修改路径
    if (!filePath.equals(mEntity.getDownloadPath())) {
      // 检查路径冲突
      if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=?", filePath)) {
        if (!target.isForceDownload()) {
          ALog.e(TAG, "下载失败，保存路径【" + filePath + "】已经被其它任务占用，请设置其它保存路径");
          return false;
        } else {
          ALog.w(TAG, "保存路径【" + filePath + "】已经被其它任务占用，当前任务将覆盖该路径的文件");
          CommonUtil.delTaskRecord(filePath, 1);
          target.setTaskWrapper(
              TaskWrapperManager.getInstance()
                  .getHttpTaskWrapper(DTaskWrapper.class, target.getUrl()));
        }
      }
      File oldFile = new File(mEntity.getDownloadPath());
      File newFile = new File(filePath);
      mEntity.setDownloadPath(filePath);
      mEntity.setFileName(newFile.getName());
      if (oldFile.exists()) {
        oldFile.renameTo(newFile);
        CommonUtil.modifyTaskRecord(oldFile.getPath(), newFile.getPath());
      }
    }
    return true;
  }

  @Override public boolean checkUrl() {
    final String url = mEntity.getUrl();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "下载失败，url为null");
      return false;
    } else if (!url.startsWith("http") && !url.startsWith("ftp")) {
      ALog.e(TAG, "下载失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "下载失败，url【" + url + "】不合法");
      return false;
    }
    if (!TextUtils.isEmpty(target.getNewUrl())) {
      mEntity.setUrl(target.getNewUrl());
    }
    return true;
  }
}
