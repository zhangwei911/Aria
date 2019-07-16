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

import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.exception.BaseException;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp文件夹下载工具
 */
public class FtpDirDownloadUtil extends AbsGroupUtil {
  private String TAG = "FtpDirDownloadUtil";

  public FtpDirDownloadUtil(IDGroupListener listener, DGTaskWrapper taskEntity) {
    super(listener, taskEntity);
  }

  @Override int getTaskType() {
    return FTP_DIR;
  }

  @Override protected void onStart() {
    super.onStart();
    if (mGTWrapper.getEntity().getFileSize() > 1) {
      startDownload();
    } else {

      FtpDirInfoThread infoThread = new FtpDirInfoThread(mGTWrapper, new OnFileInfoCallback() {
        @Override public void onComplete(String url, CompleteInfo info) {
          if (info.code >= 200 && info.code < 300) {
            startDownload();
          }
        }

        @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
          mListener.onFail(needRetry, e);
        }
      });
      new Thread(infoThread).start();
    }
  }

  private void startDownload() {
    for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
      if (wrapper.getState() != IEntity.STATE_COMPLETE) {
        createAndStartSubLoader(wrapper);
      }
    }
  }
}
