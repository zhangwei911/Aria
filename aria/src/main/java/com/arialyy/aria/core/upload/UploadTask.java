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

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.inf.AbsNormalTask;
import com.arialyy.aria.core.inf.IUploadListener;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.core.upload.uploader.SimpleUploadUtil;

/**
 * Created by lyy on 2017/2/23. 上传任务
 */
public class UploadTask extends AbsNormalTask<UploadEntity, UTaskWrapper> {

  private UploadTask(UTaskWrapper taskEntity, Handler outHandler) {
    mTaskWrapper = taskEntity;
    mOutHandler = outHandler;
    mListener = new BaseUListener(this, mOutHandler);
    mUtil = new SimpleUploadUtil(taskEntity, (IUploadListener) mListener);
  }

  @Override public int getTaskType() {
    return UPLOAD;
  }

  @Override public String getKey() {
    return mTaskWrapper.getEntity().getFilePath();
  }

  public UploadEntity getEntity() {
    return mTaskWrapper.getEntity();
  }

  @Override public String getTaskName() {
    return mTaskWrapper.getEntity().getFileName();
  }

  public static class Builder {
    private Handler mOutHandler;
    private UTaskWrapper mTaskEntity;

    public void setOutHandler(ISchedulers outHandler) {
      mOutHandler = new Handler(Looper.getMainLooper(), outHandler);
    }

    public void setUploadTaskEntity(UTaskWrapper taskEntity) {
      mTaskEntity = taskEntity;
    }

    public Builder() {

    }

    public UploadTask build() {
      return new UploadTask(mTaskEntity, mOutHandler);
    }
  }
}
