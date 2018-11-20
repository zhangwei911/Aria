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

import com.arialyy.aria.exception.BaseException;

public interface OnFileInfoCallback {
  /**
   * 处理完成
   *
   * @param info 一些回调的信息
   */
  void onComplete(String url, CompleteInfo info);

  /**
   * 请求失败
   *
   * @param e 错误信息
   */
  void onFail(String url, BaseException e, boolean needRetry);
}