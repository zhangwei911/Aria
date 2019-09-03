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

package com.arialyy.aria.core.queue;

import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DownloadGroupTask;
import com.arialyy.aria.core.scheduler.TaskSchedulers;
import com.arialyy.aria.util.ALog;

/**
 * Created by AriaL on 2017/6/29. 任务组下载队列
 */
public class DownloadGroupTaskQueue
    extends AbsTaskQueue<DownloadGroupTask, DGTaskWrapper> {
  private static volatile DownloadGroupTaskQueue INSTANCE = null;

  private final String TAG = "DownloadGroupTaskQueue";

  public static DownloadGroupTaskQueue getInstance() {
    if (INSTANCE == null) {
      synchronized (DownloadGroupTaskQueue.class) {
        INSTANCE = new DownloadGroupTaskQueue();
      }
    }
    return INSTANCE;
  }

  private DownloadGroupTaskQueue() {
  }

  @Override int getQueueType() {
    return TYPE_DG_QUEUE;
  }

  @Override public int getMaxTaskNum() {
    return AriaManager.getInstance().getDGroupConfig().getMaxTaskNum();
  }

  @Override public DownloadGroupTask createTask(DGTaskWrapper wrapper) {
    super.createTask(wrapper);
    DownloadGroupTask task = null;
    if (!mCachePool.taskExits(wrapper.getKey()) && !mExecutePool.taskExits(wrapper.getKey())) {
      task = (DownloadGroupTask) TaskFactory.getInstance()
          .createTask(wrapper, TaskSchedulers.getInstance());
      addTask(task);
    } else {
      ALog.w(TAG, "任务已存在");
    }
    return task;
  }

  @Override public int getOldMaxNum() {
    return AriaManager.getInstance().getDGroupConfig().oldMaxTaskNum;
  }
}
