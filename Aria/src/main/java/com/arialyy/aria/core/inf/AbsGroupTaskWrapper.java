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
package com.arialyy.aria.core.inf;

import com.arialyy.aria.core.download.DownloadGroupEntity;
import java.util.List;

/**
 * 组合任务实体包裹器，用于加载和任务相关的参数，如：组合任务实体{@link DownloadGroupEntity}
 */
public abstract class AbsGroupTaskWrapper<ENTITY extends AbsEntity, SUB extends AbsTaskWrapper>
    extends AbsTaskWrapper<ENTITY> {

  public AbsGroupTaskWrapper(ENTITY entity) {
    super(entity);
  }

  public abstract List<SUB> getSubTaskWrapper();

  public abstract void setSubTaskWrapper(List<SUB> subTaskWrapper);
}
