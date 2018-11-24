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
import com.arialyy.aria.core.common.AbsFileer;
import com.arialyy.aria.core.common.TaskRecord;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.download.wrapper.DTEWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.orm.DbEntity;
import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Created by Aria.Lao on 2017/11/1.
 * 任务实体工厂
 */
class DTEFactory implements INormalTEFactory<DownloadEntity, DownloadTaskEntity> {
  private static final String TAG = "DTEFactory";
  private static volatile DTEFactory INSTANCE = null;

  private DTEFactory() {
  }

  public static DTEFactory getInstance() {
    if (INSTANCE == null) {
      synchronized (DTEFactory.class) {
        INSTANCE = new DTEFactory();
      }
    }
    return INSTANCE;
  }

  /**
   * 通过下载实体创建任务实体
   */
  private DownloadTaskEntity create(DownloadEntity entity) {
    List<DTEWrapper> wrapper = DbEntity.findRelationData(DTEWrapper.class,
        "DownloadTaskEntity.key=? and DownloadTaskEntity.isGroupTask='false' and DownloadTaskEntity.url=?",
        entity.getDownloadPath(), entity.getUrl());
    DownloadTaskEntity taskEntity;
    if (wrapper != null && !wrapper.isEmpty()) {
      taskEntity = wrapper.get(0).taskEntity;
      if (taskEntity == null) {
        taskEntity = new DownloadTaskEntity();
      } else if (taskEntity.getEntity() == null || TextUtils.isEmpty(
          taskEntity.getEntity().getUrl())) {
        taskEntity.setEntity(entity);
      }
    } else {
      taskEntity = new DownloadTaskEntity();
    }
    taskEntity.setKey(entity.getDownloadPath());
    taskEntity.setUrl(entity.getUrl());
    taskEntity.setEntity(entity);
    return taskEntity;
  }

  /**
   * 通过下载地址创建任务实体
   */
  @Override public DownloadTaskEntity create(String downloadUrl) {
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
      entity.setGroupName(null);
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
