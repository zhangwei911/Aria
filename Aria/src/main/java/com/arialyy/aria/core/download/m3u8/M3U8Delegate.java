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

import android.support.annotation.CheckResult;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * m3u8 委托
 */
public class M3U8Delegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {
  private DTaskWrapper mTaskWrapper;

  public M3U8Delegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
    mTaskWrapper = (DTaskWrapper) getTaskWrapper();
    mTaskWrapper.setRequestType(AbsTaskWrapper.M3U8_VOD);
  }

  /**
   * 是否合并ts文件，默认合并ts
   *
   * @param merge {@code true}合并所有ts文件为一个
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8Delegate<TARGET> merge(boolean merge) {
    mTaskWrapper.asM3U8().setMergeFile(merge);
    return this;
  }

  /**
   * 如果你希望使用自行处理ts文件的合并，可以使用{@link ITsMergeHandler}处理ts文件的合并
   * 需要注意的是：只有{@link #merge(boolean)}设置合并ts文件，该方法才会生效
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8Delegate<TARGET> setMergeHandler(ITsMergeHandler handler) {
    mTaskWrapper.asM3U8().setMergeHandler(handler);
    return this;
  }

  /**
   * M3U8 ts 文件url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param converter {@link IVodTsUrlConverter}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8Delegate<TARGET> setTsUrlConvert(IVodTsUrlConverter converter) {
    mTaskWrapper.asM3U8().setVodUrlConverter(converter);
    return this;
  }

  /**
   * 选择需要下载的码率，默认下载的码率
   *
   * @param bandWidth 指定的码率
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8Delegate<TARGET> setBandWidth(int bandWidth) {
    mTaskWrapper.asM3U8().setBandWidth(bandWidth);
    return this;
  }

  /**
   * M3U8 bandWidth 码率url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的，
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param converter {@link IBandWidthUrlConverter}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8Delegate<TARGET> setBandWidthUrlConverter(IBandWidthUrlConverter converter) {
    mTaskWrapper.asM3U8().setBandWidthUrlConverter(converter);
    return this;
  }

  /**
   * 处理点播文件的下载参数
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8VodDelegate<TARGET> asVod() {
    return new M3U8VodDelegate<>(mTarget, mTaskWrapper);
  }

  /**
   * 处理直播类的下载
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public M3U8LiveDelegate<TARGET> asLive() {
    return new M3U8LiveDelegate<>(mTarget, mTaskWrapper);
  }
}
