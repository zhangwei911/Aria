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

import com.arialyy.aria.core.common.ftp.IFtpUploadInterceptor;
import com.arialyy.aria.core.event.ErrorEvent;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.IConfigHandler;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * Created by Aria.Lao on 2019/4/5.
 * 普通上传任务通用功能处理
 */
class UNormalConfigHandler<TARGET extends AbsTarget> implements IConfigHandler {
  private String TAG = "UNormalDelegate";
  private UploadEntity mEntity;
  private TARGET mTarget;
  private UTaskWrapper mWrapper;

  UNormalConfigHandler(TARGET target, long taskId, String targetName) {
    mTarget = target;
    initTarget(taskId, targetName);
  }

  private void initTarget(long taskId, String targetName) {
    mWrapper = TaskWrapperManager.getInstance().getNormalTaskWrapper(UTaskWrapper.class, taskId);
    if (taskId != -1 && mWrapper.getEntity().getId() == -1) {
      mWrapper.setErrorEvent(new ErrorEvent(taskId, String.format("没有id为%s的任务", taskId)));
    }
    mEntity = mWrapper.getEntity();
    mTarget.setTargetName(targetName);
    mTarget.setTaskWrapper(mWrapper);
    getTaskWrapper().setTempUrl(mEntity.getUrl());
  }

  void setFilePath(String filePath) {
    File file = new File(filePath);
    mEntity.setFilePath(filePath);
    mEntity.setFileName(file.getName());
    mEntity.setFileSize(file.length());
  }

  TARGET setUploadInterceptor(IFtpUploadInterceptor uploadInterceptor) {
    if (uploadInterceptor == null) {
      throw new NullPointerException("ftp拦截器为空");
    }
    getTaskWrapper().asFtp().setUploadInterceptor(uploadInterceptor);
    return mTarget;
  }

  @Override public AbsEntity getEntity() {
    return mEntity;
  }

  @Override public boolean taskExists() {
    return DbEntity.checkDataExist(UploadEntity.class, "key=?", mEntity.getFilePath());
  }

  @Override public boolean isRunning() {
    UploadTask task = UploadTaskQueue.getInstance().getTask(mEntity.getKey());
    return task != null && task.isRunning();
  }

  void setTempUrl(String tempUrl) {
    getTaskWrapper().setTempUrl(tempUrl);
    getTaskWrapper().asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(tempUrl));
  }

  private UTaskWrapper getTaskWrapper() {
    return mWrapper;
  }
}
