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

import com.arialyy.aria.core.common.BaseOption;
import com.arialyy.aria.core.processor.IBandWidthUrlConverter;
import com.arialyy.aria.core.processor.ITsMergeHandler;
import com.arialyy.aria.core.processor.IVodTsUrlConverter;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.ComponentUtil;

/**
 * m3u8任务设置
 */
public class M3U8Option extends BaseOption {

  private boolean generateIndexFileTemp = false;
  private boolean mergeFile = false;
  private int bandWidth;
  private ITsMergeHandler mergeHandler;
  private IVodTsUrlConverter vodUrlConverter;
  private IBandWidthUrlConverter bandWidthUrlConverter;

  M3U8Option() {
    super();
    ComponentUtil.getInstance().checkComponentExist(ComponentUtil.COMPONENT_TYPE_M3U8);
  }

  /**
   * 生成m3u8索引文件
   * 注意：创建索引文件，{@link #merge(boolean)}方法设置与否都不再合并文件
   */
  public M3U8Option generateIndexFile() {
    this.generateIndexFileTemp = true;
    return this;
  }

  /**
   * 是否合并ts文件，默认合并ts
   *
   * @param mergeFile {@code true}合并所有ts文件为一个
   */
  public M3U8Option merge(boolean mergeFile) {
    this.mergeFile = mergeFile;
    return this;
  }

  /**
   * 如果你希望使用自行处理ts文件的合并，可以使用{@link ITsMergeHandler}处理ts文件的合并
   * 需要注意的是：只有{@link #merge(boolean)}设置合并ts文件，该方法才会生效
   */
  public M3U8Option setMergeHandler(ITsMergeHandler mergeHandler) {
    CheckUtil.checkMemberClass(mergeHandler.getClass());
    this.mergeHandler = mergeHandler;
    return this;
  }

  /**
   * M3U8 ts 文件url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param vodUrlConverter {@link IVodTsUrlConverter}
   */
  public M3U8Option setTsUrlConvert(IVodTsUrlConverter vodUrlConverter) {
    CheckUtil.checkMemberClass(vodUrlConverter.getClass());
    this.vodUrlConverter = vodUrlConverter;
    return this;
  }

  /**
   * 选择需要下载的码率，默认下载的码率
   *
   * @param bandWidth 指定的码率
   */
  public M3U8Option setBandWidth(int bandWidth) {
    this.bandWidth = bandWidth;
    return this;
  }

  /**
   * M3U8 bandWidth 码率url转换器，对于某些服务器，返回的ts地址可以是相对地址，也可能是处理过的，
   * 对于这种情况，你需要使用url转换器将地址转换为可正常访问的http地址
   *
   * @param bandWidthUrlConverter {@link IBandWidthUrlConverter}
   */
  public M3U8Option setBandWidthUrlConverter(IBandWidthUrlConverter bandWidthUrlConverter) {
    CheckUtil.checkMemberClass(bandWidthUrlConverter.getClass());
    this.bandWidthUrlConverter = bandWidthUrlConverter;
    return this;
  }
}
