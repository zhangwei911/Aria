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
package com.arialyy.aria.core.upload.uploader;

import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.AbsFileer;
import com.arialyy.aria.core.common.AbsThreadTask;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IUploadListener;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import java.io.File;

/**
 * Created by Aria.Lao on 2017/7/27.
 * 文件上传器
 */
class Uploader extends AbsFileer<UploadEntity, UTaskWrapper> {

  Uploader(IUploadListener listener, UTaskWrapper taskEntity) {
    super(listener, taskEntity);
    mTempFile = new File(mEntity.getFilePath());
    setUpdateInterval(
        AriaManager.getInstance(AriaManager.APP).getUploadConfig().getUpdateInterval());
  }

  @Override protected boolean handleNewTask() {
    return true;
  }

  @Override protected AbsThreadTask selectThreadTask(SubThreadConfig<UTaskWrapper> config) {
    switch (mTaskWrapper.getRequestType()) {
      case AbsTaskWrapper.U_FTP:
        return new FtpThreadTask(config);
      case AbsTaskWrapper.U_HTTP:
        return new HttpThreadTask(config);
    }
    return null;
  }
}
