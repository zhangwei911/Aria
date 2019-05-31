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
  private IM3U8UrlExtInfHandler extInfHandler;

  /**
   * 缓存目录
   */
  private String cacheDir;

  public IM3U8UrlExtInfHandler getExtInfHandler() {
    return extInfHandler;
  }

  public void setExtInfHandler(IM3U8UrlExtInfHandler extInfHandler) {
    this.extInfHandler = extInfHandler;
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
