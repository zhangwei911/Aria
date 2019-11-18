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
package com.arialyy.aria.core.loader;

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.inf.IRecordHandler;
import com.arialyy.aria.core.inf.IThreadState;
import com.arialyy.aria.core.listener.BaseDListener;
import com.arialyy.aria.core.listener.IDLoadListener;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.io.File;

/**
 * 单文件
 */
public class NormalLoader extends AbsLoader {
  private ThreadStateManager mStateManager;
  private Handler mStateHandler;
  protected int mTotalThreadNum; //总线程数
  private int mStartThreadNum; //启动的线程数
  private ILoaderAdapter mAdapter;

  public NormalLoader(IEventListener listener, AbsTaskWrapper wrapper) {
    super(listener, wrapper);
    mTempFile = new File(getEntity().getFilePath());
    EventMsgUtil.getDefault().register(this);
    setUpdateInterval(wrapper.getConfig().getUpdateInterval());
  }

  public void setAdapter(ILoaderAdapter adapter) {
    mAdapter = adapter;
  }

  public AbsNormalEntity getEntity() {
    return (AbsNormalEntity) mTaskWrapper.getEntity();
  }

  @Override public long getFileSize() {
    return getEntity().getFileSize();
  }

  @Override public long getCurrentLocation() {
    return isRunning() ? mStateManager.getCurrentProgress() : getEntity().getCurrentProgress();
  }

  @Override protected IRecordHandler getRecordHandler(AbsTaskWrapper wrapper) {
    return mAdapter.recordHandler(wrapper);
  }

  /**
   * 设置最大下载/上传速度
   *
   * @param maxSpeed 单位为：kb
   */
  protected void setMaxSpeed(int maxSpeed) {
    for (int i = 0; i < getTaskList().size(); i++) {
      IThreadTask task = getTaskList().valueAt(i);
      if (task != null && mStartThreadNum > 0) {
        task.setMaxSpeed(maxSpeed / mStartThreadNum);
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    EventMsgUtil.getDefault().unRegister(this);
  }

  @Override protected void onPostPre() {
    super.onPostPre();
    if (mAdapter == null) {
      throw new NullPointerException("请使用adapter设置适配器");
    }
    mTotalThreadNum = mRecord.threadNum;

    if (mListener instanceof IDLoadListener) {
      ((IDLoadListener) mListener).onPostPre(getEntity().getFileSize());
    }
    File file = new File(getEntity().getFilePath());
    if (file.getParentFile() != null && !file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
  }

  /**
   * 如果使用"Content-Disposition"中的文件名，需要更新{@link #mTempFile}的路径
   */
  public void updateTempFile() {
    if (!mTempFile.getPath().equals(getEntity().getFilePath())) {
      boolean b = mTempFile.renameTo(new File(getEntity().getFilePath()));
      ALog.d(TAG, String.format("更新tempFile文件名%s", b ? "成功" : "失败"));
    }
  }

  @Override protected IThreadState createStateManager(Looper looper) {
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
  private IThreadTask createSingThreadTask(ThreadRecord record, int startNum) {
    SubThreadConfig config = new SubThreadConfig();
    config.url = getEntity().isRedirect() ? getEntity().getRedirectUrl() : getEntity().getUrl();
    config.tempFile =
        mRecord.isBlock ? new File(
            String.format(IRecordHandler.SUB_PATH, mTempFile.getPath(), record.threadId))
            : mTempFile;
    config.isBlock = mRecord.isBlock;
    config.startThreadNum = startNum;
    config.taskWrapper = mTaskWrapper;
    config.record = record;
    config.stateHandler = mStateHandler;
    return mAdapter.createThreadTask(config);
  }

  private void handleBreakpoint() {
    long fileLength = getEntity().getFileSize();
    long blockSize = fileLength / mTotalThreadNum;
    long currentProgress = 0;

    mRecord.fileLength = fileLength;
    if (mTaskWrapper.isNewTask() && !mAdapter.handleNewTask(mRecord, mTotalThreadNum)) {
      closeTimer();
      mListener.onFail(false, null);
      return;
    }

    for (ThreadRecord tr : mRecord.threadRecords) {
      if (!tr.isComplete) {
        mStartThreadNum++;
      }
    }

    for (int i = 0; i < mTotalThreadNum; i++) {
      long startL = i * blockSize, endL = (i + 1) * blockSize;
      ThreadRecord tr = mRecord.threadRecords.get(i);

      if (tr.isComplete) {//该线程已经完成
        currentProgress += endL - startL;
        ALog.d(TAG, String.format("任务【%s】线程__%s__已完成", mTaskWrapper.getKey(), i));
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
      ALog.d(TAG, String.format("任务【%s】线程__%s__恢复任务", getEntity().getFileName(), i));

      IThreadTask task = createSingThreadTask(tr, mStartThreadNum);
      if (task == null) return;
      getTaskList().put(tr.threadId, task);
    }
    if (currentProgress != 0 && currentProgress != getEntity().getCurrentProgress()) {
      ALog.d(TAG, String.format("进度修正，当前进度：%s", currentProgress));
      getEntity().setCurrentProgress(currentProgress);
    }
    mStateManager.updateProgress(currentProgress);
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
      ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), getTaskList().valueAt(i));
    }
  }

  /**
   * 处理不支持断点的任务
   */
  private void handleNoSupportBP() {
    if (mListener instanceof BaseDListener) {
      ((BaseDListener) mListener).supportBreakpoint(false);
    }
    mStartThreadNum = 1;

    IThreadTask task = createSingThreadTask(mRecord.threadRecords.get(0), 1);
    if (task == null) return;
    getTaskList().put(0, task);
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    mListener.onStart(0);
  }
}
