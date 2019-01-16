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
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadGroupTarget;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.ITarget;
import com.arialyy.aria.util.ALog;
import java.util.HashMap;
import java.util.Map;

class BaseTarget<TARGET extends AbsTarget> implements ITarget {
  private static final String TAG = "PostDelegate";
  TARGET mTarget;

  public BaseTarget(TARGET target){
    mTarget = target;
  }

  public TARGET setParams(Map<String, String> params) {
    mTarget.getTaskWrapper().asHttp().setParams(params);
    if (mTarget instanceof DownloadGroupTarget) {
      for (DTaskWrapper subTask : ((DGTaskWrapper) mTarget.getTaskWrapper()).getSubTaskWrapper()) {
        subTask.asHttp().setParams(params);
      }
    }
    return mTarget;
  }

  public TARGET setParam(String key, String value) {
    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
      ALog.d(TAG, "key 或value 为空");
      return mTarget;
    }
    Map<String, String> params = mTarget.getTaskWrapper().asHttp().getParams();
    if (params == null) {
      params = new HashMap<>();
      mTarget.getTaskWrapper().asHttp().setParams(params);
    }
    params.put(key, value);
    if (mTarget instanceof DownloadGroupTarget) {
      for (DTaskWrapper subTask : ((DGTaskWrapper) mTarget.getTaskWrapper()).getSubTaskWrapper()) {
        subTask.asHttp().setParams(params);
      }
    }
    return mTarget;
  }

  @Override public void start() {

  }

  @Override public void stop() {

  }

  @Override public void resume() {

  }

  @Override public void cancel() {

  }
}
