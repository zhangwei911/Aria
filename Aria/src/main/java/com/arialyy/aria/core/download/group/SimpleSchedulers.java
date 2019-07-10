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

package com.arialyy.aria.core.download.group;

import android.os.Message;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.NetUtils;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 组合任务子任务调度器，用于调度任务的开始、停止、失败、完成等情况
 * 该调度器生命周期和{@link AbsGroupUtil}生命周期一致
 */
class SimpleSchedulers implements ISchedulers {
  private static final String TAG = "SimpleSchedulers";
  private SimpleSubQueue mQueue;
  private GroupRunState mGState;

  private SimpleSchedulers(GroupRunState state) {
    mQueue = state.queue;
    mGState = state;
  }

  static SimpleSchedulers newInstance(GroupRunState state) {

    return new SimpleSchedulers(state);
  }

  @Override public boolean handleMessage(Message msg) {
    SubDLoadUtil loader = (SubDLoadUtil) msg.obj;
    switch (msg.what) {
      case RUNNING:
        mGState.listener.onSubRunning(loader.getEntity());
        break;
      case PRE:
        mGState.listener.onSubPre(loader.getEntity());
        mGState.updateCount(loader.getKey());
        break;
      case START:
        mGState.listener.onSubStart(loader.getEntity());
        break;
      case STOP:
        handleStop(loader);
        break;
      case COMPLETE:
        handleComplete(loader);
        break;
      case FAIL:
        handleFail(loader);
        break;
    }
    return true;
  }

  /**
   * 处理子任务失败的情况
   * 1、子任务失败次数大于等于配置的重试次数，才能认为子任务停止
   * 2、stopNum + failNum + completeNum + cacheNum == subSize，则认为组合任务停止
   * 3、failNum == subSize，只有全部的子任务都失败了，才能任务组合任务失败
   */
  private synchronized void handleFail(final SubDLoadUtil loader) {
    Configuration config = Configuration.getInstance();

    long interval = config.dGroupCfg.getSubReTryInterval();
    int num = config.dGroupCfg.getSubReTryNum();
    boolean isNotNetRetry = config.appCfg.isNotNetRetry();

    final int reTryNum = num;
    if ((!NetUtils.isConnected(AriaManager.APP) && !isNotNetRetry)
        || loader.getEntity().getFailNum() > reTryNum) {
      mQueue.removeTaskFromExecQ(loader);
      mGState.listener.onSubFail(loader.getEntity(), new TaskException(TAG,
          String.format("任务组子任务【%s】下载失败，下载地址【%s】", loader.getEntity().getFileName(),
              loader.getEntity().getUrl())));
      mGState.countFailNum(loader.getKey());
      if (mGState.getFailNum() == mGState.getSubSize()
          || mGState.getStopNum() + mGState.getFailNum() + mGState.getCompleteNum()
          == mGState.getSubSize()) {
        mQueue.clear();
        mGState.isRunning = false;
        mGState.listener.onFail(true, new TaskException(TAG,
            String.format("任务组【%s】下载失败", mGState.getGroupHash())));
      } else {
        startNext();
      }
      return;
    }
    final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);

    timer.schedule(new Runnable() {
      @Override public void run() {
        AbsEntity entity = loader.getEntity();
        if (entity.getFailNum() <= reTryNum) {
          ALog.d(TAG, String.format("任务【%s】开始重试", loader.getEntity().getFileName()));
          loader.reStart();
        } else {
          startNext();
        }
      }
    }, interval, TimeUnit.MILLISECONDS);
  }

  /**
   * 处理子任务停止的情况
   * 1、所有的子任务已经停止，则认为组合任务停止
   * 2、completeNum + failNum + stopNum = subSize，则认为组合任务停止
   */
  private synchronized void handleStop(SubDLoadUtil loader) {
    mGState.listener.onSubStop(loader.getEntity());
    mGState.countStopNum(loader.getKey());
    if (mGState.getStopNum() == mGState.getSubSize()
        || mGState.getStopNum()
        + mGState.getCompleteNum()
        + mGState.getFailNum()
        + mQueue.getCacheSize()
        == mGState.getSubSize()) {
      mQueue.clear();
      mGState.isRunning = false;
      mGState.listener.onStop(mGState.getProgress());
    } else {
      startNext();
    }
  }

  /**
   * 处理子任务完成的情况，有以下三种情况
   * 1、已经没有缓存的子任务，并且停止的子任务是数{@link GroupRunState#getStopNum()} ()}为0，失败的子任数{@link
   * GroupRunState#getFailNum()}为0，则认为组合任务已经完成
   * 2、已经没有缓存的子任务，并且停止的子任务是数{@link GroupRunState#getCompleteNum()}不为0，或者失败的子任数{@link
   * GroupRunState#getFailNum()}不为0，则认为组合任务被停止
   * 3、只有有缓存的子任务，则任务组合任务没有完成
   */
  private synchronized void handleComplete(SubDLoadUtil loader) {
    ALog.d(TAG, String.format("子任务【%s】完成", loader.getEntity().getFileName()));
    ThreadTaskManager.getInstance().removeTaskThread(loader.getKey());
    mGState.listener.onSubComplete(loader.getEntity());
    mQueue.removeTaskFromExecQ(loader);
    mGState.updateCompleteNum();
    ALog.d(TAG, String.format("总任务数：%s，完成的任务数：%s，失败的任务数：%s，停止的任务数：%s", mGState.getSubSize(),
        mGState.getCompleteNum(), mGState.getFailNum(), mGState.getStopNum()));
    if (mGState.getCompleteNum() == mGState.getSubSize()) {
      if (mGState.getStopNum() == 0 && mGState.getFailNum() == 0) {
        mGState.listener.onComplete();
      } else {
        mGState.listener.onStop(mGState.getProgress());
      }
      mQueue.clear();
      mGState.isRunning = false;
    } else {
      startNext();
    }
  }

  /**
   * 如果有等待中的任务，则启动下一任务
   */
  private void startNext() {
    if (mQueue.isStopAll()) {
      return;
    }
    SubDLoadUtil next = mQueue.getNextTask();
    if (next != null) {
      ALog.d(TAG, String.format("启动任务：%s", next.getEntity().getFileName()));
      mQueue.startTask(next);
    } else {
      ALog.i(TAG, "没有下一子任务");
    }
  }
}
