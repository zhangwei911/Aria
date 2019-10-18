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
import com.arialyy.aria.core.common.AbsBuilderTarget;
import com.arialyy.aria.core.common.FtpOption;
import com.arialyy.aria.core.inf.Suggest;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.CommonUtil;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp单任务上传
 */
public class FtpBuilderTarget extends AbsBuilderTarget<FtpBuilderTarget> {
  private UNormalConfigHandler<FtpBuilderTarget> mConfigHandler;
  private String url;

  FtpBuilderTarget(String filePath) {
    mConfigHandler = new UNormalConfigHandler<>(this, -1);
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
    url = tempUrl;
    mConfigHandler.setTempUrl(tempUrl);
    return this;
  }

  /**
   * 设置登陆、字符串编码、ftps等参数
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public FtpBuilderTarget option(FtpOption option) {
    if (option == null) {
      throw new NullPointerException("ftp 任务配置为空");
    }
    option.setUrlEntity(CommonUtil.getFtpUrlInfo(url));
    getTaskWrapper().getOptionParams().setParams(option);
    return this;
  }
}
