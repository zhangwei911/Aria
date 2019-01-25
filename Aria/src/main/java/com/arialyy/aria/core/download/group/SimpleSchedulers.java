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

import android.os.CountDownTimer;
import android.os.Message;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.download.downloader.Downloader;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.NetUtils;

/**
 * 组合任务子任务调度器，用于调度任务的开始、停止、失败、完成等情况
 * 该调度器生命周期和{@link AbsGroupUtil}生命周期一致
 */
public class SimpleSchedulers implements ISchedulers<DownloadTask> {
  private static final String TAG = "SimpleSchedulers";
  private SimpleSubQueue mQueue = SimpleSubQueue.newInstance();

  private SimpleSchedulers() {

  }

  public static SimpleSchedulers newInstance() {

    return new SimpleSchedulers();
  }

  @Override public boolean handleMessage(Message msg) {
    Downloader loader = (Downloader) msg.obj;
    switch (msg.what) {
      case ADD:
        mQueue.addTask(loader);
        break;
      case START:
        mQueue.startTask(loader);
        break;
      case STOP:
        mQueue.stopTask(loader);
        startNext();
        break;
      case COMPLETE:
        mQueue.removeTaskFromExecQ(loader);
        startNext();
      case FAIL:
        break;
    }
    return true;
  }

  /**
   * 如果有等待中的任务，则启动下一任务
   */
  private void startNext() {
    Downloader next = mQueue.getNextTask();
    if (next != null) {
      mQueue.startTask(next);
    } else {
      ALog.i(TAG, "没有下一任务");
    }
  }

  /**
   * 处理失败的任务
   */
  private void handleFail(final Downloader loader) {
    Configuration config = Configuration.getInstance();

    long interval = config.downloadCfg.getReTryInterval();
    int num = config.downloadCfg.getReTryNum();
    boolean isNotNetRetry = config.appCfg.isNotNetRetry();

    final int reTryNum = num;
    if ((!NetUtils.isConnected(AriaManager.APP) && !isNotNetRetry)
        || loader.getEntity().getFailNum() > reTryNum) {
      startNext();
      return;
    }

    CountDownTimer timer = new CountDownTimer(interval, 1000) {
      @Override public void onTick(long millisUntilFinished) {

      }

      @Override public void onFinish() {
        AbsEntity entity = loader.getEntity();
        if (entity.getFailNum() <= reTryNum) {
          ALog.d(TAG, String.format("任务【%s】开始重试", loader.getEntity().getFileName()));
          loader.retryTask();
        } else {
          startNext();
        }
      }
    };
    timer.start();
  }
}
