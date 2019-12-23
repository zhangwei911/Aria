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
package com.arialyy.aria.core.loader;

import com.arialyy.aria.core.common.RecordHandler;
import com.arialyy.aria.core.inf.IRecordHandlerAdapter;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;

/**
 * 任务记录拦截器，用于处理任务记录
 */
public final class RecordInterceptor implements ILoaderInterceptor {
  private IRecordHandlerAdapter adapter;

  public RecordInterceptor(IRecordHandlerAdapter adapter) {
    this.adapter = adapter;
  }

  @Override public ILoader intercept(Chain chain) {
    RecordHandler recordHandler = new RecordHandler((AbsTaskWrapper) chain.getWrapper());
    recordHandler.setAdapter(adapter);
    chain.updateRecord(recordHandler.getRecord());

    return chain.proceed();
  }
}
