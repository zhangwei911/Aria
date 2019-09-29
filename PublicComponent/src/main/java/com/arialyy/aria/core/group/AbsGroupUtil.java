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
import android.os.Looper;
import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.IUtil;
import com.arialyy.aria.core.listener.IDGroupListener;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组核心逻辑
 */
public abstract class AbsGroupUtil implements IUtil, Runnable {
  protected final String TAG = CommonUtil.getClassName(getClass());

  private long mCurrentLocation = 0;
  protected IDGroupListener mListener;
  private ScheduledThreadPoolExecutor mTimer;
  private long mUpdateInterval;
  private boolean isStop = false, isCancel = false;
  private Handler mScheduler;
  private SimpleSubQueue mSubQueue = SimpleSubQueue.newInstance();
  private Map<String, AbsSubDLoadUtil> mExeLoader = new WeakHashMap<>();
  private Map<String, DTaskWrapper> mCache = new WeakHashMap<>();
  private DGTaskWrapper mGTWrapper;
  private GroupRunState mState;

  protected AbsGroupUtil(IDGroupListener listener, DGTaskWrapper groupWrapper) {
    mListener = listener;
    mGTWrapper = groupWrapper;
    mUpdateInterval = Configuration.getInstance().downloadCfg.getUpdateInterval();
    mState = new GroupRunState(groupWrapper.getKey(), mListener,
        groupWrapper.getSubTaskWrapper().size(), mSubQueue);
    mScheduler = new Handler(Looper.getMainLooper(), SimpleSchedulers.newInstance(mState));
    initState();
  }

  /**
   * 创建子任务加载器工具
   *
   * @param needGetFileInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  protected abstract AbsSubDLoadUtil createSubLoader(DTaskWrapper wrapper, boolean needGetFileInfo);

  protected DGTaskWrapper getWrapper() {
    return mGTWrapper;
  }

  protected GroupRunState getState() {
    return mState;
  }

  public Handler getScheduler() {
    return mScheduler;
  }

  /**
   * 初始化组合任务状态
   */
  protected void initState() {
    for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
      if (wrapper.getEntity().getState() == IEntity.STATE_COMPLETE) {
        mState.updateCompleteNum();
        mCurrentLocation += wrapper.getEntity().getFileSize();
      } else {
        mCache.put(wrapper.getKey(), wrapper);
        mCurrentLocation += wrapper.getEntity().getCurrentProgress();
      }
    }
    mState.updateProgress(mCurrentLocation);
  }

  @Override public String getKey() {
    return mGTWrapper.getKey();
  }

  /**
   * 启动子任务下载
   *
   * @param url 子任务下载地址
   */
  public void startSubTask(String url) {
    if (!checkSubTask(url, "开始")) return;
    if (!mState.isRunning) {
      startTimer();
    }
    AbsSubDLoadUtil d = getDownloader(url);
    if (d != null && !d.isRunning()) {
      mSubQueue.startTask(d);
    }
  }

  /**
   * 停止子任务下载
   *
   * @param url 子任务下载地址
   */
  public void stopSubTask(String url) {
    if (!checkSubTask(url, "停止")) return;
    AbsSubDLoadUtil d = getDownloader(url);
    if (d != null && d.isRunning()) {
      mSubQueue.stopTask(d);
    }
  }

  /**
   * 检查子任务
   *
   * @param url 子任务url
   * @param type 任务类型
   * @return {@code true} 任务可以下载
   */
  private boolean checkSubTask(String url, String type) {
    DTaskWrapper wrapper = mCache.get(url);
    if (wrapper != null) {
      if (wrapper.getState() == IEntity.STATE_COMPLETE) {
        ALog.w(TAG, "任务【" + url + "】已完成，" + type + "失败");
        return false;
      }
    } else {
      ALog.w(TAG, "任务组中没有该任务【" + url + "】，" + type + "失败");
      return false;
    }
    return true;
  }

  /**
   * 通过地址获取下载器
   *
   * @param url 子任务下载地址
   */
  private AbsSubDLoadUtil getDownloader(String url) {
    AbsSubDLoadUtil d = mExeLoader.get(url);
    if (d == null) {
      return createSubLoader(mCache.get(url), true);
    }
    return d;
  }

  @Override public long getFileSize() {
    return mGTWrapper.getEntity().getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mCurrentLocation;
  }

  @Override public boolean isRunning() {
    return mState.isRunning;
  }

  @Override public void cancel() {
    isCancel = true;
    closeTimer();
    onPreCancel();

    mSubQueue.removeAllTask();
    mListener.onCancel();
  }

  /**
   * onCancel前的操作
   */
  public void onPreCancel() {

  }

  @Override public void stop() {
    isStop = true;
    closeTimer();
    if (onPreStop()) {
      return;
    }
    mSubQueue.stopAllTask();
  }

  /**
   * onStop前的操作
   *
   * @return 返回{@code true}，直接回调{@link IDGroupListener#onStop(long)}
   */
  protected boolean onPreStop() {

    return false;
  }

  @Override public void start() {
    new Thread(this).start();
  }

  @Override public void run() {
    if (isStop || isCancel) {
      closeTimer();
      return;
    }
    if (onStart()) {
      startRunningFlow();
    }
  }

  /**
   * 处理启动前的检查：获取组合任务大小
   *
   * @return {@code false} 将不再走后续流程，任务介绍
   */
  protected boolean onStart() {

    return false;
  }

  synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  /**
   * 开始进度流程
   */
  private void startRunningFlow() {
    closeTimer();
    mListener.onPostPre(mGTWrapper.getEntity().getFileSize());
    if (mCurrentLocation > 0) {
      mListener.onResume(mCurrentLocation);
    } else {
      mListener.onStart(mCurrentLocation);
    }
    startTimer();
  }

  private synchronized void startTimer() {
    mState.isRunning = true;
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (!mState.isRunning) {
          closeTimer();
        } else if (mCurrentLocation >= 0) {
          long t = 0;
          for (DTaskWrapper te : mGTWrapper.getSubTaskWrapper()) {
            if (te.getState() == IEntity.STATE_COMPLETE) {
              t += te.getEntity().getFileSize();
            } else {
              t += te.getEntity().getCurrentProgress();
            }
          }
          mCurrentLocation = t;
          mState.updateProgress(mCurrentLocation);
          mListener.onProgress(t);
        }
      }
    }, 0, mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  /**
   * 启动子任务下载器
   */
  protected void startSubLoader(AbsSubDLoadUtil loader) {
    mExeLoader.put(loader.getKey(), loader);
    mSubQueue.startTask(loader);
  }
}
