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
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.io.File;

/**
 * m3u8 委托
 */
public class M3U8Delegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {
  private DTaskWrapper mTaskWrapper;

  public M3U8Delegate(TARGET target) {
    super(target);
    mTaskWrapper = (DTaskWrapper) mTarget.getTaskWrapper();
    mTaskWrapper.setRequestType(AbsTaskWrapper.M3U8_FILE);
    String filePath = mTaskWrapper.getEntity().getFilePath();
    File file = new File(filePath);
    if (!file.isDirectory()) {
      mTaskWrapper.asM3U8().setCacheDir(file.getParent() + "/." + file.getName());
    }
  }

  /**
   * 设置文件长度，由于m3u8协议的特殊性质，你需要设置文件长度才能获取到正确的下载进度百分比
   *
   * @param fileSize 文件长度
   */
  public M3U8Delegate setFileSize(long fileSize) {
    if (fileSize <= 0) {
      ALog.e(TAG, "文件长度错误");
      return this;
    }
    mTaskWrapper.getEntity().setFileSize(fileSize);
    return this;
  }

  /**
   * 如果你的#EXTINF信息是相对路径或有其它需求，你需要设置extinf处理器，将#EXTINF中的切片信息转换为可下载的http连接
   *
   * @param handler {@link IM3U8UrlExtInfHandler}
   */
  public M3U8Delegate setExtInfHandler(IM3U8UrlExtInfHandler handler) {
    mTaskWrapper.asM3U8().setExtInfHandler(handler);
    return this;
  }

  /**
   * 选择需要下载的码率，默认下载最大码率
   *
   * @param bandWidth 指定的码率
   */
  public M3U8Delegate setBandWidth(int bandWidth) {
    return this;
  }

  /**
   * 处理直播类的下载
   */
  public M3U8LiveDelegate<TARGET> asLive() {
    return new M3U8LiveDelegate<>(mTarget);
  }

  /**
   * 处理需要解码的ts文件
   */
  public M3U8Delegate setDecodeAdapter() {
    return this;
  }
}
