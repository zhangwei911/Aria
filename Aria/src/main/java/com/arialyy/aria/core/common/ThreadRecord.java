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

import com.arialyy.aria.orm.ActionPolicy;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.orm.annotation.Foreign;

/**
 * Created by laoyuyu on 2018/5/8.
 * 任务的线程记录
 */
public class ThreadRecord extends DbEntity {
  @Foreign(parent = TaskRecord.class, column = "filePath", onUpdate = ActionPolicy.CASCADE, onDelete = ActionPolicy.CASCADE)
  public String key;

  /**
   * 开始位置
   */
  public long startLocation;

  /**
   * 结束位置
   */
  public long endLocation;

  /**
   * 线程是否完成
   * {@code true}完成，{@code false}未完成
   */
  public boolean isComplete = false;

  /**
   * 线程id
   */
  public int threadId = -1;

  /**
   * 分块长度
   */
  public long blockLen = 0;
}
