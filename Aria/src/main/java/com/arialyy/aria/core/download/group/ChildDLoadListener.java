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

import android.os.Handler;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;

/**
 * 子任务事件监听
 */
class ChildDLoadListener implements IDownloadListener {
  private DownloadEntity subEntity;
  private int RUN_SAVE_INTERVAL = 5 * 1000;  //5s保存一次下载中的进度
  private long lastSaveTime;
  private long lastLen;
  private Handler schedulers;
  private SubDLoadUtil loader;

  ChildDLoadListener(Handler schedulers, SubDLoadUtil loader) {
    this.loader = loader;
    this.schedulers = schedulers;
    subEntity = loader.getEntity();
    subEntity.setFailNum(0);
    lastLen = subEntity.getCurrentProgress();
    lastSaveTime = System.currentTimeMillis();
  }

  @Override public void supportBreakpoint(boolean support) {

  }

  @Override public void onPre() {
    saveData(IEntity.STATE_PRE, subEntity.getCurrentProgress());
  }

  @Override public void onPostPre(long fileSize) {
    subEntity.setFileSize(fileSize);
    subEntity.setConvertFileSize(CommonUtil.formatFileSize(fileSize));
    saveData(IEntity.STATE_POST_PRE, subEntity.getCurrentProgress());
    sendToTarget(ISchedulers.POST_PRE, loader);
  }

  @Override public void onResume(long resumeLocation) {
    lastLen = resumeLocation;
    saveData(IEntity.STATE_POST_PRE, subEntity.getCurrentProgress());
    sendToTarget(ISchedulers.START, loader);
  }

  @Override public void onStart(long startLocation) {
    lastLen = startLocation;
    saveData(IEntity.STATE_POST_PRE, subEntity.getCurrentProgress());
    sendToTarget(ISchedulers.START, loader);
  }

  @Override public void onProgress(long currentLocation) {
    long diff = currentLocation - lastLen;
    //mCurrentLocation += speed;
    subEntity.setCurrentProgress(currentLocation);
    handleSpeed(diff);
    sendToTarget(ISchedulers.RUNNING, loader);
    if (System.currentTimeMillis() - lastSaveTime >= RUN_SAVE_INTERVAL) {
      saveData(IEntity.STATE_RUNNING, currentLocation);
      lastSaveTime = System.currentTimeMillis();
    }
    lastLen = currentLocation;
  }

  @Override public void onStop(long stopLocation) {
    handleSpeed(0);
    saveData(IEntity.STATE_STOP, stopLocation);
    sendToTarget(ISchedulers.STOP, loader);
  }

  /**
   * 组合任务子任务不允许删除
   */
  @Deprecated
  @Override public void onCancel() {

  }

  @Override public void onComplete() {
    subEntity.setComplete(true);
    saveData(IEntity.STATE_COMPLETE, subEntity.getFileSize());
    handleSpeed(0);
    sendToTarget(ISchedulers.COMPLETE, loader);
  }

  @Override public void onFail(boolean needRetry, BaseException e) {
    subEntity.setFailNum(subEntity.getFailNum() + 1);
    saveData(IEntity.STATE_FAIL, subEntity.getCurrentProgress());
    handleSpeed(0);
    sendToTarget(ISchedulers.FAIL, loader);
  }

  private void handleSpeed(long speed) {
    subEntity.setSpeed(speed);
    subEntity.setConvertSpeed(
        speed <= 0 ? "" : String.format("%s/s", CommonUtil.formatFileSize(speed)));
    subEntity.setPercent((int) (subEntity.getFileSize() <= 0 ? 0
        : subEntity.getCurrentProgress() * 100 / subEntity.getFileSize()));
  }

  private void saveData(int state, long location) {
    loader.getWrapper().setState(state);
    subEntity.setState(state);
    subEntity.setComplete(state == IEntity.STATE_COMPLETE);
    if (state == IEntity.STATE_CANCEL) {
      subEntity.deleteData();
    } else if (state == IEntity.STATE_STOP) {
      subEntity.setStopTime(System.currentTimeMillis());
    } else if (subEntity.isComplete()) {
      subEntity.setCompleteTime(System.currentTimeMillis());
      subEntity.setCurrentProgress(subEntity.getFileSize());
    } else if (location > 0) {
      subEntity.setCurrentProgress(location);
    }
  }

  /**
   * 发送状态到子任务调度器{@link SimpleSchedulers}，让调度器处理任务调度
   *
   * @param state {@link ISchedulers}
   */
  private void sendToTarget(int state, SubDLoadUtil util) {
    schedulers.obtainMessage(state, util).sendToTarget();
  }
}