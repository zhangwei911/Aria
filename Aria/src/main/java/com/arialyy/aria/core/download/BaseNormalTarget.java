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
import com.arialyy.aria.core.manager.TEManager;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * Created by Aria.Lao on 2017/7/26.
 */
abstract class BaseNormalTarget<TARGET extends BaseNormalTarget>
    extends AbsDownloadTarget<TARGET, DownloadEntity, DownloadTaskEntity> {

  /**
   * 资源地址
   */
  protected String url, newUrl;

  /**
   * 通过地址初始化target
   */
  void initTarget(String url, String targetName) {
    this.url = url;
    mTargetName = targetName;
    mTaskEntity = TEManager.getInstance().getTEntity(DownloadTaskEntity.class, url);
    mEntity = mTaskEntity.getEntity();

    if (mEntity != null) {
      mTempFilePath = mEntity.getDownloadPath();
    }
  }

  /**
   * 更新下载url
   *
   * @param newUrl 新的下载url
   */
  public TARGET updateUrl(String newUrl) {
    if (TextUtils.isEmpty(newUrl)) {
      ALog.e(TAG, "下载url更新失败，newUrl为null");
      return (TARGET) this;
    }
    if (url.equals(newUrl)) {
      ALog.e(TAG, "下载url更新失败，新的下载url和旧的url一致");
      return (TARGET) this;
    }
    this.newUrl = newUrl;
    mTaskEntity.setRefreshInfo(true);
    return (TARGET) this;
  }

  /**
   * 将任务设置为最高优先级任务，最高优先级任务有以下特点：
   * 1、在下载队列中，有且只有一个最高优先级任务
   * 2、最高优先级任务会一直存在，直到用户手动暂停或任务完成
   * 3、任务调度器不会暂停最高优先级任务
   * 4、用户手动暂停或任务完成后，第二次重新执行该任务，该命令将失效
   * 5、如果下载队列中已经满了，则会停止队尾的任务，当高优先级任务完成后，该队尾任务将自动执行
   * 6、把任务设置为最高优先级任务后，将自动执行任务，不需要重新调用start()启动任务
   */
  @Override public void setHighestPriority() {
    super.setHighestPriority();
  }

  /**
   * 下载任务是否存在
   *
   * @return {@code true}任务存在
   */
  @Override public boolean taskExists() {
    return DownloadTaskQueue.getInstance().getTask(mEntity.getKey()) != null;
  }

  /**
   * 获取下载实体
   */
  public DownloadEntity getDownloadEntity() {
    return mEntity;
  }

  /**
   * 是否在下载，该api后续版本会删除
   *
   * @deprecated {@link #isRunning()}
   */
  @Deprecated public boolean isDownloading() {
    return isRunning();
  }

  /**
   * 是否在下载
   *
   * @return {@code true}任务正在下载
   */
  @Override public boolean isRunning() {
    DownloadTask task = DownloadTaskQueue.getInstance().getTask(mEntity.getKey());
    return task != null && task.isRunning();
  }

  /**
   * 检查下载实体，判断实体是否合法
   * 合法标准为：
   * 1、下载路径不为null，并且下载路径是正常的http或ftp路径
   * 2、保存路径不为null，并且保存路径是android文件系统路径
   * 3、保存路径不能重复
   *
   * @return {@code true}合法
   */
  @Override protected boolean checkEntity() {
    boolean b = getTargetType() < GROUP_HTTP && checkUrl() && checkFilePath();
    if (b) {
      mEntity.save();
      mTaskEntity.save();
    }
    return b;
  }

  /**
   * 检查并设置普通任务的文件保存路径
   *
   * @return {@code true}保存路径合法
   */
  private boolean checkFilePath() {
    String filePath = mTempFilePath;
    if (TextUtils.isEmpty(filePath)) {
      ALog.e(TAG, "下载失败，文件保存路径为null");
      return false;
    } else if (!filePath.startsWith("/")) {
      ALog.e(TAG, "下载失败，文件保存路径【" + filePath + "】错误");
      return false;
    }
    File file = new File(filePath);
    if (file.isDirectory()) {
      if (getTargetType() == HTTP) {
        ALog.e(TAG, "下载失败，保存路径【" + filePath + "】不能为文件夹，路径需要是完整的文件路径，如：/mnt/sdcard/game.zip");
        return false;
      } else if (getTargetType() == FTP) {
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
      if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=?", filePath)) {
        if (!forceDownload) {
          ALog.e(TAG, "下载失败，保存路径【" + filePath + "】已经被其它任务占用，请设置其它保存路径");
          return false;
        }else {
          ALog.d(TAG, "保存路径【" + filePath + "】已经被其它任务占用，当前任务将覆盖该路径的文件");
          DbEntity.deleteData(DownloadEntity.class, "downloadPath=?", filePath);
          mTaskEntity = TEManager.getInstance().getTEntity(DownloadTaskEntity.class, url);
        }
      }
      File oldFile = new File(mEntity.getDownloadPath());
      File newFile = new File(filePath);
      mEntity.setDownloadPath(filePath);
      mEntity.setFileName(newFile.getName());
      mTaskEntity.setKey(filePath);
      if (oldFile.exists()) {
        oldFile.renameTo(newFile);
        CommonUtil.modifyTaskRecord(oldFile.getPath(), newFile.getPath());
      }
    }
    return true;
  }

  /**
   * 检查普通任务的下载地址
   *
   * @return {@code true}地址合法
   */
  private boolean checkUrl() {
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
    if (!TextUtils.isEmpty(newUrl)) {
      mEntity.setUrl(newUrl);
      mTaskEntity.setUrl(newUrl);
    }
    return true;
  }
}
