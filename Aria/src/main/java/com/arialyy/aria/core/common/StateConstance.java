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

import com.arialyy.aria.util.ALog;

/**
 * Created by lyy on 2017/1/18.
 * 状态常量
 */
public class StateConstance {
  private static final String TAG = "StateConstance";
  public int CANCEL_NUM = 0;
  public int STOP_NUM = 0;
  public int FAIL_NUM = 0;
  public int COMPLETE_THREAD_NUM = 0;
  public int START_THREAD_NUM;  //启动的线程数
  public long CURRENT_LOCATION = 0; //当前下载进度
  public boolean isRunning = false;
  public boolean isCancel = false;
  public boolean isStop = false;
  public TaskRecord TASK_RECORD;

  StateConstance() {
  }

  public void resetState() {
    isCancel = false;
    isStop = false;
    isRunning = false;
    CANCEL_NUM = 0;
    STOP_NUM = 0;
    FAIL_NUM = 0;
    COMPLETE_THREAD_NUM = 0;
    START_THREAD_NUM = 0;
    CURRENT_LOCATION = 0;
  }

  /**
   * 所有子线程是否都已经停止
   */
  public boolean isStop() {
    //ALog.d(TAG, String.format("stop_num=%s; start_thread_num=%s; complete_num=%s", STOP_NUM,
    //    START_THREAD_NUM, COMPLETE_THREAD_NUM));
    return STOP_NUM == START_THREAD_NUM || STOP_NUM + COMPLETE_THREAD_NUM == START_THREAD_NUM;
  }

  /**
   * 所有子线程是否都已经失败
   */
  public boolean isFail() {
    ALog.d(TAG, String.format("fail_num=%s; start_thread_num=%s; complete_num=%s", FAIL_NUM,
        START_THREAD_NUM, COMPLETE_THREAD_NUM));
    return COMPLETE_THREAD_NUM != START_THREAD_NUM
        && (FAIL_NUM == START_THREAD_NUM || FAIL_NUM + COMPLETE_THREAD_NUM == START_THREAD_NUM);
  }

  /**
   * 所有子线程是否都已经完成
   */
  public boolean isComplete() {
    return COMPLETE_THREAD_NUM == START_THREAD_NUM;
  }

  /**
   * 所有子线程是否都已经取消
   */
  public boolean isCancel() {
    return CANCEL_NUM == START_THREAD_NUM;
  }
}
