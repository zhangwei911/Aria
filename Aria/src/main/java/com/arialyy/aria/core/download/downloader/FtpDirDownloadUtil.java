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
package com.arialyy.aria.core.download.downloader;

import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DownloadGroupTaskEntity;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ErrorHelp;
import java.util.Set;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp文件夹下载工具
 */
public class FtpDirDownloadUtil extends AbsGroupUtil {
  private String TAG = "FtpDirDownloadUtil";

  public FtpDirDownloadUtil(IDownloadGroupListener listener, DownloadGroupTaskEntity taskEntity) {
    super(listener, taskEntity);
  }

  @Override int getTaskType() {
    return FTP_DIR;
  }

  @Override protected void onStart() {
    super.onStart();
    if (mGTEntity.getEntity().getFileSize() > 1) {
      onPre();
      startDownload();
    } else {
      new FtpDirInfoThread(mGTEntity, new OnFileInfoCallback() {
        @Override public void onComplete(String url, CompleteInfo info) {
          if (info.code >= 200 && info.code < 300) {
            onPre();
            startDownload();
          }
        }

        @Override public void onFail(String url, BaseException e, boolean needRetry) {
          DownloadTaskEntity te = mExeMap.get(url);
          if (te != null) {
            mFailMap.put(url, te);
            mExeMap.remove(url);
          }
          mListener.onFail(needRetry, e);
        }
      }).start();
    }
  }

  private void startDownload() {
    if (mCompleteNum == mGroupSize) {
      mListener.onComplete();
      return;
    }
    int i = 0;
    Set<String> keys = mExeMap.keySet();
    for (String key : keys) {
      DownloadTaskEntity taskEntity = mExeMap.get(key);
      if (taskEntity != null) {
        createChildDownload(taskEntity);
        i++;
      }
    }
    if (mExeMap.size() == 0) {
      mListener.onComplete();
    } else if (i == mExeMap.size()) {
      startRunningFlow();
    }
  }
}
