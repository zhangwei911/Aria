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
package com.arialyy.aria.core.download.wrapper;

import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.orm.AbsWrapper;
import com.arialyy.aria.orm.annotation.Many;
import com.arialyy.aria.orm.annotation.One;
import com.arialyy.aria.orm.annotation.Wrapper;
import java.util.List;

/**
 * Created by laoyuyu on 2018/3/30.
 */
@Wrapper
public class DTEWrapper extends AbsWrapper {

  @One
  public DownloadEntity entity;

  @Many(parentColumn = "downloadPath", entityColumn = "key")
  private List<DTaskWrapper> taskEntitys = null;

  public DTaskWrapper taskEntity;

  @Override public void handleConvert() {
    taskEntity = (taskEntitys == null || taskEntitys.isEmpty()) ? null : taskEntitys.get(0);
    if (taskEntity != null) {
      taskEntity.setEntity(entity);
    }
  }
}
