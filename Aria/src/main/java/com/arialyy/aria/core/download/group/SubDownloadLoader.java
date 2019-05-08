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
package com.arialyy.aria.core.download.group;

import android.os.Handler;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.downloader.Downloader;
import com.arialyy.aria.core.download.downloader.HttpFileInfoThread;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;

/**
 * 子任务下载器，负责创建{@link Downloader}
 */
class SubDownloadLoader implements IUtil {
  private final String TAG = "SubDownloadLoader";

  private Downloader mDownloader;
  private DTaskWrapper mWrapper;
  private Handler mSchedulers;
  private ChildDownloadListener mListener;

  /**
   * @param schedulers 调度器
   * @param needGetInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  SubDownloadLoader(Handler schedulers, DTaskWrapper taskWrapper, boolean needGetInfo) {
    mWrapper = taskWrapper;
    mSchedulers = schedulers;
    mListener = new ChildDownloadListener(mSchedulers, SubDownloadLoader.this);
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
    if (mDownloader != null) {
      mDownloader.retryTask();
    }
  }

  @Override public long getFileSize() {
    return mDownloader == null ? -1 : mDownloader.getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mDownloader == null ? -1 : mDownloader.getCurrentLocation();
  }

  @Override public boolean isRunning() {
    return mDownloader != null && mDownloader.isRunning();
  }

  @Override public void cancel() {
    if (mDownloader != null) {
      mDownloader.cancel();
    }
  }

  @Override public void stop() {
    if (mDownloader != null) {
      mDownloader.stop();
    }
  }

  @Override public void start() {
    if (mWrapper.getRequestType() == AbsTaskWrapper.D_HTTP) {
      new Thread(new HttpFileInfoThread(mWrapper, new OnFileInfoCallback() {

        @Override public void onComplete(String url, CompleteInfo info) {
          mDownloader = new Downloader(mListener, mWrapper);
          mDownloader.start();
        }

        @Override public void onFail(String url, BaseException e, boolean needRetry) {
          mSchedulers.obtainMessage(ISchedulers.FAIL, SubDownloadLoader.this);
        }
      })).start();
    } else if (mWrapper.getRequestType() == AbsTaskWrapper.D_FTP) {
      mDownloader = new Downloader(mListener, mWrapper);
      mDownloader.start();
    } else {
      ALog.w(TAG, String.format("不识别的类型，requestType：%s", mWrapper.getRequestType()));
    }
  }

  @Override public void resume() {
    start();
  }

  @Override public void setMaxSpeed(int speed) {
    if (mDownloader != null) {
      mDownloader.setMaxSpeed(speed);
    }
  }
}
