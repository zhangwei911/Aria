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

import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IUploadListener;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.CheckUtil;

/**
 * Created by lyy on 2017/2/9.
 * 简单的文件上传工具
 */
public class SimpleUploadUtil implements IUtil, Runnable {
  private static final String TAG = "SimpleUploadUtil";

  private UploadEntity mUploadEntity;
  private UTaskWrapper mTaskWrapper;
  private IUploadListener mListener;
  private Uploader mUploader;
  private boolean isStop = false, isCancel = false;

  public SimpleUploadUtil(UTaskWrapper taskWrapper, IUploadListener listener) {
    mTaskWrapper = taskWrapper;
    CheckUtil.checkTaskEntity(taskWrapper);
    mUploadEntity = taskWrapper.getEntity();
    if (listener == null) {
      throw new IllegalArgumentException("上传监听不能为空");
    }
    mListener = listener;
    mUploader = new Uploader(mListener, taskWrapper);
  }

  @Override public void run() {
    mListener.onPre();
    switch (mTaskWrapper.getRequestType()) {
      case AbsTaskWrapper.U_FTP:
        FtpFileInfoThread infoThread =
            new FtpFileInfoThread(mTaskWrapper, new OnFileInfoCallback() {
              @Override public void onComplete(String url, CompleteInfo info) {
                if (info.code == FtpFileInfoThread.CODE_COMPLETE) {
                  mListener.onComplete();
                } else {
                  mUploader.start();
                }
              }

              @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
                failUpload(e, needRetry);
              }
            });
        new Thread(infoThread).start();
        break;
      case AbsTaskWrapper.U_HTTP:
        mUploader.start();
        break;
    }
  }

  private void failUpload(BaseException e, boolean needRetry) {
    if (isStop || isCancel) {
      return;
    }
    mListener.onFail(needRetry, e);
    mUploader.onDestroy();
  }

  @Override public String getKey() {
    return mTaskWrapper.getKey();
  }

  @Override public long getFileSize() {
    return mUploader.getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mUploader.getCurrentLocation();
  }

  @Override public boolean isRunning() {
    return mUploader.isRunning();
  }

  @Override public void cancel() {
    isCancel = true;
    mUploader.cancel();
  }

  @Override public void stop() {
    isStop = true;
    mUploader.stop();
  }

  @Override public void start() {
    if (isStop || isCancel) {
      return;
    }
    new Thread(this).start();
  }
}
