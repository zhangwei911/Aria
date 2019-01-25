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

import com.arialyy.aria.core.common.AbsFileer;

/**
 * 组合任务子任务队列
 *
 * @param <Fileer> {@link AbsFileer}下载器
 */
interface ISubQueue<Fileer extends AbsFileer> {

  /**
   * 添加任务
   */

  void addTask(Fileer fileer);

  /**
   * 开始任务
   */
  void startTask(Fileer fileer);

  /**
   * 停止任务
   */
  void stopTask(Fileer fileer);

  /**
   * 修改最大任务数
   *
   * @param num 任务数不能小于1
   */
  void modifyMaxExecNum(int num);

  /**
   * 从执行队列中移除任务，一般用于任务完成的情况
   */
  void removeTaskFromExecQ(Fileer fileer);

  /**
   * 删除任务
   */
  void removeTask(Fileer fileer);

  /**
   * 获取下一个任务
   */
  Fileer getNextTask();
}
