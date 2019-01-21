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

import com.arialyy.aria.core.common.AbsFileer;
import com.arialyy.aria.core.common.TaskRecord;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEntity;
import java.io.File;
import java.util.UUID;

/**
 * 创建下载任务wrapper Created by Aria.Lao on 2017/11/1.
 */
class DTaskWrapperFactory implements INormalTEFactory<DownloadEntity, DTaskWrapper> {
  private static final String TAG = "DTaskWrapperFactory";
  private static volatile DTaskWrapperFactory INSTANCE = null;

  private DTaskWrapperFactory() {
  }

  public static DTaskWrapperFactory getInstance() {
    if (INSTANCE == null) {
      synchronized (DTaskWrapperFactory.class) {
        INSTANCE = new DTaskWrapperFactory();
      }
    }
    return INSTANCE;
  }

  /**
   * 通过下载实体创建任务实体
   */
  private DTaskWrapper create(DownloadEntity entity) {
    return new DTaskWrapper(entity);
  }

  /**
   * 通过下载地址创建任务实体
   */
  @Override public DTaskWrapper create(String downloadUrl) {
    return create(getEntity(downloadUrl));
  }

  /**
   * 如果任务存在，但是下载实体不存在，则通过下载地址获取下载实体
   *
   * @param downloadUrl 下载地址
   */
  private DownloadEntity getEntity(String downloadUrl) {
    DownloadEntity entity =
        DownloadEntity.findFirst(DownloadEntity.class, "url=? and isGroupChild='false'",
            downloadUrl);
    if (entity == null) {
      entity = new DownloadEntity();
      entity.setUrl(downloadUrl);
      entity.setGroupChild(false);
      entity.setGroupHash(null);
      entity.setDownloadPath(UUID.randomUUID().toString().replace("-", ""));  //设置临时路径
    }
    File file = new File(entity.getDownloadPath());
    TaskRecord record =
        TaskRecord.findFirst(TaskRecord.class, "filePath=?", entity.getDownloadPath());
    if (record == null) {
      resetEntity(entity);
    } else {
      if (record.isBlock) {
        int count = 0;
        for (int i = 0, len = record.threadNum; i < len; i++) {
          File temp = new File(String.format(AbsFileer.SUB_PATH, record.filePath, i));
          if (!temp.exists()) {
            count++;
          }
        }
        if (count == record.threadNum) {
          resetEntity(entity);
        }
      } else if (!file.exists()) { // 非分块文件需要判断文件是否存在
        resetEntity(entity);
      }
    }
    return entity;
  }

  /**
   * 初始化下载实体
   */
  private void resetEntity(DownloadEntity entity) {
    entity.setPercent(0);
    entity.setCompleteTime(0);
    entity.setComplete(false);
    entity.setCurrentProgress(0);
    entity.setState(IEntity.STATE_WAIT);
  }
}
