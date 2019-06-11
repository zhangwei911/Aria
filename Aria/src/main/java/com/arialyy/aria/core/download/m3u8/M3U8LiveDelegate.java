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

import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;

/**
 * m3u8直播参数设置
 */
public class M3U8LiveDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {

  M3U8LiveDelegate(TARGET target) {
    super(target);
    mTarget.getTaskWrapper().setRequestType(AbsTaskWrapper.M3U8_LIVE);
  }

  /**
   * M3U8 ts 文件url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param converter {@link ILiveTsUrlConverter}
   */
  public M3U8LiveDelegate setLiveTsUrlConvert(ILiveTsUrlConverter converter) {
    ((DTaskWrapper) mTarget.getTaskWrapper()).asM3U8().setLiveTsUrlConverter(converter);
    return this;
  }

  /**
   * 设置直播的m3u8文件更新间隔，默认10000微秒。
   *
   * @param interval 更新间隔，单位微秒
   */
  public M3U8LiveDelegate setM3U8FileUpdateInterval(long interval) {
    if (interval <= 1) {
      ALog.e(TAG, "间隔时间错误");
      return this;
    }
    ((DTaskWrapper) mTarget.getTaskWrapper()).asM3U8().setLiveUpdateInterval(interval);
    return this;
  }
}
