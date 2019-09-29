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
package com.arialyy.aria.ftp.download;

import android.os.Handler;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.loader.NormalLoader;
import com.arialyy.aria.ftp.FtpTaskOption;
import com.arialyy.aria.core.group.AbsSubDLoadUtil;
import com.arialyy.aria.core.group.ChildDLoadListener;

/**
 * @Author lyy
 * @Date 2019-09-28
 */
class SubDLoaderUtil extends AbsSubDLoadUtil {
  /**
   * @param schedulers 调度器
   * @param needGetInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  SubDLoaderUtil(Handler schedulers, DTaskWrapper taskWrapper, boolean needGetInfo) {
    super(schedulers, taskWrapper, needGetInfo);
    taskWrapper.generateTaskOption(FtpTaskOption.class);
  }

  @Override protected NormalLoader createLoader(ChildDLoadListener listener, DTaskWrapper wrapper) {
    NormalLoader loader = new NormalLoader(listener, wrapper);
    FtpDLoaderAdapter adapter = new FtpDLoaderAdapter(wrapper);
    loader.setAdapter(adapter);
    return loader;
  }

  @Override public void start() {
    getDownloader().start();
  }
}
