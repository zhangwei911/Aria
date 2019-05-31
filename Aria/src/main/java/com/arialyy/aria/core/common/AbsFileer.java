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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.BaseDListener;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.util.ALog;
import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/7/1. 任务处理器
 */
public abstract class AbsFileer<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Runnable {
  private final String TAG = "AbsFileer";
  protected IEventListener mListener;
  protected TASK_WRAPPER mTaskWrapper;
  protected ENTITY mEntity;
  protected Context mContext;
  protected File mTempFile; //文件
  protected int mTotalThreadNum; //总线程数

  private SparseArray<AbsThreadTask> mTask = new SparseArray<>();
  private ScheduledThreadPoolExecutor mTimer;

  /**
   * 进度刷新间隔
   */
  private long mUpdateInterval = 1000;
  protected TaskRecord mRecord;
  private ThreadStateManager mStateManager;
  private Handler mStateHandler;
  private boolean isCancel = false, isStop = false;

  protected AbsFileer(IEventListener listener, TASK_WRAPPER taskEntity) {
    mListener = listener;
    mTaskWrapper = taskEntity;
    mEntity = mTaskWrapper.getEntity();
    mContext = AriaManager.APP;
  }

  public String getKey() {
    return mTaskWrapper.getKey();
  }

  public ENTITY getEntity() {
    return mEntity;
  }

  /**
   * 设置最大下载/上传速度
   *
   * @param maxSpeed 单位为：kb
   */
  public void setMaxSpeed(int maxSpeed) {
    for (int i = 0; i < mTotalThreadNum; i++) {
      AbsThreadTask task = mTask.get(i);
      if (task != null) {
        task.setMaxSpeed(maxSpeed);
      }
    }
  }

  /**
   * 重置任务状态
   */
  private void resetState() {
    closeTimer();
    mTotalThreadNum = 0;
    if (mTask != null && mTask.size() != 0) {
      for (int i = 0; i < mTask.size(); i++) {
        AbsThreadTask task = mTask.get(i);
        if (task != null) {
          task.breakTask();
        }
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
    resetState();
    mRecord = new RecordHandler(mTaskWrapper).getRecord();
    mTotalThreadNum = mRecord.threadNum;
    Looper.prepare();
    Looper looper = Looper.myLooper();
    mStateManager = new ThreadStateManager(looper, mRecord, mListener);
    mStateHandler = new Handler(looper, mStateManager);
    onPostPre();
    if (!mTaskWrapper.isSupportBP()) {
      handleNoSupportBP();
    } else {
      handleBreakpoint();
    }
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
   * 启动进度获取定时器
   */
  private synchronized void startTimer() {
    ALog.d(TAG, "启动定时器");
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (mStateManager.isComplete()
            || mStateManager.isStop()
            || mStateManager.isCancel()
            || mStateManager.isFail()
            || !isRunning()) {
          if (mStateManager.isComplete() || mStateManager.isFail()) {
            ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
          }
          closeTimer();
        } else if (mStateManager.getCurrentProgress() >= 0) {
          mListener.onProgress(mStateManager.getCurrentProgress());
        }
      }
    }, 0, mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  public synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
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
    return mStateManager.getCurrentProgress();
  }

  public synchronized boolean isRunning() {
    return ThreadTaskManager.getInstance().taskIsRunning(mTaskWrapper.getKey());
  }

  public synchronized void cancel() {
    if (isCancel) {
      ALog.d(TAG, String.format("任务【%s】正在删除，删除任务失败", mTaskWrapper.getKey()));
      return;
    }
    closeTimer();
    isCancel = true;
    for (int i = 0; i < mTask.size(); i++) {
      AbsThreadTask task = mTask.get(i);
      if (task != null) {
        task.cancel();
      }
    }
    ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
  }

  public synchronized void stop() {
    if (isStop) {
      return;
    }
    closeTimer();
    isStop = true;
    if (mStateManager.isComplete()) return;
    for (int i = 0; i < mTask.size(); i++) {
      AbsThreadTask task = mTask.get(i);
      if (task != null && !task.isThreadComplete()) {
        task.stop();
      }
    }
    ThreadTaskManager.getInstance().removeTaskThread(mTaskWrapper.getKey());
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

  public void resume() {
    start();
  }

  /**
   * 启动断点任务时，创建单线程任务
   *
   * @param record 线程记录
   * @param startNum 启动的线程数
   */
  private AbsThreadTask createSingThreadTask(ThreadRecord record, int startNum) {
    SubThreadConfig<TASK_WRAPPER> config = new SubThreadConfig<>();
    config.url = mEntity.isRedirect() ? mEntity.getRedirectUrl() : mEntity.getUrl();
    config.tempFile =
        mRecord.isBlock ? new File(
            String.format(RecordHandler.SUB_PATH, mTempFile.getPath(), record.threadId))
            : mTempFile;
    config.isBlock = mRecord.isBlock;
    config.isOpenDynamicFile = mRecord.isOpenDynamicFile;
    config.startThreadNum = startNum;
    config.taskWrapper = mTaskWrapper;
    config.record = record;
    config.stateHandler = mStateHandler;
    return selectThreadTask(config);
  }

  private void handleBreakpoint() {
    long fileLength = mEntity.getFileSize();
    long blockSize = fileLength / mTotalThreadNum;
    long currentProgress = 0;

    mRecord.fileLength = fileLength;
    if (mTaskWrapper.isNewTask() && !handleNewTask()) {
      return;
    }

    int startNum = mRecord.threadNum;
    for (ThreadRecord tr : mRecord.threadRecords) {
      if (!tr.isComplete) {
        startNum++;
      }
    }

    for (int i = 0; i < mTotalThreadNum; i++) {
      long startL = i * blockSize, endL = (i + 1) * blockSize;
      ThreadRecord tr = mRecord.threadRecords.get(i);

      if (tr.isComplete) {//该线程已经完成
        currentProgress += endL - startL;
        ALog.d(TAG, String.format("任务【%s】线程__%s__已完成", mTaskWrapper.getEntity().getFileName(), i));
        mStateHandler.obtainMessage(ThreadStateManager.STATE_COMPLETE).sendToTarget();
        if (mStateManager.isComplete()) {
          mRecord.deleteData();
          mListener.onComplete();
          return;
        }
        continue;
      }

      //如果有记录，则恢复任务
      long r = tr.startLocation;
      //记录的位置需要在线程区间中
      if (startL < r && r <= (i == (mTotalThreadNum - 1) ? fileLength : endL)) {
        currentProgress += r - startL;
      }
      ALog.d(TAG, String.format("任务【%s】线程__%s__恢复任务", mEntity.getFileName(), i));

      AbsThreadTask task = createSingThreadTask(tr, startNum);
      if (task == null) return;
      mTask.put(tr.threadId, task);
    }
    if (currentProgress != 0 && currentProgress != mEntity.getCurrentProgress()) {
      ALog.d(TAG, String.format("进度修正，当前进度：%s", currentProgress));
      mEntity.setCurrentProgress(currentProgress);
    }
    mStateHandler.obtainMessage(ThreadStateManager.STATE_UPDATE_PROGRESS, currentProgress)
        .sendToTarget();
    startThreadTask();
  }

  /**
   * 启动单线程任务
   */
  private void startThreadTask() {
    if (isBreak()) {
      return;
    }
    if (mStateManager.getCurrentProgress() > 0) {
      mListener.onResume(mStateManager.getCurrentProgress());
    } else {
      mListener.onStart(mStateManager.getCurrentProgress());
    }
    for (int i = 0; i < mTask.size(); i++) {
      ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), mTask.get(i));
    }
  }

  /**
   * 重试任务
   */
  public void retryTask() {
    ALog.w(TAG, String.format("任务【%s】开始重试", mEntity.getFileName()));
    startFlow();
  }

  /**
   * 处理新任务
   *
   * @return {@code true}创建新任务失败
   */
  protected abstract boolean handleNewTask();

  /**
   * 处理不支持断点的任务
   */
  private void handleNoSupportBP() {
    if (mListener instanceof BaseDListener) {
      ((BaseDListener) mListener).supportBreakpoint(false);
    }

    AbsThreadTask task = createSingThreadTask(mRecord.threadRecords.get(0), 1);
    if (task == null) return;
    mTask.put(0, task);
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    mListener.onStart(0);
  }

  /**
   * 选择单任务线程的类型
   */
  protected abstract AbsThreadTask selectThreadTask(SubThreadConfig<TASK_WRAPPER> config);

  /**
   * 任务是否已经中断
   *
   * @return {@code true}中断
   */
  public boolean isBreak() {
    if (isCancel || isStop) {
      closeTimer();
      ALog.d(TAG, String.format("任务【%s】已停止或取消了", mEntity.getFileName()));
      return true;
    }
    return false;
  }
}
