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

import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;

/**
 * 任务记录处理适配器
 *
 * @Author lyy
 * @Date 2019-09-19
 */
public interface IRecordHandlerAdapter {

  /**
   * 处理任务记录
   */
  void handlerTaskRecord(TaskRecord record);

  /**
   * 处理线程任务
   *
   * @param record 任务记录
   * @param threadId 线程id
   * @param startL 线程开始位置
   * @param endL 线程结束位置
   */
  ThreadRecord createThreadRecord(TaskRecord record, int threadId, long startL, long endL);

  /**
   * 新任务创建任务记录
   */
  TaskRecord createTaskRecord(int threadNum);

  /**
   * 配置新任务的线程数
   *
   * @return 新任务的线程数
   */
  int initTaskThreadNum();
}
