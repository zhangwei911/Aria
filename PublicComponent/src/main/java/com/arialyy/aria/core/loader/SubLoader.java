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
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.inf.IThreadStateManager;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.List;

/**
 * 子任务加载器
 */
public final class SubLoader implements ILoader, ILoaderVisitor {
  private String TAG = CommonUtil.getClassName(this);
  // 是否需要获取信息
  private boolean needGetInfo = true;
  private Handler schedulers;
  private boolean isCancel = false, isStop = false;
  private AbsTaskWrapper wrapper;
  private IInfoTask infoTask;
  private IThreadTaskBuilder ttBuild;
  private IRecordHandler recordHandler;
  private IThreadTask threadTask;

  public SubLoader(AbsTaskWrapper wrapper, Handler schedulers) {
    this.wrapper = wrapper;
    this.schedulers = schedulers;
  }

  private void handlerTask() {
    List<IThreadTask> task =
        ttBuild.buildThreadTask(recordHandler.getRecord(wrapper.getEntity().getFileSize()),
            schedulers);
    if (task == null || task.isEmpty()) {
      ALog.e(TAG, "创建子任务的线程任务失败，key：" + getKey());
      //schedulers.obtainMessage(ISchedulers.FAIL, SubLoader.this).sendToTarget();
      return;
    }
    threadTask = task.get(0);
    try {
      ThreadTaskManager.getInstance().startThread(getKey(), threadTask);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setNeedGetInfo(boolean needGetInfo) {
    this.needGetInfo = needGetInfo;
  }

  public void retryTask() {
    try {
      if (threadTask != null) {
        threadTask.call();
      } else {
        ALog.e(TAG, "子任务的线程任务为空");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override public void stop() {
    if (isStop) {
      ALog.w(TAG, "子任务已停止");
      return;
    }
    isStop = true;
    threadTask.stop();
  }

  @Override public boolean isRunning() {
    return !threadTask.isBreak();
  }

  @Override public void cancel() {
    if (isCancel) {
      ALog.w(TAG, "子任务已取消");
      return;
    }
    isCancel = true;
    threadTask.cancel();
  }

  @Override public boolean isBreak() {
    if (isCancel || isStop) {
      ALog.d(TAG, "isCancel = " + isCancel + ", isStop = " + isStop);
      ALog.d(TAG, String.format("任务【%s】已停止或取消了", wrapper.getKey()));
      return true;
    }
    return false;
  }

  @Override public String getKey() {
    return wrapper.getKey();
  }

  /**
   * @deprecated 子任务不需要实现这个
   */
  @Deprecated
  @Override public long getCurrentProgress() {
    return 0;
  }

  @Override public void addComponent(IRecordHandler recordHandler) {
    this.recordHandler = recordHandler;
  }

  @Override public void addComponent(IInfoTask infoTask) {
    this.infoTask = infoTask;
    infoTask.setCallback(new IInfoTask.Callback() {
      @Override public void onSucceed(String key, CompleteInfo info) {
        handlerTask();
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        schedulers.obtainMessage(ISchedulers.FAIL, SubLoader.this).sendToTarget();
      }
    });
  }

  /**
   * @deprecated 子任务不需要实现这个
   */
  @Override public void addComponent(IThreadStateManager threadState) {
    // 子任务不需要实现这个
  }

  @Override public void addComponent(IThreadTaskBuilder builder) {
    ttBuild = builder;
  }

  @Override public void run() {
    checkComponent();
    if (isBreak()) {
      return;
    }
    if (needGetInfo) {
      infoTask.run();
    } else {
      handlerTask();
    }
  }

  /**
   * 检查组件:  {@link #recordHandler}、{@link #infoTask}、{@link #ttBuild}
   */
  private void checkComponent() {
    if (recordHandler == null) {
      throw new NullPointerException("任务记录组件为空");
    }
    if (infoTask == null) {
      throw new NullPointerException(("文件信息组件为空"));
    }
    if (ttBuild == null) {
      throw new NullPointerException("线程任务组件为空");
    }
  }
}
