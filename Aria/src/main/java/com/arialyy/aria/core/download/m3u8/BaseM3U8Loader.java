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

import com.arialyy.aria.core.common.AbsFileer;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

public abstract class BaseM3U8Loader extends AbsFileer<DownloadEntity, DTaskWrapper> {

  BaseM3U8Loader(IEventListener listener, DTaskWrapper wrapper) {
    super(listener, wrapper);
    mTempFile = new File(wrapper.getEntity().getFilePath());
  }

  @Override protected long delayTimer() {
    return 1000;
  }

  /**
   * 获取ts文件保存路径
   *
   * @param dirCache 缓存目录
   * @param threadId ts文件名
   */
  public static String getTsFilePath(String dirCache, int threadId) {
    return String.format("%s/%s.ts", dirCache, threadId);
  }

  String getCacheDir() {
    String cacheDir = mTaskWrapper.asM3U8().getCacheDir();
    if (!new File(cacheDir).exists()) {
      CommonUtil.createDir(cacheDir);
    }
    return cacheDir;
  }
}
