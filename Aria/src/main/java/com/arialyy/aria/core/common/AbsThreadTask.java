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
package com.arialyy.aria.core.common;

import android.net.TrafficStats;
import android.os.Process;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.config.BaseTaskConfig;
import com.arialyy.aria.core.config.DGroupConfig;
import com.arialyy.aria.core.config.DownloadConfig;
import com.arialyy.aria.core.config.UploadConfig;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.FileException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.ErrorHelp;
import com.arialyy.aria.util.FileUtil;
import com.arialyy.aria.util.NetUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lyy on 2017/1/18. 任务线程
 */
public abstract class AbsThreadTask<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Callable<AbsThreadTask> {
  /**
   * 线程重试次数
   */
  private final int RETRY_NUM = 2;

  private final String TAG = "AbsThreadTask";
  /**
   * 当前子线程相对于总长度的位置
   */
  protected long mChildCurrentLocation = 0;
  protected IEventListener mListener;
  private StateConstance sState;
  private SubThreadConfig<TASK_WRAPPER> sConfig;
  private ENTITY mEntity;
  private TASK_WRAPPER mTaskWrapper;
  private int mFailTimes = 0;
  private long mLastSaveTime;
  private ExecutorService mConfigThreadPool;
  private boolean isNotNetRetry;  //断网情况是否重试
  private boolean taskBreak = false;  //任务跳出
  private int mThreadNum;
  protected BandwidthLimiter mSpeedBandUtil; //速度限制工具
  protected AriaManager mAridManager;
  private boolean isInterrupted = false;

  private Thread mConfigThread = new Thread(new Runnable() {
    @Override public void run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
      final long currentTemp = mChildCurrentLocation;
      writeConfig(false, currentTemp);
    }
  });

  protected AbsThreadTask(StateConstance constance, IEventListener listener,
      SubThreadConfig<TASK_WRAPPER> config) {
    sState = constance;
    sConfig = config;
    mListener = listener;
    mTaskWrapper = getConfig().TASK_WRAPPER;
    mEntity = mTaskWrapper.getEntity();
    mLastSaveTime = System.currentTimeMillis();
    mConfigThreadPool = Executors.newCachedThreadPool();
    mThreadNum = getState().TASK_RECORD.threadRecords.size();
    mAridManager = AriaManager.getInstance(AriaManager.APP);
    if (getMaxSpeed() > 0) {
      mSpeedBandUtil = new BandwidthLimiter(getMaxSpeed(), mThreadNum);
    }
    isNotNetRetry = mAridManager.getAppConfig().isNotNetRetry();
  }

  /**
   * 设置线程是否中断
   *
   * @param isInterrupted {@code true} 中断
   */
  public void setInterrupted(boolean isInterrupted) {
    this.isInterrupted = isInterrupted;
  }

  /**
   * 线程是否存活
   *
   * @return {@code true}存活
   */
  protected boolean isLive() {
    return !Thread.currentThread().isInterrupted() && !isInterrupted;
  }

  /**
   * 当前线程是否完成，对于不支持断点的任务，一律未完成 {@code true} 完成；{@code false} 未完成
   */
  boolean isThreadComplete() {
    return getConfig().THREAD_RECORD.isComplete;
  }

  /**
   * 获取状态信息
   */
  protected StateConstance getState() {
    return sState;
  }

  /**
   * 获取实体
   */
  protected ENTITY getEntity() {
    return mEntity;
  }

  /**
   * 获取任务驱动对象
   */
  protected TASK_WRAPPER getTaskWrapper() {
    return mTaskWrapper;
  }

  /**
   * 获取任务记录
   */
  private TaskRecord getTaskRecord() {
    return getState().TASK_RECORD;
  }

  /**
   * 获取线程记录
   */
  protected ThreadRecord getThreadRecord() {
    return getConfig().THREAD_RECORD;
  }

  /**
   * 获取配置的最大上传/下载速度
   *
   * @return 单位为：kb
   */
  public abstract int getMaxSpeed();

  /**
   * 读取任务配置
   *
   * @return {@link DownloadConfig}、{@link UploadConfig}、{@link DGroupConfig}
   */
  protected abstract BaseTaskConfig getTaskConfig();

  /**
   * 设置最大下载速度
   *
   * @param speed 单位为：kb
   */
  public void setMaxSpeed(int speed) {
    if (mSpeedBandUtil != null) {
      mSpeedBandUtil.setMaxRate(speed / mThreadNum);
    }
  }

  /**
   * 中断任务
   */
  void breakTask() {
    synchronized (AriaManager.LOCK) {
      taskBreak = true;
      if (getConfig().SUPPORT_BP) {
        final long currentTemp = mChildCurrentLocation;
        getState().STOP_NUM++;
        ALog.d(TAG, String.format("任务【%s】thread__%s__中断【停止位置：%s】", getConfig().TEMP_FILE.getName(),
            getConfig().THREAD_ID, currentTemp));
        writeConfig(false, currentTemp);
        if (getState().isStop()) {
          ALog.i(TAG, String.format("任务【%s】已中断", getConfig().TEMP_FILE.getName()));
        }
      } else {
        ALog.i(TAG, String.format("任务【%s】已中断", getConfig().TEMP_FILE.getName()));
      }
    }
  }

  public boolean isInterrupted() {
    return Thread.currentThread().isInterrupted();
  }

  /**
   * 获取线程配置信息
   */
  protected SubThreadConfig<TASK_WRAPPER> getConfig() {
    return sConfig;
  }

  @Override protected void finalize() throws Throwable {
    super.finalize();
    if (mConfigThreadPool != null) {
      mConfigThreadPool.shutdown();
    }
  }

  /**
   * 任务是否中断，中断条件： 1、任务取消 2、任务停止 3、手动中断 {@link #taskBreak}
   *
   * @return {@code true} 中断，{@code false} 不是中断
   */
  protected boolean isBreak() {
    return getState().isCancel || getState().isStop || taskBreak;
  }

  /**
   * 合并文件
   *
   * @return {@code true} 合并成功，{@code false}合并失败
   */
  protected boolean mergeFile() {
    List<String> partPath = new ArrayList<>();
    for (int i = 0, len = getState().TASK_RECORD.threadNum; i < len; i++) {
      partPath.add(String.format(AbsFileer.SUB_PATH, getState().TASK_RECORD.filePath, i));
    }
    boolean isSuccess = FileUtil.mergeFile(getState().TASK_RECORD.filePath, partPath);
    if (isSuccess) {
      for (String pp : partPath) {
        File f = new File(pp);
        if (f.exists()) {
          f.delete();
        }
      }
      File targetFile = new File(getState().TASK_RECORD.filePath);
      if (targetFile.exists() && targetFile.length() > getEntity().getFileSize()) {
        ALog.e(TAG, String.format("任务【%s】分块文件合并失败，下载长度超出文件真实长度，downloadLen: %s，fileSize: %s",
            getConfig().TEMP_FILE.getName(), targetFile.length(), getEntity().getFileSize()));
        return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * 检查下载完成的分块大小，如果下载完成的分块大小大于或小于分配的大小，则需要重新下载该分块 如果是非分块任务，直接返回{@code true}
   *
   * @return {@code true} 分块分大小正常，{@code false} 分块大小错误
   */
  protected boolean checkBlock() {
    if (!getTaskRecord().isBlock) {
      return true;
    }
    ThreadRecord tr = getThreadRecord();
    File blockFile = getBockFile();
    if (!blockFile.exists() || blockFile.length() != tr.blockLen) {
      ALog.i(TAG, String.format("分块【%s】下载错误，即将重新下载该分块，开始位置：%s，结束位置：%s", blockFile.getName(),
          tr.startLocation, tr.endLocation));
      if (blockFile.exists()) {
        blockFile.delete();
        ALog.i(TAG, String.format("删除分块【%s】成功", blockFile.getName()));
      }
      retryThis(isBreak());
      return false;
    }
    return true;
  }

  /**
   * 停止任务
   */
  public void stop() {
    synchronized (AriaManager.LOCK) {
      if (getConfig().SUPPORT_BP) {
        final long stopLocation;
        if (getTaskRecord().isBlock) {
          File blockFile = getBockFile();
          ThreadRecord tr = getThreadRecord();
          long block = getEntity().getFileSize() / getTaskRecord().threadRecords.size();
          stopLocation =
              blockFile.exists() ? (tr.threadId * block + blockFile.length()) : tr.threadId * block;
        } else {
          stopLocation = mChildCurrentLocation;
        }
        getState().STOP_NUM++;
        ALog.d(TAG,
            String.format("任务【%s】thread__%s__停止【当前线程停止位置：%s】", getConfig().TEMP_FILE.getName(),
                getConfig().THREAD_ID, stopLocation));
        writeConfig(false, stopLocation);
        //ALog.d(TAG, String.format("stop_thread_num=%s; start_thread_num=%s; complete_thread_num=%s",
        //    getState().STOP_NUM, getState().START_THREAD_NUM, getState().COMPLETE_THREAD_NUM));
        if (getState().isStop()) {
          ALog.i(TAG, String.format("任务【%s】已停止", getConfig().TEMP_FILE.getName()));
          mListener.onStop(getState().CURRENT_LOCATION);
        }
      } else {
        ALog.i(TAG, String.format("任务【%s】已停止", getConfig().TEMP_FILE.getName()));
        mListener.onStop(getState().CURRENT_LOCATION);
      }
    }
  }

  /**
   * 执行中
   */
  protected void progress(long len) {
    synchronized (AriaManager.LOCK) {
      if (getState().CURRENT_LOCATION > getEntity().getFileSize() && !getTaskWrapper().asHttp()
          .isChunked()) {
        String errorMsg =
            String.format("下载失败，下载长度超出文件真实长度；currentLocation=%s, fileSize=%s",
                getState().CURRENT_LOCATION,
                getEntity().getFileSize());
        taskBreak = true;
        fail(mChildCurrentLocation, new FileException(TAG, errorMsg), false);
        return;
      }
      mChildCurrentLocation += len;
      getState().CURRENT_LOCATION += len;
      if (System.currentTimeMillis() - mLastSaveTime > 5000
          && mChildCurrentLocation < getConfig().END_LOCATION) {
        mLastSaveTime = System.currentTimeMillis();
        if (!mConfigThreadPool.isShutdown()) {
          mConfigThreadPool.execute(mConfigThread);
        }
      }
    }
  }

  /**
   * 取消任务
   */
  public void cancel() {
    synchronized (AriaManager.LOCK) {
      if (getConfig().SUPPORT_BP) {
        getState().CANCEL_NUM++;
        ALog.d(TAG,
            String.format("任务【%s】thread__%s__取消", getConfig().TEMP_FILE.getName(),
                getConfig().THREAD_ID));
        if (getState().isCancel()) {
          if (getConfig().TEMP_FILE.exists() && !(getEntity() instanceof UploadEntity)) {
            getConfig().TEMP_FILE.delete();
          }
          ALog.d(TAG, String.format("任务【%s】已取消", getConfig().TEMP_FILE.getName()));
          mListener.onCancel();
        }
      } else {
        ALog.d(TAG, String.format("任务【%s】已取消", getConfig().TEMP_FILE.getName()));
        mListener.onCancel();
      }
    }
  }

  /**
   * 线程任务失败
   *
   * @param subCurrentLocation 当前线程下载进度
   * @param ex 异常信息
   */
  protected void fail(final long subCurrentLocation, BaseException ex) {
    fail(subCurrentLocation, ex, true);
  }

  /**
   * 任务失败
   *
   * @param subCurrentLocation 当前子线程进度
   */
  protected void fail(final long subCurrentLocation, BaseException ex, boolean needRetry) {
    if (ex != null) {
      ALog.e(TAG, ALog.getExceptionString(ex));
    }
    if (getConfig().SUPPORT_BP) {
      writeConfig(false, subCurrentLocation);
      retryThis(needRetry && getState().START_THREAD_NUM != 1);
    } else {
      ALog.e(TAG, String.format("任务【%s】执行失败", getConfig().TEMP_FILE.getName()));
      ErrorHelp.saveError(TAG, "", ALog.getExceptionString(ex));
      handleFailState(!isBreak());
    }
  }

  /**
   * 重试当前线程，如果其中一条线程已经下载失败，则任务该任务下载失败，并且停止该任务的所有线程
   *
   * @param needRetry 是否可以重试
   */
  private void retryThis(boolean needRetry) {
    if (!NetUtils.isConnected(AriaManager.APP) && !isNotNetRetry) {
      ALog.w(TAG, String.format("任务【%s】thread__%s__重试失败，网络未连接", getConfig().TEMP_FILE.getName(),
          getConfig().THREAD_ID));
    }
    if (mFailTimes < RETRY_NUM && needRetry && (NetUtils.isConnected(AriaManager.APP)
        || isNotNetRetry) && !isBreak()) {
      ALog.w(TAG,
          String.format("任务【%s】thread__%s__正在重试", getConfig().TEMP_FILE.getName(),
              getConfig().THREAD_ID));
      mFailTimes++;
      handleRetryRecord();
      ThreadTaskManager.getInstance().retryThread(AbsThreadTask.this);
    } else {
      handleFailState(!isBreak());
    }
  }

  /**
   * 处理线程重试的记录，只有多线程任务才会执行
   */
  private void handleRetryRecord() {
    if (getTaskRecord().isBlock) {
      ThreadRecord tr = getThreadRecord();
      long block = getEntity().getFileSize() / getTaskRecord().threadRecords.size();

      File blockFile = getBockFile();
      if (blockFile.length() > tr.blockLen) {
        ALog.i(TAG, String.format("分块【%s】错误，将重新下载该分块", blockFile.getPath()));
        blockFile.delete();
        tr.startLocation = block * tr.threadId;
        tr.isComplete = false;
        getConfig().START_LOCATION = tr.startLocation;
      } else if (blockFile.length() < tr.blockLen) {
        tr.startLocation = block * tr.threadId + blockFile.length();
        tr.isComplete = false;
        getConfig().START_LOCATION = tr.startLocation;
        getState().CURRENT_LOCATION = getBlockRealTotalSize();
        ALog.i(TAG, String.format("修正分块【%s】，开始位置：%s，当前进度：%s", blockFile.getPath(), tr.startLocation,
            getState().CURRENT_LOCATION));
      } else {
        getState().COMPLETE_THREAD_NUM++;
        tr.isComplete = true;
      }
      tr.update();
    } else {
      getConfig().START_LOCATION = mChildCurrentLocation == 0 ? getConfig().START_LOCATION
          : getConfig().THREAD_RECORD.startLocation;
    }
  }

  /**
   * 获取分块文件
   *
   * @return 分块文件
   */
  private File getBockFile() {
    return new File(String.format(AbsFileer.SUB_PATH, getState().TASK_RECORD.filePath,
        getThreadRecord().threadId));
  }

  /**
   * 获取分块任务真实的进度
   *
   * @return 进度
   */
  private long getBlockRealTotalSize() {
    long size = 0;
    for (int i = 0, len = getTaskRecord().threadRecords.size(); i < len; i++) {
      File temp = new File(String.format(AbsFileer.SUB_PATH, getTaskRecord().filePath, i));
      if (temp.exists()) {
        size += temp.length();
      }
    }
    return size;
  }

  /**
   * 处理失败状态
   *
   * @param taskNeedReTry 任务是否需要重试{@code true} 需要
   */
  private void handleFailState(boolean taskNeedReTry) {
    getState().FAIL_NUM++;
    if (getState().isFail()) {
      // 手动停止不进行fail回调
      if (!getState().isStop) {
        String errorMsg = String.format("任务【%s】执行失败", getConfig().TEMP_FILE.getName());
        mListener.onFail(taskNeedReTry, new TaskException(TAG, errorMsg));
      }
    }
  }

  /**
   * 将记录写入到配置文件
   *
   * @param isComplete 当前线程是否完成 {@code true}完成
   * @param record 当前下载进度
   */
  protected void writeConfig(boolean isComplete, final long record) {
    ThreadRecord tr = getThreadRecord();
    if (tr != null) {
      tr.isComplete = isComplete;
      if (getTaskRecord().isBlock) {
        tr.startLocation = record;
      } else if (getTaskRecord().isOpenDynamicFile) {
        tr.startLocation = getConfig().TEMP_FILE.length();
      } else {
        if (0 < record && record < getConfig().END_LOCATION) {
          tr.startLocation = record;
        }
      }
      tr.update();
    }
  }

  @Override public AbsThreadTask call() throws Exception {
    isInterrupted = false;
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    TrafficStats.setThreadStatsTag(UUID.randomUUID().toString().hashCode());
    return this;
  }
}
