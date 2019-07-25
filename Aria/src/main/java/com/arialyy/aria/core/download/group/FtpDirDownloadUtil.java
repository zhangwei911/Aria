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

import android.net.Uri;
import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.exception.BaseException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp文件夹下载工具
 */
public class FtpDirDownloadUtil extends AbsGroupUtil {
  private String TAG = "FtpDirDownloadUtil";
  private ReentrantLock LOCK = new ReentrantLock();
  private Condition condition = LOCK.newCondition();

  public FtpDirDownloadUtil(IDGroupListener listener, DGTaskWrapper taskEntity) {
    super(listener, taskEntity);
  }

  @Override int getTaskType() {
    return FTP_DIR;
  }

  @Override protected void onPreStart() {
    super.onPreStart();
    if (mGTWrapper.getEntity().getFileSize() > 1) {
      startDownload(true);
    } else {
      FtpDirInfoThread infoThread = new FtpDirInfoThread(mGTWrapper, new OnFileInfoCallback() {
        @Override public void onComplete(String url, CompleteInfo info) {
          if (info.code >= 200 && info.code < 300) {
            startDownload(false);
          }
        }

        @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
          mListener.onFail(needRetry, e);
        }
      });
      new Thread(infoThread).start();
      try {
        LOCK.lock();
        condition.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        LOCK.unlock();
      }
    }
  }

  /**
   * @param needCloneInfo 第一次下载，信息已经在{@link FtpDirInfoThread}中clone了
   */
  private void startDownload(boolean needCloneInfo) {
    try {
      LOCK.lock();
      condition.signalAll();
    } finally {
      LOCK.unlock();
    }
    initState();
    for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
      if (needCloneInfo) {
        cloneInfo(wrapper);
      }
      if (wrapper.getState() != IEntity.STATE_COMPLETE) {
        createAndStartSubLoader(wrapper);
      }
    }
  }

  private void cloneInfo(DTaskWrapper subWrapper) {
    FtpUrlEntity urlEntity = mGTWrapper.asFtp().getUrlEntity().clone();
    Uri uri = Uri.parse(subWrapper.getEntity().getUrl());
    String remotePath = uri.getPath();
    urlEntity.remotePath = TextUtils.isEmpty(remotePath) ? "/" : remotePath;

    subWrapper.asFtp().setUrlEntity(urlEntity);
    subWrapper.asFtp().setCharSet(mGTWrapper.asFtp().getCharSet());
    subWrapper.asFtp().setProxy(mGTWrapper.asFtp().getProxy());
  }
}
