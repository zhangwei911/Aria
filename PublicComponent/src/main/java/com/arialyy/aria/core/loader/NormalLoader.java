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

import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.inf.IThreadState;
import com.arialyy.aria.core.listener.IDLoadListener;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import java.io.File;

/**
 * 单文件
 */
public class NormalLoader extends AbsLoader {
  private int mStartThreadNum; //启动的线程数

  public NormalLoader(AbsTaskWrapper wrapper, IEventListener listener) {
    super(wrapper, listener);
    mTempFile = new File(getEntity().getFilePath());
    EventMsgUtil.getDefault().register(this);
    setUpdateInterval(wrapper.getConfig().getUpdateInterval());
  }

  public AbsNormalEntity getEntity() {
    return (AbsNormalEntity) mTaskWrapper.getEntity();
  }

  @Override public long getFileSize() {
    return getEntity().getFileSize();
  }

  /**
   * 设置最大下载/上传速度AbsFtpInfoThread
   *
   * @param maxSpeed 单位为：kb
   */
  protected void setMaxSpeed(int maxSpeed) {
    for (IThreadTask threadTask : getTaskList()) {
      if (threadTask != null && mStartThreadNum > 0) {
        threadTask.setMaxSpeed(maxSpeed / mStartThreadNum);
      }
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    EventMsgUtil.getDefault().unRegister(this);
  }

  @Override protected void onPostPre() {
    super.onPostPre();
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

  /**
   * 启动单线程任务
   */
  @Override
  public void handleTask() {
    if (isBreak()) {
      return;
    }
    mStateManager.setLooper(mRecord, getLooper());
    mInfoTask.run();
  }

  private void startThreadTask() {
    getTaskList().addAll(mTTBuilder.buildThreadTask(mRecord, getLooper(), mStateManager));
    mStartThreadNum = mTTBuilder.getCreatedThreadNum();
    if (mStateManager.getCurrentProgress() > 0) {
      mListener.onResume(mStateManager.getCurrentProgress());
    } else {
      mListener.onStart(mStateManager.getCurrentProgress());
    }

    for (IThreadTask threadTask : getTaskList()) {
      ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), threadTask);
    }
  }

  @Override public long getCurrentProgress() {
    return isRunning() ? mStateManager.getCurrentProgress() : getEntity().getCurrentProgress();
  }

  @Override public void addComponent(IRecordHandler recordHandler) {
    mRecordHandler = recordHandler;
    mRecord = mRecordHandler.getRecord();
    if (recordHandler.checkTaskCompleted()) {
      mRecord.deleteData();
      mListener.onComplete();
    }
  }

  @Override public void addComponent(IInfoTask infoTask) {
    mInfoTask = infoTask;
    infoTask.setCallback(new IInfoTask.Callback() {
      @Override public void onSucceed(String key, CompleteInfo info) {
        startThreadTask();
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        mListener.onFail(needRetry, e);
      }
    });
  }

  @Override public void addComponent(IThreadState threadState) {
    mStateManager = threadState;
  }

  @Override public void addComponent(IThreadTaskBuilder builder) {
    mTTBuilder = builder;
  }
}
