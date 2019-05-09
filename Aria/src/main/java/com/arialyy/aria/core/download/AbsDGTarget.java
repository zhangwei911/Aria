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
package com.arialyy.aria.core.download;

import android.support.annotation.CheckResult;
import android.text.TextUtils;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.manager.SubTaskManager;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyy on 2017/7/26.
 */
abstract class AbsDGTarget<TARGET extends AbsDGTarget> extends AbsTarget<TARGET> {

  private SubTaskManager mSubTaskManager;

  /**
   * 获取子任务管理器
   *
   * @return 子任务管理器
   */
  @CheckResult
  public SubTaskManager getSubTaskManager() {
    if (mSubTaskManager == null) {
      mSubTaskManager = new SubTaskManager(getTargetName(), getTaskWrapper());
    }
    return mSubTaskManager;
  }

  @Override public DownloadGroupEntity getEntity() {
    return (DownloadGroupEntity) super.getEntity();
  }

  @Override public DGTaskWrapper getTaskWrapper() {
    return (DGTaskWrapper) super.getTaskWrapper();
  }

  /**
   * 设置任务组别名
   */
  @CheckResult
  public TARGET setGroupAlias(String alias) {
    if (TextUtils.isEmpty(alias)) return (TARGET) this;
    getEntity().setAlias(alias);
    return (TARGET) this;
  }
}
