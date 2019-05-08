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

import com.arialyy.aria.core.inf.AbsTaskWrapper;
import java.io.File;

/**
 * 子线程下载信息类
 */
public class SubThreadConfig<TASK_WRAPPER extends AbsTaskWrapper> {
  //线程Id
  public int THREAD_ID;
  //文件总长度
  public long TOTAL_FILE_SIZE;
  //子线程启动下载位置
  public long START_LOCATION;
  //子线程结束下载位置
  public long END_LOCATION;
  //下载文件或上传的文件路径
  public File TEMP_FILE;
  //服务器地址
  public String URL;
  public TASK_WRAPPER TASK_WRAPPER;
  public boolean SUPPORT_BP = true;
  public ThreadRecord THREAD_RECORD;
}