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
package com.arialyy.aria.m3u8.live;

import android.text.TextUtils;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.processor.ILiveTsUrlConverter;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.loader.AbsLoader;
import com.arialyy.aria.core.loader.AbsNormalLoaderUtil;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.m3u8.M3U8InfoThread;
import com.arialyy.aria.m3u8.M3U8Listener;
import com.arialyy.aria.m3u8.M3U8TaskOption;
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
public class M3U8LiveUtil extends AbsNormalLoaderUtil {
  private final String TAG = "M3U8LiveDownloadUtil";
  private M3U8InfoThread mInfoThread;
  private ScheduledThreadPoolExecutor mTimer;
  private ExecutorService mInfoPool = Executors.newCachedThreadPool();
  private List<String> mPeerUrls = new ArrayList<>();
  private M3U8TaskOption mM3U8Option;

  protected M3U8LiveUtil(DTaskWrapper wrapper, M3U8Listener listener) {
    super(wrapper, listener);
    wrapper.generateM3u8Option(M3U8TaskOption.class);
    mM3U8Option = (M3U8TaskOption) wrapper.getM3u8Option();
  }

  @Override protected AbsLoader createLoader() {
    return new M3U8LiveLoader((M3U8Listener) getListener(), (DTaskWrapper) getTaskWrapper());
  }

  @Override protected Runnable createInfoThread() {
    return null;
  }

  private Runnable createLiveInfoThread(){
    M3U8InfoThread infoThread =
        new M3U8InfoThread((DTaskWrapper) getTaskWrapper(), new OnFileInfoCallback() {
          @Override public void onComplete(String key, CompleteInfo info) {
            ALog.d(TAG, "更新直播的m3u8文件");
          }

          @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
            fail(e, needRetry);
          }
        });
    infoThread.setOnGetPeerCallback(new M3U8InfoThread.OnGetLivePeerCallback() {
      @Override public void onGetPeer(String url) {
        if (mPeerUrls.contains(url)) {
          return;
        }
        mPeerUrls.add(url);
        ILiveTsUrlConverter converter = mM3U8Option.getLiveTsUrlConverter();
        if (converter != null) {
          if (TextUtils.isEmpty(mM3U8Option.getBandWidthUrl())) {
            url = converter.convert(((DownloadEntity) getTaskWrapper().getEntity()).getUrl(), url);
          } else {
            url = converter.convert(mM3U8Option.getBandWidthUrl(), url);
          }
        }
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
          fail(new M3U8Exception(TAG, String.format("ts地址错误，url：%s", url)), false);
          return;
        }
        getLoader().offerPeer(url);
      }
    });
    return infoThread;
  }

  @Override protected void onCancel() {
    super.onCancel();
    if (mInfoThread != null) {
      mInfoThread.setStop(true);
    }
  }

  /**
   * 对于直播来说是没有停止的，停止就代表完成
   */
  @Override protected void onStop() {
    super.onStop();
    handleComplete();
  }

  private void handleComplete() {
    if (mInfoThread != null) {
      mInfoThread.setStop(true);
      closeTimer();
      if (mM3U8Option.isMergeFile()) {
        if (getLoader().mergeFile()) {
          getListener().onComplete();
        } else {
          getListener().onFail(false, new M3U8Exception(TAG, "合并文件失败"));
        }
      } else {
        getListener().onComplete();
      }
    }
  }

  @Override protected void onStart() {
    super.onStart();
    startTimer();
  }

  private void startTimer() {
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        mInfoThread = (M3U8InfoThread) createLiveInfoThread();
        mInfoPool.execute(mInfoThread);
      }
    }, 0, mM3U8Option.getLiveUpdateInterval(), TimeUnit.MILLISECONDS);
  }

  private void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  @Override protected void fail(BaseException e, boolean needRetry) {
    super.fail(e, needRetry);
    handleComplete();
  }

  @Override public M3U8LiveLoader getLoader() {
    return (M3U8LiveLoader) super.getLoader();
  }
}
