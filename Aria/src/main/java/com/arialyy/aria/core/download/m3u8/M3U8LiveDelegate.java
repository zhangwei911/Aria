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
import com.arialyy.aria.core.processor.ILiveTsUrlConverter;
import com.arialyy.aria.core.inf.Suggest;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IOptionConstant;
import com.arialyy.aria.util.ALog;

/**
 * m3u8直播参数设置
 */
public class M3U8LiveDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {

  M3U8LiveDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
    getTaskWrapper().setRequestType(AbsTaskWrapper.M3U8_LIVE);
  }

  /**
   * M3U8 ts 文件url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param converter {@link ILiveTsUrlConverter}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8LiveDelegate<TARGET> setLiveTsUrlConvert(ILiveTsUrlConverter converter) {
    ((DTaskWrapper) getTaskWrapper()).getM3U8Params()
        .setParams(IOptionConstant.liveTsUrlConverter, converter);
    return this;
  }

  /**
   * 设置直播的m3u8文件更新间隔，默认10000微秒。
   *
   * @param interval 更新间隔，单位微秒
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8LiveDelegate<TARGET> setM3U8FileUpdateInterval(long interval) {
    if (interval <= 1) {
      ALog.e(TAG, "间隔时间错误");
      return this;
    }
    ((DTaskWrapper) getTaskWrapper()).getM3U8Params()
        .setParams(IOptionConstant.liveUpdateInterval, interval);
    return this;
  }
}
