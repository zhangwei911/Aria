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

import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.wrapper.ITaskWrapper;
import java.util.List;

/**
 * 责任链
 */
public final class LoaderChain implements ILoaderInterceptor.Chain {
  private ITaskWrapper wrapper;
  private IEventListener listener;
  private TaskRecord taskRecord;
  private int index;
  private List<ILoaderInterceptor> interceptors;

  public LoaderChain(List<ILoaderInterceptor> interceptors, ITaskWrapper wrapper,
      IEventListener listener, TaskRecord taskRecord,
      int index) {
    this.interceptors = interceptors;
    this.wrapper = wrapper;
    this.listener = listener;
    this.taskRecord = taskRecord;
    this.index = index;
  }

  @Override public void updateRecord(TaskRecord record) {
    this.taskRecord = record;
  }

  @Override public TaskRecord getRecord() {
    return taskRecord;
  }

  @Override public IEventListener getListener() {
    return listener;
  }

  @Override public ITaskWrapper getWrapper() {
    return wrapper;
  }

  @Override public ILoader proceed() {
    int index = this.index + 1;
    if (index >= interceptors.size()) {
      throw new AssertionError();
    }

    LoaderChain next = new LoaderChain(interceptors, wrapper, listener, taskRecord, index);
    ILoaderInterceptor interceptor = interceptors.get(index);
    ILoader loader = interceptor.intercept(next);

    if (loader == null) {
      throw new NullPointerException("Loader为空");
    }

    return loader;
  }
}
