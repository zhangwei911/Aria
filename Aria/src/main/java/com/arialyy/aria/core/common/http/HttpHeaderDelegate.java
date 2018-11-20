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

import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IHttpHeaderDelegate;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by laoyuyu on 2018/3/9.
 * HTTP header参数设置委托类
 */
public class HttpHeaderDelegate<TARGET extends AbsTarget>
    implements IHttpHeaderDelegate<TARGET> {
  private static final String TAG = "HttpHeaderDelegate";
  private TARGET mTarget;

  public HttpHeaderDelegate(TARGET target) {
    mTarget = target;
  }

  /**
   * 给url请求添加Header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param key header对应的key
   * @param value header对应的value
   */
  @Override
  public TARGET addHeader(@NonNull String key, @NonNull String value) {
    if (TextUtils.isEmpty(key)) {
      ALog.w(TAG, "设置header失败，header对应的key不能为null");
      return mTarget;
    } else if (TextUtils.isEmpty(value)) {
      ALog.w(TAG, "设置header失败，header对应的value不能为null");
      return mTarget;
    }
    addHeader(mTarget.getTaskEntity(), key, value);
    return mTarget;
  }

  /**
   * 给url请求添加一组header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param headers 一组http header数据
   */
  @Override
  public TARGET addHeaders(@NonNull Map<String, String> headers) {
    if (headers.size() == 0) {
      ALog.w(TAG, "设置header失败，map没有header数据");
      return mTarget;
    }
    addHeaders(mTarget.getTaskEntity(), headers);
    return mTarget;
  }

  @Override public TARGET setUrlProxy(Proxy proxy) {
    mTarget.getTaskEntity().setProxy(proxy);
    return mTarget;
  }

  public void addHeader(AbsTaskEntity taskEntity, String key, String value) {
    if (taskEntity.getHeaders().get(key) == null) {
      taskEntity.getHeaders().put(key, value);
    } else if (!taskEntity.getHeaders().get(key).equals(value)) {
      taskEntity.getHeaders().put(key, value);
    }
  }

  public void addHeaders(AbsTaskEntity taskEntity, Map<String, String> headers) {
     /*
      两个map比较逻辑
      1、比对key是否相同
      2、如果key相同，比对value是否相同
      3、只有当上面两个步骤中key 和 value都相同时才能任务两个map数据一致
     */
    boolean mapEquals = false;
    if (taskEntity.getHeaders().size() == headers.size()) {
      int i = 0;
      Set<String> keys = taskEntity.getHeaders().keySet();
      for (String key : keys) {
        if (headers.containsKey(key)) {
          i++;
        } else {
          break;
        }
      }
      if (i == taskEntity.getHeaders().size()) {
        int j = 0;
        Collection<String> values = taskEntity.getHeaders().values();
        for (String value : values) {
          if (headers.containsValue(value)) {
            j++;
          } else {
            break;
          }
        }
        if (j == taskEntity.getHeaders().size()) {
          mapEquals = true;
        }
      }
    }

    if (!mapEquals) {
      taskEntity.getHeaders().clear();
      Set<String> keys = headers.keySet();
      for (String key : keys) {
        taskEntity.getHeaders().put(key, headers.get(key));
      }
    }
  }
}
