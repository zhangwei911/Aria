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
package com.arialyy.aria.core.group;

import android.os.Handler;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IUtil;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.loader.NormalLoader;
import com.arialyy.aria.util.CommonUtil;

/**
 * 子任务下载器，负责创建
 */
public abstract class AbsSubDLoadUtil implements IUtil {
  protected final String TAG = CommonUtil.getClassName(getClass());

  private NormalLoader mDLoader;
  private DTaskWrapper mWrapper;
  private Handler mSchedulers;
  private ChildDLoadListener mListener;
  private boolean needGetInfo;

  /**
   * @param schedulers 调度器
   * @param needGetInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  protected AbsSubDLoadUtil(Handler schedulers, DTaskWrapper taskWrapper, boolean needGetInfo) {
    mWrapper = taskWrapper;
    mSchedulers = schedulers;
    this.needGetInfo = needGetInfo;
    mListener = new ChildDLoadListener(mSchedulers, AbsSubDLoadUtil.this);
    mDLoader = createLoader(mListener, taskWrapper);
  }

  /**
   * 创建加载器
   */
  protected abstract NormalLoader createLoader(ChildDLoadListener listener, DTaskWrapper wrapper);

  protected boolean isNeedGetInfo() {
    return needGetInfo;
  }

  public Handler getSchedulers() {
    return mSchedulers;
  }

  @Override public String getKey() {
    return mWrapper.getKey();
  }

  public DTaskWrapper getWrapper() {
    return mWrapper;
  }

  public DownloadEntity getEntity() {
    return mWrapper.getEntity();
  }

  /**
   * 重新开始任务
   */
  void reStart() {
    if (mDLoader != null) {
      mDLoader.retryTask();
    }
  }

  public NormalLoader getDownloader() {
    return mDLoader;
  }

  @Override public long getFileSize() {
    return mDLoader == null ? -1 : mDLoader.getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mDLoader == null ? -1 : mDLoader.getCurrentLocation();
  }

  @Override public boolean isRunning() {
    return mDLoader != null && mDLoader.isRunning();
  }

  @Override public void cancel() {
    if (mDLoader != null && isRunning()) {
      mDLoader.cancel();
    } else {
      mSchedulers.obtainMessage(ISchedulers.CANCEL, this).sendToTarget();
    }
  }

  @Override public void stop() {
    if (mDLoader != null && isRunning()) {
      mDLoader.stop();
    } else {
      mSchedulers.obtainMessage(ISchedulers.STOP, this).sendToTarget();
    }
  }
}
