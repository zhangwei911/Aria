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
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.loader.AbsNormalTTBuilder;
import com.arialyy.aria.core.task.IThreadTaskAdapter;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;

/**
 * @Author lyy
 * @Date 2019-09-19
 */
final class HttpUTTBuilder extends AbsNormalTTBuilder {

  HttpUTTBuilder(AbsTaskWrapper wrapper) {
    super(wrapper);
  }

  @Override public IThreadTaskAdapter getAdapter(SubThreadConfig config) {
    return new HttpUThreadTaskAdapter(config);
  }

  @Override public boolean handleNewTask(TaskRecord record, int totalThreadNum) {
    return true;
  }
}
