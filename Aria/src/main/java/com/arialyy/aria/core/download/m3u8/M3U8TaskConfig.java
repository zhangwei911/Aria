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

import java.util.List;

/**
 * m3u8任务配信息
 */
public class M3U8TaskConfig {

  /**
   * 所有ts文件的下载地址
   */
  private List<String> urls;

  /**
   * #EXTINF 标签信息处理器
   */
  private ITsUrlConverter tsUrlConverter;

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
  private ITsMergeHandler mergeHandler;

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
  private IBandWidthUrlConverter bandWidthUrlConverter;

  /**
   * 码率地址
   */
  private String bandWidthUrl;

  public String getBandWidthUrl() {
    return bandWidthUrl;
  }

  public void setBandWidthUrl(String bandWidthUrl) {
    this.bandWidthUrl = bandWidthUrl;
  }

  public IBandWidthUrlConverter getBandWidthUrlConverter() {
    return bandWidthUrlConverter;
  }

  public void setBandWidthUrlConverter(
      IBandWidthUrlConverter bandWidthUrlConverter) {
    this.bandWidthUrlConverter = bandWidthUrlConverter;
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
    return mergeHandler;
  }

  public void setMergeHandler(ITsMergeHandler mergeHandler) {
    this.mergeHandler = mergeHandler;
  }

  public ITsUrlConverter getTsUrlConverter() {
    return tsUrlConverter;
  }

  public void setTsUrlConverter(ITsUrlConverter tsUrlConverter) {
    this.tsUrlConverter = tsUrlConverter;
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
