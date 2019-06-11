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
package com.arialyy.aria.core.inf;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by AriaL on 2017/6/29.
 */
public abstract class AbsTask<TASK_WRAPPER extends AbsTaskWrapper>
    implements ITask<TASK_WRAPPER> {
  public static final String ERROR_INFO_KEY = "ERROR_INFO_KEY";

  /**
   * 是否需要重试，默认为false
   */
  private boolean needRetry = true;
  protected TASK_WRAPPER mTaskWrapper;
  protected Handler mOutHandler;
  protected Context mContext;
  boolean isHeighestTask = false;
  private boolean isCancel = false, isStop = false;
  private IUtil mUtil;
  /**
   * 扩展信息
   */
  private Map<String, Object> mExpand = new HashMap<>();
  /**
   * 该任务的调度类型
   */
  @TaskSchedulerType
  private int mSchedulerType = TaskSchedulerType.TYPE_DEFAULT;
  protected IEventListener mListener;
  protected String TAG;

  protected AbsTask() {
    TAG = CommonUtil.getClassName(this);
  }

  public Handler getOutHandler() {
    return mOutHandler;
  }

  protected abstract IUtil createUtil();

  protected synchronized IUtil getUtil() {
    if (mUtil == null) {
      mUtil = createUtil();
    }
    return mUtil;
  }

  /**
   * 添加扩展数据 读取扩展数据{@link #getExpand(String)}
   */
  public void putExpand(String key, Object obj) {
    if (TextUtils.isEmpty(key)) {
      ALog.e(TAG, "key 为空");
      return;
    } else if (obj == null) {
      ALog.e(TAG, "扩展数据为空");
      return;
    }
    mExpand.put(key, obj);
  }

  @Override public boolean isNeedRetry() {
    return needRetry;
  }

  public void setNeedRetry(boolean needRetry) {
    this.needRetry = needRetry;
  }

  /**
   * 读取扩展数据
   */
  @Override
  public Object getExpand(String key) {
    return mExpand.get(key);
  }

  /**
   * 设置最大下载/上传速度
   *
   * @param speed 单位为：kb
   */
  public void setMaxSpeed(int speed) {
    if (getUtil() != null) {
      getUtil().setMaxSpeed(speed);
    }
  }

  /**
   * 任务是否完成
   *
   * @return {@code true} 已经完成，{@code false} 未完成
   */
  public boolean isComplete() {
    return mTaskWrapper.getEntity().isComplete();
  }

  /**
   * 获取当前下载进度
   */
  public long getCurrentProgress() {
    return mTaskWrapper.getEntity().getCurrentProgress();
  }

  /**
   * 获取单位转换后的进度
   *
   * @return 如：已经下载3mb的大小，则返回{@code 3mb}
   */
  public String getConvertCurrentProgress() {
    if (mTaskWrapper.getEntity().getCurrentProgress() == 0) {
      return "0b";
    }
    return CommonUtil.formatFileSize(mTaskWrapper.getEntity().getCurrentProgress());
  }

  /**
   * 转换单位后的文件长度
   *
   * @return 如果文件长度为0，则返回0m，否则返回转换后的长度1b、1kb、1mb、1gb、1tb
   */
  public String getConvertFileSize() {
    if (mTaskWrapper.getEntity().getFileSize() == 0) {
      return "0mb";
    }
    return CommonUtil.formatFileSize(mTaskWrapper.getEntity().getFileSize());
  }

  /**
   * 获取文件大小
   */
  public long getFileSize() {
    return mTaskWrapper.getEntity().getFileSize();
  }

  /**
   * 获取百分比进度
   *
   * @return 返回百分比进度，如果文件长度为0，返回0
   */
  public int getPercent() {
    return mTaskWrapper.getEntity().getPercent();
  }

  /**
   * 任务当前状态
   *
   * @return {@link IEntity}
   */
  public int getState() {
    return mTaskWrapper.getState();
  }

  /**
   * 获取保存的扩展字段
   *
   * @return 如果实体不存在，则返回null，否则返回扩展字段
   */
  public String getExtendField() {
    return mTaskWrapper.getEntity() == null ? null : mTaskWrapper.getEntity().getStr();
  }

  @Override public void start() {
    start(TaskSchedulerType.TYPE_DEFAULT);
  }

  @Override public void start(@TaskSchedulerType int type) {
    mSchedulerType = type;
    if (type == TaskSchedulerType.TYPE_START_AND_RESET_STATE) {
      if (getUtil().isRunning()) {
        ALog.e(TAG, String.format("任务【%s】重启失败", getTaskName()));
        return;
      }
      mUtil = createUtil();
      mUtil.start();
      ALog.d(TAG, String.format("任务【%s】重启成功", getTaskName()));
      return;
    }
    if (getUtil().isRunning()) {
      ALog.d(TAG, "任务正在下载");
    } else {
      getUtil().start();
    }
  }

  @Override public void stop() {
    stop(TaskSchedulerType.TYPE_DEFAULT);
  }

  @Override public void stop(@TaskSchedulerType int type) {
    isStop = true;
    mSchedulerType = type;
    if (getUtil().isRunning()) {
      getUtil().stop();
    } else {
      ALog.d(TAG, "下载任务未执行");
      mListener.onStop(mTaskWrapper.getEntity().getCurrentProgress());
    }
  }

  @Override public void cancel() {
    cancel(TaskSchedulerType.TYPE_DEFAULT);
  }

  @Override public void cancel(@TaskSchedulerType int type) {
    isCancel = true;
    mSchedulerType = type;
    if (!getUtil().isRunning()) {
      mListener.onCancel();
    } else {
      getUtil().cancel();
    }
  }

  /**
   * 是否真正下载
   *
   * @return {@code true} 正在下载
   */
  @Override public boolean isRunning() {
    return getUtil().isRunning();
  }

  /**
   * 任务的调度类型
   */
  @Override
  public int getSchedulerType() {
    return mSchedulerType;
  }

  /**
   * 任务是否取消了
   *
   * @return {@code true}任务已经取消
   */
  @Override
  public boolean isCancel() {
    return isCancel;
  }

  /**
   * 任务是否停止了
   *
   * @return {@code true}任务已经停止
   */
  @Override
  public boolean isStop() {
    return isStop;
  }

  /**
   * @return 返回原始byte速度，需要你在配置文件中配置
   * <pre>
   *   {@code
   *    <xml>
   *      <download>
   *        ...
   *        <convertSpeed value="false"/>
   *      </download>
   *
   *      或在代码中设置
   *      Aria.get(this).getDownloadConfig().setConvertSpeed(false);
   *    </xml>
   *   }
   * </pre>
   * 才能生效
   */
  public long getSpeed() {
    return mTaskWrapper.getEntity().getSpeed();
  }

  /**
   * @return 返回转换单位后的速度，需要你在配置文件中配置，转换完成后为：1b/s、1kb/s、1mb/s、1gb/s、1tb/s
   * <pre>
   *   {@code
   *    <xml>
   *      <download>
   *        ...
   *        <convertSpeed value="true"/>
   *      </download>
   *
   *      或在代码中设置
   *      Aria.get(this).getDownloadConfig().setConvertSpeed(true);
   *    </xml>
   *   }
   * </pre>
   * 才能生效
   */
  public String getConvertSpeed() {
    return mTaskWrapper.getEntity().getConvertSpeed();
  }

  @Override public TASK_WRAPPER getTaskWrapper() {
    return mTaskWrapper;
  }

  public boolean isHighestPriorityTask() {
    return isHeighestTask;
  }
}
