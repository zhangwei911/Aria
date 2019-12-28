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
package com.arialyy.aria.http.upload;

import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.common.RecordHandler;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.loader.IRecordHandler;
import com.arialyy.aria.core.task.AbsNormalLoaderAdapter;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.task.ThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.core.wrapper.ITaskWrapper;
import com.arialyy.aria.http.HttpRecordHandler;

/**
 * @Author lyy
 * @Date 2019-09-19
 */
final class HttpULoaderAdapter extends AbsNormalLoaderAdapter {
  public HttpULoaderAdapter(ITaskWrapper wrapper) {
    super(wrapper);
  }

  @Override public boolean handleNewTask(TaskRecord record, int totalThreadNum) {
    return true;
  }

  @Override public IThreadTask createThreadTask(SubThreadConfig config) {
    ThreadTask task = new ThreadTask(config);
    HttpUThreadTaskAdapter adapter = new HttpUThreadTaskAdapter(config);
    task.setAdapter(adapter);
    return task;
  }

  @Override public IRecordHandler recordHandler(AbsTaskWrapper wrapper) {
    RecordHandler recordHandler = new RecordHandler(wrapper);
    HttpRecordHandler adapter = new HttpRecordHandler(wrapper);
    recordHandler.setAdapter(adapter);
    return recordHandler;
  }
}
