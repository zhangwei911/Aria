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

import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.download.wrapper.DGEWrapper;
import com.arialyy.aria.core.download.wrapper.DGTEWrapper;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aria.Lao on 2017/11/1.
 * 任务实体工厂
 */
class DGTEFactory implements IGTEFactory<DownloadGroupEntity, DownloadGroupTaskEntity> {
  private static final String TAG = "DTEFactory";
  private static volatile DGTEFactory INSTANCE = null;

  private DGTEFactory() {
  }

  public static DGTEFactory getInstance() {
    if (INSTANCE == null) {
      synchronized (DGTEFactory.class) {
        INSTANCE = new DGTEFactory();
      }
    }
    return INSTANCE;
  }

  @Override public DownloadGroupTaskEntity getGTE(String groupName, List<String> urls) {
    DownloadGroupEntity entity = createDGroupEntity(groupName, urls);
    List<DGTEWrapper> wrapper =
        DbEntity.findRelationData(DGTEWrapper.class, "DownloadGroupTaskEntity.key=?",
            entity.getGroupName());
    DownloadGroupTaskEntity gte;

    if (wrapper != null && !wrapper.isEmpty()) {
      gte = wrapper.get(0).taskEntity;
      if (gte == null) {
        // 创建新的任务组任务实体
        gte = new DownloadGroupTaskEntity();
      }
    } else {
      gte = new DownloadGroupTaskEntity();
    }
    gte.setSubTaskEntities(createDGSubTaskEntity(entity));
    gte.setKey(entity.getGroupName());
    gte.setEntity(entity);

    return gte;
  }

  @Override public DownloadGroupTaskEntity getFTE(String ftpUrl) {
    List<DGTEWrapper> wrapper =
        DbEntity.findRelationData(DGTEWrapper.class, "DownloadGroupTaskEntity.key=?",
            ftpUrl);
    DownloadGroupTaskEntity fte;

    if (wrapper != null && !wrapper.isEmpty()) {
      fte = wrapper.get(0).taskEntity;
      if (fte == null) {
        fte = new DownloadGroupTaskEntity();
        DownloadGroupEntity dge = new DownloadGroupEntity();
        dge.setGroupName(ftpUrl);
        fte.setEntity(dge);
      } else if (fte.getEntity() == null) {
        DownloadGroupEntity dge = new DownloadGroupEntity();
        dge.setGroupName(ftpUrl);
        fte.setEntity(dge);
      }
    } else {
      fte = new DownloadGroupTaskEntity();
      DownloadGroupEntity dge = new DownloadGroupEntity();
      dge.setGroupName(ftpUrl);
      fte.setEntity(dge);
    }
    fte.setKey(ftpUrl);
    fte.setUrlEntity(CommonUtil.getFtpUrlInfo(ftpUrl));

    if (fte.getEntity().getSubEntities() == null) {
      fte.getEntity().setSubEntities(new ArrayList<DownloadEntity>());
    }
    if (fte.getSubTaskEntities() == null) {
      fte.setSubTaskEntities(new ArrayList<DownloadTaskEntity>());
    }
    return fte;
  }

  /**
   * 创建任务组子任务的任务实体
   */
  private List<DownloadTaskEntity> createDGSubTaskEntity(DownloadGroupEntity dge) {
    List<DownloadTaskEntity> list = new ArrayList<>();
    for (DownloadEntity entity : dge.getSubEntities()) {
      DownloadTaskEntity taskEntity = new DownloadTaskEntity();
      taskEntity.setEntity(entity);
      taskEntity.setKey(entity.getDownloadPath());
      taskEntity.setGroupName(dge.getKey());
      taskEntity.setGroupTask(true);
      taskEntity.setUrl(entity.getUrl());
      list.add(taskEntity);
    }
    return list;
  }

  /**
   * 查询任务组实体，如果数据库不存在该实体，则新创建一个新的任务组实体
   */
  private DownloadGroupEntity createDGroupEntity(String groupName, List<String> urls) {
    List<DGEWrapper> wrapper =
        DbEntity.findRelationData(DGEWrapper.class, "DownloadGroupEntity.groupName=?",
            groupName);

    DownloadGroupEntity groupEntity;
    if (wrapper != null && !wrapper.isEmpty()) {
      groupEntity = wrapper.get(0).groupEntity;
      if (groupEntity == null) {
        groupEntity = new DownloadGroupEntity();
        groupEntity.setSubEntities(createSubTask(groupName, urls));
      }
    } else {
      groupEntity = new DownloadGroupEntity();
      groupEntity.setSubEntities(createSubTask(groupName, urls));
    }
    groupEntity.setGroupName(groupName);
    groupEntity.setUrls(urls);
    return groupEntity;
  }

  /**
   * 创建子任务
   */
  private List<DownloadEntity> createSubTask(String groupName, List<String> urls) {
    List<DownloadEntity> list = new ArrayList<>();
    for (int i = 0, len = urls.size(); i < len; i++) {
      String url = urls.get(i);
      DownloadEntity entity = new DownloadEntity();
      entity.setUrl(url);
      entity.setDownloadPath(groupName + "_" + i);
      int lastIndex = url.lastIndexOf(File.separator);
      entity.setFileName(url.substring(lastIndex + 1, url.length()));
      entity.setGroupName(groupName);
      entity.setGroupChild(true);
      list.add(entity);
    }
    return list;
  }
}
