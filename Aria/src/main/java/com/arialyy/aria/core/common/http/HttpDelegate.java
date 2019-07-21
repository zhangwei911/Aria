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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * HTTP协议处理
 */
public class HttpDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {

  public HttpDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
  }

  /**
   * 设置请求类型
   *
   * @param requestEnum {@link RequestEnum}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setRequestType(RequestEnum requestEnum) {
    getTaskWrapper().asHttp().setRequestEnum(requestEnum);
    return this;
  }

  /**
   * 设置http请求参数
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setParams(Map<String, String> params) {
    getTaskWrapper().asHttp().setParams(params);
    return this;
  }

  /**
   * 设置http请求参数
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setParam(String key, String value) {
    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
      ALog.d(TAG, "key 或value 为空");
      return this;
    }
    Map<String, String> params = getTaskWrapper().asHttp().getParams();
    if (params == null) {
      params = new HashMap<>();
      getTaskWrapper().asHttp().setParams(params);
    }
    params.put(key, value);
    return this;
  }

  /**
   * 给url请求添加Header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param key header对应的key
   * @param value header对应的value
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> addHeader(@NonNull String key, @NonNull String value) {
    if (TextUtils.isEmpty(key)) {
      ALog.w(TAG, "设置header失败，header对应的key不能为null");
      return this;
    } else if (TextUtils.isEmpty(value)) {
      ALog.w(TAG, "设置header失败，header对应的value不能为null");
      return this;
    }
    addHeader(getTaskWrapper(), key, value);
    return this;
  }

  /**
   * 给url请求添加一组header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param headers 一组http header数据
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> addHeaders(@NonNull Map<String, String> headers) {
    if (headers.size() == 0) {
      ALog.w(TAG, "设置header失败，map没有header数据");
      return this;
    }
    addHeaders(getTaskWrapper(), headers);
    return this;
  }

  /**
   * 设置代理
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setUrlProxy(Proxy proxy) {
    getTaskWrapper().asHttp().setProxy(proxy);
    return this;
  }

  private void addHeader(AbsTaskWrapper taskWrapper, String key, String value) {
    HttpTaskConfig taskDelegate = taskWrapper.asHttp();
    if (taskDelegate.getHeaders().get(key) == null) {
      taskDelegate.getHeaders().put(key, value);
    } else if (!taskDelegate.getHeaders().get(key).equals(value)) {
      taskDelegate.getHeaders().put(key, value);
    }
  }

  private void addHeaders(AbsTaskWrapper taskWrapper, Map<String, String> headers) {
    HttpTaskConfig taskDelegate = taskWrapper.asHttp();
     /*
      两个map比较逻辑
      1、比对key是否相同
      2、如果key相同，比对value是否相同
      3、只有当上面两个步骤中key 和 value都相同时才能任务两个map数据一致
     */
    boolean mapEquals = false;
    if (taskDelegate.getHeaders().size() == headers.size()) {
      int i = 0;
      Set<String> keys = taskDelegate.getHeaders().keySet();
      for (String key : keys) {
        if (headers.containsKey(key)) {
          i++;
        } else {
          break;
        }
      }
      if (i == taskDelegate.getHeaders().size()) {
        int j = 0;
        Collection<String> values = taskDelegate.getHeaders().values();
        for (String value : values) {
          if (headers.containsValue(value)) {
            j++;
          } else {
            break;
          }
        }
        if (j == taskDelegate.getHeaders().size()) {
          mapEquals = true;
        }
      }
    }

    if (!mapEquals) {
      taskDelegate.getHeaders().clear();
      Set<String> keys = headers.keySet();
      for (String key : keys) {
        taskDelegate.getHeaders().put(key, headers.get(key));
      }
    }
  }
}
