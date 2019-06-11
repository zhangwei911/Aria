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
package com.arialyy.aria.core.download.m3u8;

import android.text.TextUtils;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.util.ALog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * M3U8直播文件下载工具，对于直播来说，需要定时更新m3u8文件
 * 工作流程：
 * 1、持续获取切片信息，直到调用停止|取消才停止获取切片信息
 * 2、完成所有分片下载后，合并ts文件
 * 3、删除该隐藏文件夹
 * 4、对于直播来说是没有停止的，停止就代表完成
 * 5、不处理直播切片下载失败的状态
 */
public class M3U8LiveDownloadUtil implements IUtil {
  private final String TAG = "M3U8LiveDownloadUtil";
  private DTaskWrapper mWrapper;
  private IDownloadListener mListener;
  private boolean isStop = false, isCancel = false;
  private M3U8LiveLoader mLoader;
  private M3U8InfoThread mInfoThread;
  private ScheduledThreadPoolExecutor mTimer;
  private ExecutorService mInfoPool = Executors.newCachedThreadPool();
  private List<String> mPeerUrls = new ArrayList<>();

  public M3U8LiveDownloadUtil(DTaskWrapper wrapper, IDownloadListener listener) {
    mWrapper = wrapper;
    mListener = listener;
    mLoader = new M3U8LiveLoader(mListener, mWrapper);
  }

  @Override public String getKey() {
    return mWrapper.getKey();
  }

  @Override public long getFileSize() {
    return 0;
  }

  @Override public long getCurrentLocation() {
    return 0;
  }

  @Override public boolean isRunning() {
    return mLoader.isRunning();
  }

  @Override public void cancel() {
    isCancel = true;
    mLoader.cancel();
    if (mInfoThread != null) {
      mInfoThread.setStop(true);
    }
  }

  /**
   * 对于直播来说是没有停止的，停止就代表完成
   */
  @Override public void stop() {
    isStop = true;
    mLoader.stop();
    handleComplete();
  }

  private void handleComplete() {
    if (mInfoThread != null) {
      mInfoThread.setStop(true);
      closeTimer();
      if (mLoader.mergeFile()) {
        mListener.onComplete();
      } else {
        mListener.onFail(false, new M3U8Exception(TAG, "合并文件失败"));
      }
    }
    ILiveTsUrlConverter converter = mWrapper.asM3U8().getLiveTsUrlConverter();
    if (converter != null && converter.getClass().isAnonymousClass()) {
      mWrapper.asM3U8().setLiveTsUrlConverter(null);
    }
  }

  @Override public void start() {
    if (isStop || isCancel) {
      return;
    }
    mListener.onPre();
    getLiveInfo();
    mLoader.start();
    startTimer();
  }

  private void startTimer() {
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        mInfoThread = (M3U8InfoThread) getLiveInfo();
        mInfoPool.execute(mInfoThread);
      }
    }, 0, mWrapper.asM3U8().getLiveUpdateInterval(), TimeUnit.MILLISECONDS);
  }

  private void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  @Override public void setMaxSpeed(int speed) {
    mLoader.setMaxSpeed(speed);
  }

  /**
   * 获取直播文件信息
   */
  private Runnable getLiveInfo() {
    M3U8InfoThread infoThread = new M3U8InfoThread(mWrapper, new OnFileInfoCallback() {
      @Override public void onComplete(String key, CompleteInfo info) {
        ALog.d(TAG, "更新直播的m3u8文件");
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        failDownload(e, needRetry);
      }
    });
    infoThread.setOnGetPeerCallback(new M3U8InfoThread.OnGetLivePeerCallback() {
      @Override public void onGetPeer(String url) {
        if (mPeerUrls.contains(url)) {
          return;
        }
        mPeerUrls.add(url);
        ILiveTsUrlConverter converter = mWrapper.asM3U8().getLiveTsUrlConverter();
        if (converter != null) {
          if (TextUtils.isEmpty(mWrapper.asM3U8().getBandWidthUrl())) {
            url = converter.convert(mWrapper.getEntity().getUrl(), url);
          } else {
            url = converter.convert(mWrapper.asM3U8().getBandWidthUrl(), url);
          }
        }
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
          failDownload(new M3U8Exception(TAG, String.format("ts地址错误，url：%s", url)), false);
          return;
        }
        mLoader.offerPeer(url);
      }
    });
    return infoThread;
  }

  private void failDownload(BaseException e, boolean needRetry) {
    if (isStop || isCancel) {
      return;
    }
    handleComplete();
    mListener.onFail(needRetry, e);
  }
}
