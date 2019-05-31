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
 * M3U8 #EXTINF 信息处理器
 */
public interface IM3U8UrlExtInfHandler {

  /**
   * 处理#EXTINF信息，对于某些服务器，返回的切片信息有可能是相对地址，因此，你需要自行转换为可下载http连接
   *
   * @param extInf #EXTINF 切片信息列表
   * @return 根据切片信息转换后的http连接列表，如果你的切片信息是可以直接下载的http连接，直接返回extInf便可
   */
  List<String> handler(List<String> extInf);
}
