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

package com.arialyy.aria.core.manager;

import com.arialyy.aria.core.common.AbsThreadTask;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 线程管理器
 */
public class ThreadTaskManager {
  private static volatile ThreadTaskManager INSTANCE = null;
  private static final Object LOCK = new Object();
  private final String TAG = "ThreadTaskManager";
  private ExecutorService mExePool;
  private Map<String, Set<Future>> mThreadTasks = new HashMap<>();

  public static ThreadTaskManager getInstance() {
    if (INSTANCE == null) {
      synchronized (LOCK) {
        INSTANCE = new ThreadTaskManager();
      }
    }
    return INSTANCE;
  }

  private ThreadTaskManager() {
    mExePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  }

  /**
   * 启动线程任务
   *
   * @param key 任务对应的key{@link AbsTaskEntity#getKey()}
   * @param threadTask 线程任务{@link AbsThreadTask}
   */
  public synchronized void startThread(String key, AbsThreadTask threadTask) {
    if (mExePool.isShutdown()) {
      ALog.e(TAG, "线程池已经关闭");
      return;
    }
    key = getKey(key);
    Set<Future> temp = mThreadTasks.get(key);
    if (temp == null) {
      temp = new HashSet<>();
      mThreadTasks.put(key, temp);
    }
    temp.add(mExePool.submit(threadTask));
  }

  /**
   * 停止任务的所有线程
   *
   * @param key 任务对应的key{@link AbsTaskEntity#getKey()}
   */
  public synchronized void stopTaskThread(String key) {
    if (mExePool.isShutdown()) {
      ALog.e(TAG, "线程池已经关闭");
      return;
    }
    key = getKey(key);
    Set<Future> temp = mThreadTasks.get(key);
    if (temp != null && temp.size() > 0) {
      try {
        for (Future future : temp) {
          if (future.isDone() || future.isCancelled()) {
            continue;
          }
          AbsThreadTask task = (AbsThreadTask) future.get();
          task.setInterrupted(true);
          future.cancel(true);
        }
      } catch (Exception e) {
        ALog.e(TAG, e);
      }
      temp.clear();
    }
    mThreadTasks.remove(key);
  }

  /**
   * 重试线程任务
   *
   * @param task 线程任务
   */
  public synchronized void retryThread(AbsThreadTask task) {
    if (mExePool.isShutdown()) {
      ALog.e(TAG, "线程池已经关闭");
      return;
    }
    try {
      if (task == null || task.isInterrupted()) {
        ALog.e(TAG, "线程为空或线程已经中断");
        return;
      }
    } catch (Exception e) {
      ALog.e(TAG, e);
      return;
    }
    mExePool.submit(task);
  }

  /**
   * map中的key
   *
   * @param key 任务的key{@link AbsTaskEntity#getKey()}
   * @return 转换后的map中的key
   */
  private String getKey(String key) {
    return CommonUtil.getStrMd5(key);
  }
}
