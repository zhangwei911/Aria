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
package com.arialyy.aria.http.download;

import android.os.Handler;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.group.AbsSubDLoadUtil;
import com.arialyy.aria.core.group.ChildDLoadListener;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.loader.NormalLoader;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.http.HttpFileInfoThread;

/**
 * @Author lyy
 * @Date 2019-09-28
 */
class HttpSubDLoaderUtil extends AbsSubDLoadUtil {
  /**
   * @param schedulers 调度器
   * @param needGetInfo {@code true} 需要获取文件信息。{@code false} 不需要获取文件信息
   */
  HttpSubDLoaderUtil(Handler schedulers, DTaskWrapper taskWrapper, boolean needGetInfo) {
    super(schedulers, taskWrapper, needGetInfo);
  }

  @Override protected NormalLoader createLoader(ChildDLoadListener listener, DTaskWrapper wrapper) {
    NormalLoader loader = new NormalLoader(listener, wrapper);
    HttpDLoaderAdapter adapter = new HttpDLoaderAdapter(wrapper);
    loader.setAdapter(adapter);
    return loader;
  }

  @Override public void start() {
    if (isNeedGetInfo()) {
      new Thread(new HttpFileInfoThread(getWrapper(), new OnFileInfoCallback() {

        @Override public void onComplete(String url, CompleteInfo info) {
          getDownloader().start();
        }

        @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
          getSchedulers().obtainMessage(ISchedulers.FAIL, HttpSubDLoaderUtil.this).sendToTarget();
        }
      })).start();
    } else {
      getDownloader().start();
    }
  }
}

