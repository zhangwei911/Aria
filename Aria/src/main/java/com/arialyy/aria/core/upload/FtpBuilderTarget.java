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

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import com.arialyy.aria.core.common.AbsBuilderTarget;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.common.ftp.IFtpUploadInterceptor;
import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp单任务上传
 */
public class FtpBuilderTarget extends AbsBuilderTarget<FtpBuilderTarget> {
  private UNormalConfigHandler<FtpBuilderTarget> mConfigHandler;

  FtpBuilderTarget(String filePath, String targetName) {
    mConfigHandler = new UNormalConfigHandler<>(this, -1, targetName);
    mConfigHandler.setFilePath(filePath);
    getTaskWrapper().setRequestType(AbsTaskWrapper.U_FTP);
  }

  /**
   * 设置上传路径
   *
   * @param tempUrl 上传路径
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public FtpBuilderTarget setUploadUrl(String tempUrl) {
    mConfigHandler.setTempUrl(tempUrl);
    return this;
  }

  /**
   * FTP文件上传拦截器，如果远端已有同名文件，可使用该拦截器控制覆盖文件或修改该文件上传到服务器端端的文件名
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public FtpBuilderTarget setUploadInterceptor(@NonNull IFtpUploadInterceptor uploadInterceptor) {
    return mConfigHandler.setUploadInterceptor(uploadInterceptor);
  }

  /**
   * 设置登陆、字符串编码、ftps等参数
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public FtpDelegate<FtpBuilderTarget> option() {
    return new FtpDelegate<>(this, getTaskWrapper());
  }
}
