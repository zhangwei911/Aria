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

import android.text.TextUtils;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.IOptionConstant;
import com.arialyy.aria.core.inf.Suggest;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP任务设置
 */
public class HttpDelegate<TARGET extends AbsTarget> extends BaseOption<TARGET> {

  private Map<String, String> params;
  private Map<String, String> headers;

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
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.requestEnum, requestEnum);
    return this;
  }

  /**
   * 设置http请求参数
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setParams(Map<String, String> params) {
    if (this.params == null) {
      this.params = new HashMap<>();
    }
    this.params.putAll(params);
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.params, this.params);
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
    if (params == null) {
      params = new HashMap<>();
    }
    params.put(key, value);
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.params, params);
    return this;
  }

  /**
   * 设置http表单字段
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setFormFields(Map<String, String> params) {
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.formFields, params);
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
    if (this.headers == null) {
      this.headers = new HashMap<>();
    }
    this.headers.put(key, value);
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.headers, this.headers);
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
    if (this.headers == null) {
      this.headers = new HashMap<>();
    }
    this.headers.putAll(headers);
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.headers, this.headers);
    return this;
  }

  /**
   * 设置代理
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public HttpDelegate<TARGET> setUrlProxy(Proxy proxy) {
    getTaskWrapper().getOptionParams().setParams(IOptionConstant.proxy, proxy);
    return this;
  }
}
