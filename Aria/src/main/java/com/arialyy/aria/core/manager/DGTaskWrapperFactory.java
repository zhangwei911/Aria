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

import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DGEntityWrapper;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.DbDataHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aria.Lao on 2017/11/1. 组合任务wrapper
 */
class DGTaskWrapperFactory implements IGTEFactory<DownloadGroupEntity, DGTaskWrapper> {
  private static final String TAG = "DTaskWrapperFactory";
  private static volatile DGTaskWrapperFactory INSTANCE = null;

  private DGTaskWrapperFactory() {
  }

  public static DGTaskWrapperFactory getInstance() {
    if (INSTANCE == null) {
      synchronized (DGTaskWrapperFactory.class) {
        INSTANCE = new DGTaskWrapperFactory();
      }
    }
    return INSTANCE;
  }

  @Override public DGTaskWrapper getGTE(String groupName, List<String> urls) {
    DownloadGroupEntity entity = DbDataHelper.getHttpDGEntity(groupName, urls);
    DGTaskWrapper wrapper = new DGTaskWrapper(entity);
    wrapper.setSubTaskWrapper(createDGSubTaskWrapper(entity));
    wrapper.setKey(entity.getGroupName());
    return wrapper;
  }

  @Override public DGTaskWrapper getFTE(String ftpUrl) {
    DownloadGroupEntity entity = DbDataHelper.getFtpDGEntity(ftpUrl);
    DGTaskWrapper fte = new DGTaskWrapper(entity);

    fte.setKey(ftpUrl);
    fte.asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(ftpUrl));

    if (fte.getEntity().getSubEntities() == null) {
      fte.getEntity().setSubEntities(new ArrayList<DownloadEntity>());
    }
    if (fte.getSubTaskWrapper() == null) {
      fte.setSubTaskWrapper(new ArrayList<DTaskWrapper>());
    }
    return fte;
  }

  /**
   * 创建任务组子任务的任务实体
   */
  private List<DTaskWrapper> createDGSubTaskWrapper(DownloadGroupEntity dge) {
    List<DTaskWrapper> list = new ArrayList<>();
    for (DownloadEntity entity : dge.getSubEntities()) {
      DTaskWrapper taskEntity = new DTaskWrapper(entity);
      taskEntity.setKey(entity.getDownloadPath());
      taskEntity.setGroupName(dge.getKey());
      taskEntity.setGroupTask(true);
      taskEntity.setUrl(entity.getUrl());
      list.add(taskEntity);
    }
    return list;
  }


}
