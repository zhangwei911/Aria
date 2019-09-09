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
package com.arialyy.aria.core.upload.target;

import androidx.annotation.CheckResult;
import com.arialyy.aria.core.common.AbsNormalTarget;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.util.CommonUtil;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp单任务上传
 */
public class FtpNormalTarget extends AbsNormalTarget<FtpNormalTarget> {
  private UNormalConfigHandler<FtpNormalTarget> mConfigHandler;

  FtpNormalTarget(long taskId) {
    mConfigHandler = new UNormalConfigHandler<>(this, taskId);
    getTaskWrapper().asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(getEntity().getUrl()));
    getTaskWrapper().setRequestType(AbsTaskWrapper.U_FTP);
  }

  /**
   * 设置登陆、字符串编码、ftps等参数
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public FtpDelegate<FtpNormalTarget> option() {
    return new FtpDelegate<>(this, getTaskWrapper());
  }

  @Override public UploadEntity getEntity() {
    return (UploadEntity) super.getEntity();
  }

  @Override public boolean isRunning() {
    return mConfigHandler.isRunning();
  }

  @Override public boolean taskExists() {
    return mConfigHandler.taskExists();
  }
}
