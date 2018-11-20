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
package com.arialyy.aria.core.common.http;

import android.text.TextUtils;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.download.DownloadGroupTarget;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.IPostDelegate;
import com.arialyy.aria.core.inf.ITarget;
import com.arialyy.aria.util.ALog;
import java.util.HashMap;
import java.util.Map;

/**
 * post处理委托类
 */
public class PostDelegate<TARGET extends AbsTarget> implements IPostDelegate<TARGET>, ITarget {
  private static final String TAG = "PostDelegate";
  private TARGET mTarget;

  public PostDelegate(TARGET target) {
    mTarget = target;
    mTarget.getTaskEntity().setRequestEnum(RequestEnum.POST);
  }

  @Override public TARGET setParams(Map<String, String> params) {
    mTarget.getTaskEntity().setParams(params);
    if (mTarget instanceof DownloadGroupTarget) {
      for (DownloadTaskEntity subTask : ((DownloadGroupTaskEntity) mTarget.getTaskEntity()).getSubTaskEntities()) {
        subTask.setParams(params);
      }
    }
    return mTarget;
  }

  @Override public TARGET setParam(String key, String value) {
    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
      ALog.d(TAG, "key 或value 为空");
      return mTarget;
    }
    Map<String, String> params = mTarget.getTaskEntity().getParams();
    if (params == null) {
      params = new HashMap<>();
      mTarget.getTaskEntity().setParams(params);
    }
    params.put(key, value);
    if (mTarget instanceof DownloadGroupTarget) {
      for (DownloadTaskEntity subTask : ((DownloadGroupTaskEntity) mTarget.getTaskEntity()).getSubTaskEntities()) {
        subTask.setParams(params);
      }
    }
    return mTarget;
  }

  @Override public void start() {
    mTarget.start();
  }

  @Override public void stop() {
    mTarget.stop();
  }

  @Override public void resume() {
    mTarget.resume();
  }

  @Override public void cancel() {
    mTarget.cancel();
  }
}
