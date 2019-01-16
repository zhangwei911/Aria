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
package com.arialyy.aria.core.manager;

import android.text.TextUtils;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.wrapper.UTEWrapper;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.Regular;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Aria.Lao on 2017/11/1.
 * 任务实体工厂
 */
class UTEFactory implements INormalTEFactory<UploadEntity, UTaskWrapper> {
  private static final String TAG = "DTEFactory";
  private static volatile UTEFactory INSTANCE = null;

  private UTEFactory() {
  }

  public static UTEFactory getInstance() {
    if (INSTANCE == null) {
      synchronized (UTEFactory.class) {
        INSTANCE = new UTEFactory();
      }
    }
    return INSTANCE;
  }

  private UTaskWrapper create(UploadEntity entity) {
    List<UTEWrapper> wrapper =
        DbEntity.findRelationData(UTEWrapper.class, "UTaskWrapper.key=?",
            entity.getFilePath());
    UTaskWrapper uTaskEntity = null;
    if (wrapper != null && !wrapper.isEmpty()) {
      uTaskEntity = wrapper.get(0).taskEntity;
      if (uTaskEntity == null) {
        uTaskEntity = new UTaskWrapper();
        uTaskEntity.setEntity(entity);
      } else if (uTaskEntity.getEntity() == null || TextUtils.isEmpty(
          uTaskEntity.getEntity().getFilePath())) {
        uTaskEntity.setEntity(entity);
      }
    } else {
      uTaskEntity = new UTaskWrapper();
      uTaskEntity.setEntity(entity);
    }
    uTaskEntity.setKey(entity.getFilePath());
    return uTaskEntity;
  }

  @Override public UTaskWrapper create(String key) {
    return create(getUploadEntity(key));
  }

  /**
   * 从数据中读取上传实体，如果数据库查不到，则新创建一个上传实体
   *
   * @param filePath 上传文件的文件路径
   */
  private UploadEntity getUploadEntity(String filePath) {
    UploadEntity entity = UploadEntity.findFirst(UploadEntity.class, "filePath=?", filePath);
    if (entity == null) {
      entity = new UploadEntity();
      String regex = Regular.REG_FILE_NAME;
      Pattern p = Pattern.compile(regex);
      String[] strs = p.split(filePath);
      String fileName = strs[strs.length - 1];
      entity.setFileName(fileName);
      entity.setFilePath(filePath);
    }
    return entity;
  }
}
