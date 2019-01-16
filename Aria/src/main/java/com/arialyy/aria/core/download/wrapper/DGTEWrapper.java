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

import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.orm.AbsWrapper;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.orm.annotation.Many;
import com.arialyy.aria.orm.annotation.One;
import com.arialyy.aria.orm.annotation.Wrapper;
import com.arialyy.aria.util.CommonUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by laoyuyu on 2018/3/30.
 * 任务组实体和任务组任务实体的关系
 */
@Wrapper
public class DGTEWrapper extends AbsWrapper {

  @One
  public DownloadGroupEntity entity;

  @Many(parentColumn = "groupName", entityColumn = "key")
  private List<DGTaskWrapper> taskEntitys;

  public DGTaskWrapper taskEntity;

  @Override protected void handleConvert() {
    taskEntity = (taskEntitys == null || taskEntitys.isEmpty()) ? null : taskEntitys.get(0);
    if (taskEntity != null) {
      taskEntity.setEntity(entity);
      List<DTEWrapper> subWrappers =
          DbEntity.findRelationData(DTEWrapper.class, "DTaskWrapper.groupName=?",
              taskEntity.getKey());
      if (subWrappers != null && !subWrappers.isEmpty()) {
        List<DTaskWrapper> temp = new ArrayList<>();
        for (DTEWrapper dw : subWrappers) {
          if (dw.taskEntity.getRequestType() == AbsTaskWrapper.D_FTP) {
            dw.taskEntity.asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(dw.taskEntity.getUrl()));
          }
          temp.add(dw.taskEntity);
        }
        taskEntity.setSubTaskWrapper(temp);
      }
    }
  }
}
