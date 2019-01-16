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
package com.arialyy.aria.core.upload;

import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * Created by lyy on 2017/2/9. 上传任务实体
 */
public class UTaskWrapper extends AbsTaskWrapper<UploadEntity> {

  private UploadEntity entity;

  private String filePath;

  private String key;

  public UTaskWrapper() {
  }

  @Override public UploadEntity getEntity() {
    return entity;
  }

  @Override public String getKey() {
    return key;
  }

  public void setEntity(UploadEntity entity) {
    this.entity = entity;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
