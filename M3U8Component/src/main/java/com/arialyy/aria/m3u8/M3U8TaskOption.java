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
package com.arialyy.aria.m3u8;

import com.arialyy.aria.core.processor.IBandWidthUrlConverter;
import com.arialyy.aria.core.inf.ITaskOption;
import com.arialyy.aria.core.processor.ITsMergeHandler;
import com.arialyy.aria.core.processor.ILiveTsUrlConverter;
import com.arialyy.aria.core.processor.IVodTsUrlConverter;
import java.lang.ref.SoftReference;
import java.util.List;

/**
 * m3u8任务配信息
 */
public class M3U8TaskOption implements ITaskOption {

  /**
   * 所有ts文件的下载地址
   */
  private List<String> urls;

  /**
   * #EXTINF 标签信息处理器
   */
  private SoftReference<IVodTsUrlConverter> vodUrlConverter;

  /**
   * 缓存目录
   */
  private String cacheDir;

  /**
   * 是否合并ts文件 {@code true} 合并ts文件为一个
   */
  private boolean mergeFile = true;

  /**
   * 合并处理器
   */
  private SoftReference<ITsMergeHandler> mergeHandler;

  /**
   * 已完成的ts分片数量
   */
  private int completeNum = 0;

  /**
   * 视频时长，单位s
   */
  private long duration;

  /**
   * 码率
   */
  private int bandWidth = 0;

  /**
   * 码率url转换器
   */
  private SoftReference<IBandWidthUrlConverter> bandWidthUrlConverter;

  /**
   * 码率地址
   */
  private String bandWidthUrl;

  /**
   * 直播下载，ts url转换器
   */
  private SoftReference<ILiveTsUrlConverter> liveTsUrlConverter;

  /**
   * 直播的m3u8文件更新间隔
   */
  private long liveUpdateInterval = 10 * 1000;

  /**
   * 同时下载的分片数量
   */
  private int maxTsQueueNum = 4;

  /**
   * 指定的索引位置
   */
  private int jumpIndex;

  /**
   * 生成索引占位字段
   */
  private boolean generateIndexFileTemp;

  public boolean isGenerateIndexFileTemp() {
    return generateIndexFileTemp;
  }

  public void setGenerateIndexFileTemp(boolean generateIndexFileTemp) {
    this.generateIndexFileTemp = generateIndexFileTemp;
  }

  public int getJumpIndex() {
    return jumpIndex;
  }

  public void setJumpIndex(int jumpIndex) {
    this.jumpIndex = jumpIndex;
  }

  public int getMaxTsQueueNum() {
    return maxTsQueueNum;
  }

  public void setMaxTsQueueNum(int maxTsQueueNum) {
    this.maxTsQueueNum = maxTsQueueNum;
  }

  public long getLiveUpdateInterval() {
    return liveUpdateInterval;
  }

  public void setLiveUpdateInterval(long liveUpdateInterval) {
    this.liveUpdateInterval = liveUpdateInterval;
  }

  public ILiveTsUrlConverter getLiveTsUrlConverter() {
    return liveTsUrlConverter == null ? null : liveTsUrlConverter.get();
  }

  public void setLiveTsUrlConverter(ILiveTsUrlConverter liveTsUrlConverter) {
    this.liveTsUrlConverter = new SoftReference<>(liveTsUrlConverter);
  }

  public String getBandWidthUrl() {
    return bandWidthUrl;
  }

  public void setBandWidthUrl(String bandWidthUrl) {
    this.bandWidthUrl = bandWidthUrl;
  }

  public IBandWidthUrlConverter getBandWidthUrlConverter() {
    return bandWidthUrlConverter == null ? null : bandWidthUrlConverter.get();
  }

  public void setBandWidthUrlConverter(IBandWidthUrlConverter bandWidthUrlConverter) {
    this.bandWidthUrlConverter = new SoftReference<>(bandWidthUrlConverter);
  }

  public int getBandWidth() {
    return bandWidth;
  }

  public void setBandWidth(int bandWidth) {
    this.bandWidth = bandWidth;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public int getCompleteNum() {
    return completeNum;
  }

  public void setCompleteNum(int completeNum) {
    this.completeNum = completeNum;
  }

  public boolean isMergeFile() {
    return mergeFile;
  }

  public void setMergeFile(boolean mergeFile) {
    this.mergeFile = mergeFile;
  }

  public ITsMergeHandler getMergeHandler() {
    return mergeHandler == null ? null : mergeHandler.get();
  }

  public void setMergeHandler(ITsMergeHandler mergeHandler) {
    this.mergeHandler = new SoftReference<>(mergeHandler);
  }

  public IVodTsUrlConverter getVodUrlConverter() {
    return vodUrlConverter == null ? null : vodUrlConverter.get();
  }

  public void setVodUrlConverter(IVodTsUrlConverter vodUrlConverter) {
    this.vodUrlConverter = new SoftReference<>(vodUrlConverter);
  }

  public List<String> getUrls() {
    return urls;
  }

  public void setUrls(List<String> urls) {
    this.urls = urls;
  }

  public String getCacheDir() {
    return cacheDir;
  }

  public void setCacheDir(String cacheDir) {
    this.cacheDir = cacheDir;
  }
}
