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
package com.arialyy.aria.sftp;

import com.arialyy.aria.core.AriaConfig;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.ftp.FtpTaskOption;
import com.arialyy.aria.util.CommonUtil;

/**
 *
 * https://cloud.tencent.com/developer/article/1354612
 * @author lyy
 */
public class AbsSFtpInfoThread<ENTITY extends AbsEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Runnable {

  private final String TAG = CommonUtil.getClassName(getClass());
  protected ENTITY mEntity;
  protected TASK_WRAPPER mTaskWrapper;
  protected FtpTaskOption mTaskOption;
  private int mConnectTimeOut;
  protected OnFileInfoCallback mCallback;
  protected long mSize = 0;
  protected String charSet = "UTF-8";
  private boolean isUpload = false;

  public AbsSFtpInfoThread(TASK_WRAPPER taskWrapper, OnFileInfoCallback callback) {
    mTaskWrapper = taskWrapper;
    mEntity = taskWrapper.getEntity();
    mTaskOption = (FtpTaskOption) taskWrapper.getTaskOption();
    mConnectTimeOut = AriaConfig.getInstance().getDConfig().getConnectTimeOut();
    mCallback = callback;
    if (mEntity instanceof UploadEntity) {
      isUpload = true;
    }
  }

  @Override public void run() {

  }
}
