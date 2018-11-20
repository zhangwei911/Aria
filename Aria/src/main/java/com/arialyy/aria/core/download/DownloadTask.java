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

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.downloader.SimpleDownloadUtil;
import com.arialyy.aria.core.inf.AbsNormalTask;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.util.ALog;
import java.io.File;

/**
 * Created by lyy on 2016/8/11.
 * 下载任务类
 */
public class DownloadTask extends AbsNormalTask<DownloadEntity, DownloadTaskEntity> {
  public static final String TAG = "DownloadTask";

  private DownloadTask(DownloadTaskEntity taskEntity, Handler outHandler) {
    mTaskEntity = taskEntity;
    mOutHandler = outHandler;
    mContext = AriaManager.APP;
    mListener = new BaseDListener(this, mOutHandler);
    mUtil = new SimpleDownloadUtil(taskEntity, (IDownloadListener) mListener);
    mEntity = taskEntity.getEntity();
  }

  /**
   * 获取文件保存路径
   *
   * @return 如果路径不存在，返回null
   */
  public String getDownloadPath() {
    File file = new File(mEntity.getDownloadPath());
    if (!file.exists()) {
      return null;
    }
    return mEntity.getDownloadPath();
  }

  public DownloadEntity getEntity() {
    return mTaskEntity.getEntity();
  }

  /**
   * 获取当前下载任务的下载地址
   *
   * @see DownloadTask#getKey()
   */
  @Deprecated public String getDownloadUrl() {
    return mEntity.getUrl();
  }

  @Override public String getKey() {
    return mEntity.getUrl();
  }

  public DownloadEntity getDownloadEntity() {
    return mEntity;
  }

  @Override public String getTaskName() {
    return mEntity.getFileName();
  }

  public static class Builder {
    DownloadTaskEntity taskEntity;
    Handler outHandler;

    public Builder(DownloadTaskEntity taskEntity) {
      this.taskEntity = taskEntity;
    }

    /**
     * 设置自定义Handler处理下载状态时间
     *
     * @param schedulers {@link ISchedulers}
     */
    public Builder setOutHandler(ISchedulers schedulers) {
      outHandler = new Handler(Looper.getMainLooper(), schedulers);
      return this;
    }

    public DownloadTask build() {
      return new DownloadTask(taskEntity, outHandler);
    }
  }
}