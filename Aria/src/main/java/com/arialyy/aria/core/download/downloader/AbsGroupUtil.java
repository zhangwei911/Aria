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
package com.arialyy.aria.core.download.downloader;

import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.NetUtils;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组核心逻辑
 */
public abstract class AbsGroupUtil implements IUtil, Runnable {
  private static final Object LOCK = new Object();
  private final String TAG = "AbsGroupUtil";
  /**
   * FTP文件夹
   */
  int FTP_DIR = 0xa1;
  /**
   * HTTP 任务组
   */
  int HTTP_GROUP = 0xa2;

  /**
   * 任务组所有任务总长度
   */
  long mTotalLen = 0;
  long mCurrentLocation = 0;
  protected IDownloadGroupListener mListener;
  DownloadGroupTaskEntity mGTEntity;
  private boolean isRunning = false;
  private ScheduledThreadPoolExecutor mTimer;
  /**
   * 保存所有没有下载完成的任务，key为下载地址
   */
  Map<String, DownloadTaskEntity> mExeMap = new ConcurrentHashMap<>();

  /**
   * 下载失败的映射表，key为下载地址
   */
  Map<String, DownloadTaskEntity> mFailMap = new ConcurrentHashMap<>();

  /**
   * 该任务组对应的所有任务
   */
  private Map<String, DownloadTaskEntity> mTasksMap = new ConcurrentHashMap<>();

  /**
   * 下载器映射表，key为下载地址
   */
  private Map<String, Downloader> mDownloaderMap = new ConcurrentHashMap<>();

  /**
   * 是否需要读取文件长度，{@code true}需要
   */
  boolean isNeedLoadFileSize = true;
  //已经完成的任务数
  int mCompleteNum = 0;
  //停止的任务数
  private int mStopNum = 0;
  //任务组大小
  int mGroupSize = 0;
  private long mUpdateInterval = 1000;
  private boolean isStop = false, isCancel = false;

  AbsGroupUtil(IDownloadGroupListener listener, DownloadGroupTaskEntity groupEntity) {
    mListener = listener;
    mGTEntity = groupEntity;
    mUpdateInterval =
        AriaManager.getInstance(AriaManager.APP).getDownloadConfig().getUpdateInterval();
  }

  /**
   * 获取任务类型
   *
   * @return {@link #FTP_DIR}、{@link #HTTP_GROUP}
   */
  abstract int getTaskType();

  /**
   * 更新任务组文件大小
   */
  void updateFileSize() {
    if (isNeedLoadFileSize) {
      mGTEntity.getEntity().setFileSize(mTotalLen);
      mGTEntity.getEntity().update();
    }
  }

  /**
   * 启动子任务下载
   *
   * @param url 子任务下载地址
   */
  public void startSubTask(String url) {
    if (!checkSubTask(url, "开始")) return;
    if (!isRunning) {
      startTimer();
    }
    Downloader d = getDownloader(url, false);
    if (d != null && !d.isRunning()) {
      d.setNewTask(false);
      d.start();
    }
  }

  /**
   * 停止子任务下载
   *
   * @param url 子任务下载地址
   */
  public void stopSubTask(String url) {
    if (!checkSubTask(url, "停止")) return;
    Downloader d = getDownloader(url, false);
    if (d != null && d.isRunning()) {
      d.stop();
    }
  }

  /**
   * 删除子任务
   *
   * @param url 子任务下载地址
   */
  public void cancelSubTask(String url) {
    Set<String> urls = mTasksMap.keySet();
    if (!urls.isEmpty() && urls.contains(url)) {
      DownloadTaskEntity det = mTasksMap.get(url);
      if (det != null) {
        mTotalLen -= det.getEntity().getFileSize();
        mCurrentLocation -= det.getEntity().getCurrentProgress();
        mExeMap.remove(det.getKey());
        mFailMap.remove(det.getKey());
        mGroupSize--;
        if (mGroupSize == 0) {
          closeTimer();
          mListener.onCancel();
        }
      }
      mGTEntity.update();
    }
    Downloader d = getDownloader(url, false);
    if (d != null) {
      d.cancel();
    }
  }

  /**
   * 检查子任务
   *
   * @param url 子任务url
   * @param type 任务类型
   * @return {@code true} 任务可以下载
   */
  private boolean checkSubTask(String url, String type) {
    DownloadTaskEntity entity = mTasksMap.get(url);
    if (entity != null) {
      if (entity.getState() == IEntity.STATE_COMPLETE) {
        ALog.w(TAG, "任务【" + url + "】已完成，" + type + "失败");
        return false;
      }
    } else {
      ALog.w(TAG, "任务组中没有该任务【" + url + "】，" + type + "失败");
      return false;
    }
    return true;
  }

  /**
   * 通过地址获取下载器
   *
   * @param url 子任务下载地址
   * @param start 是否启动任务
   */
  private Downloader getDownloader(String url, boolean start) {
    Downloader d = mDownloaderMap.get(url);
    if (d == null) {
      return createChildDownload(mTasksMap.get(url), start);
    }
    return d;
  }

  @Override public long getFileSize() {
    return mTotalLen;
  }

  @Override public long getCurrentLocation() {
    return mCurrentLocation;
  }

  @Override public boolean isRunning() {
    return isRunning;
  }

  @Override public void cancel() {
    isCancel = true;
    closeTimer();
    onCancel();
    Set<String> keys = mDownloaderMap.keySet();
    for (String key : keys) {
      Downloader dt = mDownloaderMap.get(key);
      if (dt != null) {
        dt.cancel();
      }
    }
    clearState();
    mListener.onCancel();
  }

  public void onCancel() {

  }

  @Override public void stop() {
    isStop = true;
    closeTimer();
    onStop();

    Set<String> keys = mDownloaderMap.keySet();
    for (String key : keys) {
      Downloader dt = mDownloaderMap.get(key);
      if (dt != null) {
        dt.stop();
      }
    }
    clearState();
    mListener.onStop(mCurrentLocation);
  }

  protected void onStop() {

  }

  /**
   * 预处理操作
   * 而FTP文件夹的，需要获取完成所有子任务信息才算预处理完成
   */
  protected void onPre() {
    mListener.onPre();
    isRunning = true;
    mGroupSize = mGTEntity.getSubTaskEntities().size();
    mTotalLen = mGTEntity.getEntity().getFileSize();
    isNeedLoadFileSize = mTotalLen <= 10;
    for (DownloadTaskEntity te : mGTEntity.getSubTaskEntities()) {
      File file = new File(te.getKey());
      if (te.getState() == IEntity.STATE_COMPLETE && file.exists()) {
        mCompleteNum++;
        mCurrentLocation += te.getEntity().getFileSize();
      } else {
        mExeMap.put(te.getUrl(), te);
        mCurrentLocation += file.exists() ? te.getEntity().getCurrentProgress() : 0;
      }
      if (isNeedLoadFileSize) {
        mTotalLen += te.getEntity().getFileSize();
      }
      mTasksMap.put(te.getUrl(), te);
    }
  }

  @Override public void start() {
    new Thread(this).start();
  }

  @Override public void run() {
    if (isStop || isCancel) {
      closeTimer();
      return;
    }
    clearState();
    onStart();
  }

  protected void onStart() {

  }

  @Override public void resume() {
    start();
  }

  @Override public void setMaxSpeed(int speed) {
    Set<String> keys = mDownloaderMap.keySet();
    for (String key : keys) {
      Downloader dt = mDownloaderMap.get(key);
      if (dt != null) {
        dt.setMaxSpeed(speed);
      }
    }
  }

  private void clearState() {
    mDownloaderMap.clear();
    mFailMap.clear();
  }

  synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  /**
   * 开始进度流程
   */
  void startRunningFlow() {
    closeTimer();
    mListener.onPostPre(mTotalLen);
    if (mCurrentLocation > 0) {
      mListener.onResume(mCurrentLocation);
    } else {
      mListener.onStart(mCurrentLocation);
    }
    startTimer();
  }

  private synchronized void startTimer() {
    isRunning = true;
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (!isRunning) {
          closeTimer();
        } else if (mCurrentLocation >= 0) {
          long t = 0;
          for (DownloadTaskEntity te : mGTEntity.getSubTaskEntities()) {
            if (te.getState() == IEntity.STATE_COMPLETE) {
              t += te.getEntity().getFileSize();
            } else {
              t += te.getEntity().getCurrentProgress();
            }
          }
          mCurrentLocation = t;
          mListener.onProgress(t);
        }
      }
    }, 0, mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  /**
   * 创建子任务下载器，默认创建完成自动启动
   */
  void createChildDownload(DownloadTaskEntity taskEntity) {
    createChildDownload(taskEntity, true);
  }

  /**
   * 创建子任务下载器，启动子任务下载器
   *
   * @param start 是否启动下载
   */
  private Downloader createChildDownload(DownloadTaskEntity taskEntity, boolean start) {
    ChildDownloadListener listener = new ChildDownloadListener(taskEntity);
    Downloader dt = new Downloader(listener, taskEntity);
    mDownloaderMap.put(taskEntity.getEntity().getUrl(), dt);
    if (start) {
      dt.start();
    }
    return dt;
  }

  /**
   * 子任务事件监听
   */
  private class ChildDownloadListener implements IDownloadListener {
    private DownloadTaskEntity subTaskEntity;
    private DownloadEntity subEntity;
    private int RUN_SAVE_INTERVAL = 5 * 1000;  //5s保存一次下载中的进度
    private long lastSaveTime;
    private long lastLen;
    private ScheduledThreadPoolExecutor timer;
    private boolean isNotNetRetry;

    ChildDownloadListener(DownloadTaskEntity entity) {
      subTaskEntity = entity;
      subEntity = subTaskEntity.getEntity();
      subEntity.setFailNum(0);
      lastLen = subEntity.getCurrentProgress();
      lastSaveTime = System.currentTimeMillis();
      isNotNetRetry = AriaManager.getInstance(AriaManager.APP).getDownloadConfig().isNotNetRetry();
    }

    @Override public void onPre() {
      saveData(IEntity.STATE_PRE, -1);
    }

    @Override public void onPostPre(long fileSize) {
      subEntity.setFileSize(fileSize);
      subEntity.setConvertFileSize(CommonUtil.formatFileSize(fileSize));
      saveData(IEntity.STATE_POST_PRE, -1);
      mListener.onSubPre(subEntity);
    }

    @Override public void onResume(long resumeLocation) {
      saveData(IEntity.STATE_POST_PRE, IEntity.STATE_RUNNING);
      lastLen = resumeLocation;
      mListener.onSubStart(subEntity);
    }

    @Override public void onStart(long startLocation) {
      saveData(IEntity.STATE_POST_PRE, IEntity.STATE_RUNNING);
      lastLen = startLocation;
      mListener.onSubStart(subEntity);
    }

    @Override public void onProgress(long currentLocation) {
      long speed = currentLocation - lastLen;
      //mCurrentLocation += speed;
      subEntity.setCurrentProgress(currentLocation);
      handleSpeed(speed);
      mListener.onSubRunning(subEntity);
      if (System.currentTimeMillis() - lastSaveTime >= RUN_SAVE_INTERVAL) {
        saveData(IEntity.STATE_RUNNING, currentLocation);
        lastSaveTime = System.currentTimeMillis();
      }
      lastLen = currentLocation;
    }

    @Override public void onStop(long stopLocation) {
      saveData(IEntity.STATE_STOP, stopLocation);
      handleSpeed(0);
      mListener.onSubStop(subEntity);
      synchronized (AbsGroupUtil.LOCK) {
        mStopNum++;
        if (mStopNum + mCompleteNum + mFailMap.size() == mGroupSize && !isStop) {
          closeTimer();
          mListener.onStop(mCurrentLocation);
        }
      }
    }

    @Override public void onCancel() {
      saveData(IEntity.STATE_CANCEL, -1);
      handleSpeed(0);
      mListener.onSubCancel(subEntity);
    }

    @Override public void onComplete() {
      subEntity.setComplete(true);
      saveData(IEntity.STATE_COMPLETE, subEntity.getFileSize());
      handleSpeed(0);
      mListener.onSubComplete(subEntity);
      synchronized (AbsGroupUtil.LOCK) {
        mCompleteNum++;
        //如果子任务完成的数量和总任务数一致，表示任务组任务已经完成
        if (mCompleteNum == mGroupSize) {
          closeTimer();
          mListener.onComplete();
        } else if (mFailMap.size() > 0
            && mStopNum + mCompleteNum + mFailMap.size() >= mGroupSize
            && !isStop) {
          //如果子任务完成数量加上失败的数量和总任务数一致，则任务组停止下载
          closeTimer();
          mListener.onStop(mCurrentLocation);
        }
      }
    }

    @Override public void onFail(boolean needRetry, BaseException e) {
      subEntity.setFailNum(subEntity.getFailNum() + 1);
      saveData(IEntity.STATE_FAIL, lastLen);
      handleSpeed(0);
      reTry(needRetry);
    }

    /**
     * 重试下载，只有全部都下载失败才会执行任务组的整体重试，否则只会执行单个子任务的重试
     */
    private void reTry(boolean needRetry) {
      synchronized (AbsGroupUtil.LOCK) {
        Downloader dt = mDownloaderMap.get(subEntity.getUrl());
        if (!isCancel && !isStop && dt != null
            && !dt.isBreak()
            && needRetry
            && subEntity.getFailNum() < 3
            && (NetUtils.isConnected(AriaManager.APP) || isNotNetRetry)) {
          ALog.d(TAG, "downloader retry");
          reStartTask(dt);
        } else {
          mFailMap.put(subTaskEntity.getUrl(), subTaskEntity);
          mListener.onSubFail(subEntity, new TaskException(TAG,
              String.format("任务组子任务【%s】下载失败，下载地址【%s】", subEntity.getFileName(),
                  subEntity.getUrl())));
          if (mFailMap.size() == mExeMap.size() || mFailMap.size() + mCompleteNum == mGroupSize) {
            closeTimer();
          }
          if (mFailMap.size() == mGroupSize) {
            mListener.onFail(true, new TaskException(TAG,
                String.format("任务组【%s】下载失败", mGTEntity.getEntity().getGroupName())));
          } else if (mFailMap.size() + mCompleteNum >= mExeMap.size()) {
            mListener.onStop(mCurrentLocation);
          }
        }
      }
    }

    private void reStartTask(final Downloader dt) {
      if (timer == null || timer.isShutdown()) {
        timer = new ScheduledThreadPoolExecutor(1);
      }
      timer.schedule(new Runnable() {
        @Override public void run() {
          if (dt != null) {
            dt.retryTask();
          }
        }
      }, 5, TimeUnit.SECONDS);
    }

    private void handleSpeed(long speed) {
      subEntity.setSpeed(speed);
      subEntity.setConvertSpeed(
          speed <= 0 ? "" : String.format("%s/s", CommonUtil.formatFileSize(speed)));
      subEntity.setPercent((int) (subEntity.getFileSize() <= 0 ? 0
          : subEntity.getCurrentProgress() * 100 / subEntity.getFileSize()));
    }

    private void saveData(int state, long location) {
      subTaskEntity.setState(state);
      subEntity.setState(state);
      subEntity.setComplete(state == IEntity.STATE_COMPLETE);
      if (state == IEntity.STATE_CANCEL) {
        subEntity.deleteData();
        return;
      } else if (state == IEntity.STATE_STOP) {
        subEntity.setStopTime(System.currentTimeMillis());
      } else if (subEntity.isComplete()) {
        subEntity.setCompleteTime(System.currentTimeMillis());
        subEntity.setCurrentProgress(subEntity.getFileSize());
      } else if (location > 0) {
        subEntity.setCurrentProgress(location);
      }
      subTaskEntity.update();
    }

    @Override public void supportBreakpoint(boolean support) {

    }
  }
}
