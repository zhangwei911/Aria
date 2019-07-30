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
package com.arialyy.aria.core.download.m3u8;

import androidx.annotation.CheckResult;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.event.PeerIndexEvent;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.util.ALog;

/**
 * m3u8点播文件参数设置
 */
public class M3U8VodDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {
  private DTaskWrapper mTaskWrapper;

  M3U8VodDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
    mTaskWrapper = (DTaskWrapper) getTaskWrapper();
    mTaskWrapper.setRequestType(AbsTaskWrapper.M3U8_VOD);
  }

  /**
   * 由于m3u8协议的特殊性质，无法有效快速获取到正确到文件长度，如果你需要显示文件中长度，你需要自行设置文件长度
   *
   * @param fileSize 文件长度
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8VodDelegate<TARGET> setFileSize(long fileSize) {
    if (fileSize <= 0) {
      ALog.e(TAG, "文件长度错误");
      return this;
    }
    mTaskWrapper.getEntity().setFileSize(fileSize);
    return this;
  }

  /**
   * 默认情况下，对于同一点播文件的下载，最多同时下载4个ts分片，如果你希望增加或减少同时下载的ts分片数量，可以使用该方法设置同时下载的ts分片数量
   *
   * @param num 同时下载的ts分片数量
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8VodDelegate<TARGET> setMaxTsQueueNum(int num) {
    if (num < 1) {
      ALog.e(TAG, "同时下载的分片数量不能小于1");
      return this;
    }
    mTaskWrapper.asM3U8().setMaxTsQueueNum(num);
    return this;
  }

  /**
   * 启动任务时初始化索引位置
   *
   * 优先下载指定索引后的切片
   * 如果指定的切片索引大于切片总数，则此操作无效
   * 如果指定的切片索引小于当前正在下载的切片索引，并且指定索引和当前索引区间内有未下载的切片，则优先下载该区间的切片；否则此操作无效
   * 如果指定索引后的切片已经全部下载完成，但是索引前有未下载的切片，间会自动下载未下载的切片
   *
   * @param index 指定的切片位置
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8VodDelegate<TARGET> setPeerIndex(int index) {
    if (index < 1) {
      ALog.e(TAG, "切片索引不能小于1");
      return this;
    }
    mTaskWrapper.asM3U8().setJumpIndex(index);
    return this;
  }

  /**
   * 任务执行中，跳转索引位置
   * 优先下载指定索引后的切片
   * 如果指定的切片索引大于切片总数，则此操作无效
   * 如果指定的切片索引小于当前正在下载的切片索引，并且指定索引和当前索引区间内有未下载的切片，则优先下载该区间的切片；否则此操作无效
   * 如果指定索引后的切片已经全部下载完成，但是索引前有未下载的切片，间会自动下载未下载的切片
   *
   * @param index 指定的切片位置
   */
  public void jumPeerIndex(int index) {
    if (index < 1) {
      ALog.e(TAG, "切片索引不能小于1");
      return;
    }

    if (!DownloadTaskQueue.getInstance().taskIsRunning(mTaskWrapper.getKey())) {
      ALog.e(TAG,
          String.format("任务【%s】没有运行，如果你希望在启动任务时初始化索引位置，请调用setPeerIndex(xxx）",
              mTaskWrapper.getKey()));
      return;
    }

    EventMsgUtil.getDefault().post(new PeerIndexEvent(index));
  }
}
