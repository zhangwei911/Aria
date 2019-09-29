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
package com.arialyy.aria.core;

import com.arialyy.aria.core.inf.IEventHandler;
import com.arialyy.aria.core.inf.IOptionConstant;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务配置参数
 *
 * @Author lyy
 * @Date 2019-09-10
 */
public class TaskOptionParams {

  /**
   * 普通参数
   */
  private Map<String, Object> params = new HashMap<>();

  /**
   * 事件处理对象
   */
  private Map<String, IEventHandler> handler = new HashMap<>();

  /**
   * 设置普通参数
   *
   * @param key {@link IOptionConstant}
   */
  public TaskOptionParams setParams(String key, Object value) {
    params.put(key, value);
    return this;
  }

  /**
   * 设置对象参数
   */
  public TaskOptionParams setObjs(String key, IEventHandler handler) {
    this.handler.put(key, handler);
    return this;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public Object getParam(String key){
    return params.get(key);
  }

  public IEventHandler getHandler(String key){
    return handler.get(key);
  }

  public Map<String, IEventHandler> getHandler() {
    return handler;
  }
}
