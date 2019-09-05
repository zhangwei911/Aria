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
package com.arialyy.aria.core.common;

import android.os.Looper;
import android.util.SparseArray;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/7/1.
 * 控制线程任务状态，如：开始，停止，取消，重试
 */
public abstract class AbsFileer<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Runnable {
  protected final String TAG;
  protected IEventListener mListener;
  protected TASK_WRAPPER mTaskWrapper;
  protected ENTITY mEntity;
  protected File mTempFile;

  private SparseArray<AbsThreadTask> mTask = new SparseArray<>();
  private ScheduledThreadPoolExecutor mTimer;

  /**
   * 进度刷新间隔
   */
  private long mUpdateInterval = 1000;
  protected TaskRecord mRecord;
  private IThreadState mStateManager;
  private boolean isCancel = false, isStop = false;
  private boolean isRuning = false;

  protected AbsFileer(IEventListener listener, TASK_WRAPPER wrapper) {
    mListener = listener;
    mTaskWrapper = wrapper;
    mEntity = mTaskWrapper.getEntity();
    TAG = CommonUtil.getClassName(getClass());
  }

  protected abstract IThreadState getStateManager(Looper looper);

  /**
   * 处理任务
   */
  protected abstract void handleTask();

  public String getKey() {
    return mTaskWrapper.getKey();
  }

  public ENTITY getEntity() {
    return mEntity;
  }

  public SparseArray<AbsThreadTask> getTaskList() {
    return mTask;
  }

  /**
   * 重置任务状态
   */
  private void resetState() {
    closeTimer();
    if (mTask != null && mTask.size() != 0) {
      for (int i = 0; i < mTask.size(); i++) {
        mTask.valueAt(i).breakTask();
      }
      mTask.clear();
    }
  }

  /**
   * 开始流程
   */
  private void startFlow() {
    if (isBreak()) {
      return;
    }
    isRuning = true;
    resetState();
    mRecord = new RecordHandler(mTaskWrapper).getRecord();
    Looper.prepare();
    Looper looper = Looper.myLooper();
    mStateManager = getStateManager(looper);
    onPostPre();
    handleTask();
    startTimer();
    Looper.loop();
  }

  @Override public void run() {
    if (isRunning()) {
      return;
    }
    startFlow();
  }

  /**
   * 预处理完成
   */
  protected void onPostPre() {

  }

  /**
   * 延迟启动定时器
   */
  protected long delayTimer() {
    return 1000;
  }

  /**
   * 启动进度获取定时器
   */
  private synchronized void startTimer() {
    if (isBreak()) {
      return;
    }
    ALog.d(TAG, "启动定时器");
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (mStateManager.isComplete()
            || mStateManager.isFail()
            || !isRunning()
            || isBreak()) {
          //ALog.d(TAG, "isComplete = " + mStateManager.isComplete()
          //    + "; isFail = " + mStateManager.isFail()
          //    + "; isRunning = " + isRunning()
          //    + "; isBreak = " + isBreak());
          ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
          closeTimer();
          onDestroy();
        } else if (mStateManager.getCurrentProgress() >= 0) {
          mListener.onProgress(mStateManager.getCurrentProgress());
        }
      }
    }, delayTimer(), mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  public synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  public void onDestroy() {
    isRuning = false;
  }

  /**
   * 设置定时器更新间隔
   *
   * @param interval 单位毫秒，不能小于0
   */
  protected void setUpdateInterval(long interval) {
    if (interval < 0) {
      ALog.w(TAG, "更新间隔不能小于0，默认为1000毫秒");
      return;
    }
    mUpdateInterval = interval;
  }

  public long getFileSize() {
    return mEntity.getFileSize();
  }

  /**
   * 获取当前任务位置
   */
  public long getCurrentLocation() {
    return isRuning ? mStateManager.getCurrentProgress() : mEntity.getCurrentProgress();
  }

  public synchronized boolean isRunning() {
    boolean b = ThreadTaskManager.getInstance().taskIsRunning(mTaskWrapper.getKey());
    //ALog.d(TAG, "isRunning = " + b);
    return b && isRuning;
  }

  final public synchronized void cancel() {
    if (isCancel) {
      ALog.d(TAG, String.format("任务【%s】正在删除，删除任务失败", mTaskWrapper.getKey()));
      return;
    }
    closeTimer();
    isCancel = true;
    onCancel();
    for (int i = 0; i < mTask.size(); i++) {
      AbsThreadTask task = mTask.valueAt(i);
      if (task != null && !task.isThreadComplete()) {
        task.cancel();
      }
    }
    ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
    onPostCancel();
    onDestroy();
    mListener.onCancel();
  }

  /**
   * 删除线程任务前的操作
   */
  protected void onCancel() {

  }

  /**
   * 删除操作处理完成
   */
  protected void onPostCancel() {

  }

  final public synchronized void stop() {
    if (isStop) {
      return;
    }
    closeTimer();
    isStop = true;
    onStop();
    for (int i = 0; i < mTask.size(); i++) {
      AbsThreadTask task = mTask.valueAt(i);
      if (task != null && !task.isThreadComplete()) {
        task.stop();
      }
    }
    ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
    onPostStop();
    onDestroy();
    mListener.onStop(getCurrentLocation());
  }

  /**
   * 停止线程任务前的操作
   */
  protected void onStop() {

  }

  /**
   * 停止操作完成
   */
  protected void onPostStop() {

  }

  /**
   * 直接调用的时候会自动启动线程执行
   */
  public synchronized void start() {
    if (isRunning()) {
      ALog.d(TAG, String.format("任务【%s】正在执行，启动任务失败", mTaskWrapper.getKey()));
      return;
    }
    new Thread(this).start();
  }

  /**
   * 重试任务
   */
  public void retryTask() {
    ALog.w(TAG, String.format("任务【%s】开始重试", mEntity.getFileName()));
    startFlow();
  }

  /**
   * 任务是否已经中断
   *
   * @return {@code true}中断
   */
  public boolean isBreak() {
    if (isCancel || isStop) {
      //closeTimer();
      ALog.d(TAG, "isCancel = " + isCancel + ", isStop = " + isStop);
      ALog.d(TAG, String.format("任务【%s】已停止或取消了", mEntity.getFileName()));
      return true;
    }
    return false;
  }
}
