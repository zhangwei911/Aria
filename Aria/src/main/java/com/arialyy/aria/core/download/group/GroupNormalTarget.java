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
package com.arialyy.aria.core.download.group;

import androidx.annotation.CheckResult;
import com.arialyy.aria.core.common.AbsNormalTarget;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.common.http.HttpDelegate;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.core.manager.SubTaskManager;
import java.util.List;

/**
 * Created by AriaL on 2017/6/29.
 * 下载任务组
 */
public class GroupNormalTarget extends AbsNormalTarget<GroupNormalTarget> {
  private HttpGroupConfigHandler<GroupNormalTarget> mConfigHandler;

  GroupNormalTarget(long taskId, String targetName) {
    setTargetName(targetName);
    mConfigHandler = new HttpGroupConfigHandler<>(this, taskId);
    getTaskWrapper().setRequestType(ITaskWrapper.DG_HTTP);
  }

  /**
   * 设置http请求参数，header等信息
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public HttpDelegate<GroupNormalTarget> option() {
    return new HttpDelegate<>(this, getTaskWrapper());
  }

  /**
   * 获取子任务管理器
   *
   * @return 子任务管理器
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public SubTaskManager getSubTaskManager() {
    return mConfigHandler.getSubTaskManager();
  }

  /**
   * 设置任务组别名
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public GroupNormalTarget setGroupAlias(String alias) {
    mConfigHandler.setGroupAlias(alias);
    return this;
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public GroupNormalTarget updateUrls(List<String> urls) {
    return mConfigHandler.updateUrls(urls);
  }

  /**
   * 更新任务组的文件夹路径，在Aria中，任务组的所有子任务都会下载到以任务组组名的文件夹中。
   * 如：groupDirPath = "/mnt/sdcard/download/group_test"
   * <pre>
   *   {@code
   *      + mnt
   *        + sdcard
   *          + download
   *            + group_test
   *              - task1.apk
   *              - task2.apk
   *              - task3.apk
   *              ....
   *
   *   }
   * </pre>
   *
   * @param dirPath 任务组保存文件夹路径
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public GroupNormalTarget modifyDirPath(String dirPath) {
    return mConfigHandler.setDirPath(dirPath);
  }

  /**
   * 更新子任务文件名，该方法必须在{@link #modifyDirPath(String)}之后调用，否则不生效
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public GroupNormalTarget modifySubFileName(List<String> subTaskFileName) {
    return mConfigHandler.setSubFileName(subTaskFileName);
  }

  @Override public boolean isRunning() {
    return mConfigHandler.isRunning();
  }

  @Override public boolean taskExists() {
    return mConfigHandler.taskExists();
  }
}
