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
package com.arialyy.aria.core.group;

import com.arialyy.aria.core.listener.IDGroupListener;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import java.util.HashSet;
import java.util.Set;

/**
 * 组合任务执行中的状态信息
 */
public class GroupRunState {
  /**
   * 子任务数
   */
  private int mSubSize;

  /**
   * 已经完成的任务数
   */
  private int mCompleteNum;

  /**
   * 失败的任务数
   */
  private int mFailNum;

  /**
   * 停止的任务数
   */
  private int mStopNum;

  /**
   * 当前进度
   */
  private long mProgress;

  /**
   * 组合任务监听
   */
  IDGroupListener listener;

  /**
   * 子任务队列
   */
  SimpleSubQueue queue;

  /**
   * 是否在执行
   */
  boolean isRunning = false;

  /**
   * 子任务失败、停止记录，用于当子任务失败重新被用户点击开始时，更新{@link #mStopNum}或{@link #mFailNum}
   * 保存的数据为：子任务key
   */
  private Set<String> mFailTemp = new HashSet<>(), mStopTemp = new HashSet<>();

  private String mGroupHash;

  GroupRunState(String groupHash, IDGroupListener listener, int subSize,
      SimpleSubQueue queue) {
    this.listener = listener;
    this.queue = queue;
    mSubSize = subSize;
    mGroupHash = groupHash;
  }

  /**
   * 组合任务是否正在自行
   *
   * @return {@code true}组合任务正在执行
   */
  public boolean isRunning() {
    return isRunning;
  }

  public void setRunning(boolean running) {
    isRunning = running;
  }

  String getGroupHash() {
    return mGroupHash;
  }

  /**
   * 获取组合任务子任务数
   */
  public int getSubSize() {
    return mSubSize;
  }

  /**
   * 获取失败的数量
   */
  public int getFailNum() {
    return mFailNum;
  }

  /**
   * 获取停止的数量
   */
  public int getStopNum() {
    return mStopNum;
  }

  /**
   * 获取完成的数量
   */
  public int getCompleteNum() {
    return mCompleteNum;
  }

  /**
   * 获取当前组合任务总进度
   */
  public long getProgress() {
    return mProgress;
  }

  /**
   * 更新完成的数量，mCompleteNum + 1
   */
  public void updateCompleteNum() {
    mCompleteNum++;
  }

  /**
   * 更新任务进度
   */
  public void updateProgress(long newProgress) {
    this.mProgress = newProgress;
  }

  /**
   * 当子任务开始时，更新停止\失败的任务数
   *
   * @param key {@link AbsTaskWrapper#getKey()}
   */
  public void updateCount(String key) {
    if (mFailTemp.contains(key)) {
      mFailTemp.remove(key);
      mFailNum--;
    } else if (mStopTemp.contains(key)) {
      mStopTemp.remove(key);
      mStopNum--;
    }
  }

  /**
   * 统计子任务停止的数量
   *
   * @param key {@link AbsTaskWrapper#getKey()}
   */
  public void countStopNum(String key) {
    mStopTemp.add(key);
    mStopNum++;
  }

  /**
   * 统计子任务失败的数量
   *
   * @param key {@link AbsTaskWrapper#getKey()}
   */
  public void countFailNum(String key) {
    mFailTemp.add(key);
    mFailNum++;
  }
}
