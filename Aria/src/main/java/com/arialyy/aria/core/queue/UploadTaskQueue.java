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
import com.arialyy.aria.core.queue.pool.BaseCachePool;
import com.arialyy.aria.core.queue.pool.BaseExecutePool;
import com.arialyy.aria.core.queue.pool.UploadSharePool;
import com.arialyy.aria.core.scheduler.UploadSchedulers;
import com.arialyy.aria.core.upload.UploadTask;
import com.arialyy.aria.core.upload.UploadTaskEntity;
import com.arialyy.aria.util.ALog;

/**
 * Created by lyy on 2017/2/27.
 * 上传任务队列
 */
public class UploadTaskQueue extends AbsTaskQueue<UploadTask, UploadTaskEntity> {
  private static final String TAG = "UploadTaskQueue";
  private static volatile UploadTaskQueue INSTANCE = null;

  public static UploadTaskQueue getInstance() {
    if (INSTANCE == null) {
      synchronized (AriaManager.LOCK) {
        INSTANCE = new UploadTaskQueue();
      }
    }
    return INSTANCE;
  }

  private UploadTaskQueue() {
  }

  @Override BaseCachePool<UploadTask> setCachePool() {
    return UploadSharePool.getInstance().cachePool;
  }

  @Override BaseExecutePool<UploadTask> setExecutePool() {
    return UploadSharePool.getInstance().executePool;
  }

  @Override public int getConfigMaxNum() {
    return AriaManager.getInstance(AriaManager.APP).getUploadConfig().oldMaxTaskNum;
  }

  @Override public int getMaxTaskNum() {
    return AriaManager.getInstance(AriaManager.APP).getUploadConfig().getMaxTaskNum();
  }

  @Override public UploadTask createTask(UploadTaskEntity entity) {
    UploadTask task = null;
    if (mCachePool.getTask(entity.getEntity().getKey()) == null
        && mExecutePool.getTask(entity.getEntity().getKey()) == null) {
      task = (UploadTask) TaskFactory.getInstance()
          .createTask(entity, UploadSchedulers.getInstance());
      mCachePool.putTask(task);
    } else {
      ALog.w(TAG, "任务已存在");
    }
    return task;
  }
}
