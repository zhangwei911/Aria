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
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.download.downloader.SimpleDownloadUtil;
import com.arialyy.aria.core.download.m3u8.M3U8LiveUtil;
import com.arialyy.aria.core.download.m3u8.M3U8VodUtil;
import com.arialyy.aria.core.inf.AbsNormalTask;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.core.scheduler.ISchedulers;

/**
 * Created by lyy on 2016/8/11.
 * 下载任务类
 */
public class DownloadTask extends AbsNormalTask<DTaskWrapper> {
  public static final String TAG = "DownloadTask";

  private DownloadTask(DTaskWrapper taskWrapper, Handler outHandler) {
    mTaskWrapper = taskWrapper;
    mOutHandler = outHandler;
    mContext = AriaManager.APP;
    if (taskWrapper.getRequestType() == AbsTaskWrapper.M3U8_VOD
        || taskWrapper.getRequestType() == AbsTaskWrapper.M3U8_LIVE) {
      mListener = new M3U8Listener(this, mOutHandler);
    } else {
      mListener = new BaseDListener(this, mOutHandler);
    }
  }

  /**
   * 获取文件保存路径
   *
   * @deprecated 后续版本将删除该方法，请使用{@link #getFilePath()}
   */
  @Deprecated
  public String getDownloadPath() {
    return getFilePath();
  }

  /**
   * 获取文件保存路径
   */
  public String getFilePath() {
    return mTaskWrapper.getEntity().getFilePath();
  }

  public DownloadEntity getEntity() {
    return mTaskWrapper.getEntity();
  }

  /**
   * 获取当前下载任务的下载地址
   *
   * @see DownloadTask#getKey()
   */
  @Deprecated public String getDownloadUrl() {
    return mTaskWrapper.getEntity().getUrl();
  }

  @Override public int getTaskType() {
    return DOWNLOAD;
  }

  @Override public String getKey() {
    return mTaskWrapper.getEntity().getUrl();
  }

  public DownloadEntity getDownloadEntity() {
    return mTaskWrapper.getEntity();
  }

  @Override public String getTaskName() {
    return mTaskWrapper.getEntity().getFileName();
  }

  @Override protected synchronized IUtil createUtil() {
    int taskType = mTaskWrapper.getRequestType();
    if (taskType == ITaskWrapper.M3U8_VOD) {
      return new M3U8VodUtil(mTaskWrapper, (M3U8Listener) mListener);
    } else if (taskType == ITaskWrapper.M3U8_LIVE) {
      return new M3U8LiveUtil(mTaskWrapper, (M3U8Listener) mListener);
    } else if (taskType == ITaskWrapper.D_HTTP || taskType == ITaskWrapper.D_FTP) {
      return new SimpleDownloadUtil(mTaskWrapper, (IDownloadListener) mListener);
    } else {
      throw new IllegalArgumentException(String.format("不识别的任务, 任务类型：%s", taskType));
    }
  }

  public static class Builder {
    DTaskWrapper taskEntity;
    Handler outHandler;

    public Builder(DTaskWrapper taskEntity) {
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