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

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.download.BaseDListener;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.util.ALog;
import java.io.File;

public abstract class NormalFileer<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    extends AbsFileer<ENTITY, TASK_WRAPPER> {
  private ThreadStateManager mStateManager;
  private Handler mStateHandler;
  protected int mTotalThreadNum; //总线程数

  protected NormalFileer(IEventListener listener, TASK_WRAPPER wrapper) {
    super(listener, wrapper);
  }

  /**
   * 处理新任务
   *
   * @return {@code true}创建新任务成功
   */
  protected abstract boolean handleNewTask();

  /**
   * 选择单任务线程的类型
   */
  protected abstract AbsThreadTask selectThreadTask(SubThreadConfig<TASK_WRAPPER> config);

  /**
   * 设置最大下载/上传速度
   *
   * @param maxSpeed 单位为：kb
   */
  public void setMaxSpeed(int maxSpeed) {
    for (int i = 0; i < mTotalThreadNum; i++) {
      AbsThreadTask task = getTaskList().get(i);
      if (task != null) {
        task.setMaxSpeed(maxSpeed);
      }
    }
  }

  @Override protected void onPostPre() {
    super.onPostPre();
    mTotalThreadNum = mRecord.threadNum;
  }

  @Override protected IThreadState getStateManager(Looper looper) {
    mStateManager = new ThreadStateManager(looper, mRecord, mListener);
    mStateHandler = new Handler(looper, mStateManager);
    return mStateManager;
  }

  @Override protected void handleTask() {
    if (mTaskWrapper.isSupportBP()) {
      handleBreakpoint();
    } else {
      handleNoSupportBP();
    }
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
        mStateHandler.obtainMessage(IThreadState.STATE_COMPLETE).sendToTarget();
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
      getTaskList().put(tr.threadId, task);
    }
    if (currentProgress != 0 && currentProgress != mEntity.getCurrentProgress()) {
      ALog.d(TAG, String.format("进度修正，当前进度：%s", currentProgress));
      mEntity.setCurrentProgress(currentProgress);
    }
    mStateHandler.obtainMessage(IThreadState.STATE_UPDATE_PROGRESS, currentProgress)
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
    for (int i = 0; i < getTaskList().size(); i++) {
      ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), getTaskList().get(i));
    }
  }

  /**
   * 处理不支持断点的任务
   */
  private void handleNoSupportBP() {
    if (mListener instanceof BaseDListener) {
      ((BaseDListener) mListener).supportBreakpoint(false);
    }

    AbsThreadTask task = createSingThreadTask(mRecord.threadRecords.get(0), 1);
    if (task == null) return;
    getTaskList().put(0, task);
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    mListener.onStart(0);
  }
}
