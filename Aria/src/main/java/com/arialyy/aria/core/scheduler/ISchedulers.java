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

package com.arialyy.aria.core.scheduler;

import android.os.Handler;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.core.inf.ITask;
import com.arialyy.aria.core.upload.UploadEntity;

/**
 * Created by lyy on 2016/11/2. 调度器功能接口
 */
public interface ISchedulers<Task extends AbsTask> extends Handler.Callback {

  String ARIA_TASK_INFO_ACTION = "ARIA_TASK_INFO_ACTION";
  /**
   * 广播接收器中通过TASK_TYPE字段获取任务类型 {@link ITask#DOWNLOAD}、{@link ITask#DOWNLOAD_GROUP}、{@link
   * ITask#UPLOAD}、{@link ITask#DOWNLOAD_GROUP_SUB}
   */
  String TASK_TYPE = "ARIA_TASK_TYPE";

  /**
   * 广播接收器中通过TASK_STATE字段获取任务状态
   *
   * 普通任务的有： {@link #NO_SUPPORT_BREAK_POINT}、{@link #PRE}、{@link
   * #POST_PRE}、{@link #START}、{@link #STOP}、{@link #FAIL}、{@link #CANCEL}、{@link #COMPLETE}、{@link
   * #RUNNING}、{@link #RESUME}、{@link #WAIT}
   *
   * 子任务的有：{@link #SUB_PRE}、{@link #SUB_START}、{@link
   * #SUB_STOP}、{@link #SUB_CANCEL}、{@link #SUB_FAIL}、{@link #SUB_RUNNING}、{@link #SUB_COMPLETE}
   */
  String TASK_STATE = "ARIA_TASK_STATE";

  /**
   * 广播接收器中通过TASK_ENTITY字段获取任务实体 {@link DownloadEntity}、{@link UploadEntity}、{@link
   * DownloadGroupEntity}
   */
  String TASK_ENTITY = "ARIA_TASK_ENTITY";

  /**
   * 任务速度，单位：byte/s
   */
  String TASK_SPEED = "ARIA_TASK_SPEED";

  /**
   * 任务进度
   */
  String TASK_PERCENT = "ARIA_TASK_PERCENT";

  /**
   * 为组合任务任务
   */
  int IS_SUB_TASK = 0xd1;

  /**
   * 任务不支持断点
   */
  int NO_SUPPORT_BREAK_POINT = 9;
  /**
   * 任务预加载
   */
  int PRE = 0;
  /**
   * 任务预加载完成
   */
  int POST_PRE = 1;

  /**
   * 任务开始
   */
  int START = 2;
  /**
   * 任务停止
   */
  int STOP = 3;
  /**
   * 任务失败
   */
  int FAIL = 4;
  /**
   * 任务取消
   */
  int CANCEL = 5;
  /**
   * 任务完成
   */
  int COMPLETE = 6;
  /**
   * 任务处理中
   */
  int RUNNING = 7;
  /**
   * 恢复任务
   */
  int RESUME = 8;
  /**
   * 等待
   */
  int WAIT = 10;

  /**
   * 组合任务子任务预处理
   */
  int SUB_PRE = 0xa1;

  /**
   * 组合任务子任务开始
   */
  int SUB_START = 0xa2;

  /**
   * 组合任务子任务停止
   */
  int SUB_STOP = 0xa3;

  /**
   * 组合任务子任务取消
   */
  int SUB_CANCEL = 0xa4;

  /**
   * 组合任务子任务失败
   */
  int SUB_FAIL = 0xa5;

  /**
   * 组合任务子任务执行执行中
   */
  int SUB_RUNNING = 0xa6;

  /**
   * 组合任务子任务完成
   */
  int SUB_COMPLETE = 0xa7;

  /**
   * 将当前类注册到Aria
   *
   * @param obj 观察者类
   */
  void register(Object obj);

  /**
   * 移除注册
   *
   * @param obj 观察者类
   */
  void unRegister(Object obj);
}