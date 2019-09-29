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
package com.arialyy.aria.core.common;

import android.os.Handler;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import java.io.File;

/**
 * 子线程下载信息类
 */
public class SubThreadConfig {

  public AbsTaskWrapper taskWrapper;
  public boolean isBlock = false;
  // 启动的线程
  public int startThreadNum;
  public String url;
  public File tempFile;
  // 线程记录
  public ThreadRecord record;
  // 状态处理器
  public Handler stateHandler;
  // 动态文件
  public boolean isOpenDynamicFile;
  // m3u8切片索引
  public int peerIndex;
}