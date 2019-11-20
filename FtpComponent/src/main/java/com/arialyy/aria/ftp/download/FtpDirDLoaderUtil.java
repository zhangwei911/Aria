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
package com.arialyy.aria.ftp.download;

import android.net.Uri;
import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.group.AbsGroupUtil;
import com.arialyy.aria.core.group.AbsSubDLoadUtil;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.ftp.FtpDirInfoThread;
import com.arialyy.aria.ftp.FtpTaskOption;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp文件夹下载工具
 */
public class FtpDirDLoaderUtil extends AbsGroupUtil {
  private ReentrantLock LOCK = new ReentrantLock();
  private Condition condition = LOCK.newCondition();

  public FtpDirDLoaderUtil(AbsTaskWrapper wrapper, IEventListener listener) {
    super(wrapper, listener);
    wrapper.generateTaskOption(FtpTaskOption.class);
  }

  @Override
  protected AbsSubDLoadUtil createSubLoader(DTaskWrapper wrapper, boolean needGetFileInfo) {
    return new FtpSubDLoaderUtil(getScheduler(), wrapper, needGetFileInfo);
  }

  @Override protected boolean onStart() {
    super.onStart();

    if (getWrapper().getEntity().getFileSize() > 1) {
      startDownload(true);
    } else {
      FtpDirInfoThread infoThread = new FtpDirInfoThread(getWrapper(), new OnFileInfoCallback() {
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
    return true;
  }

  /**
   * @param needCloneInfo 第一次下载，信息已经在{@link FtpDirInfoThread}中clone了
   */
  private void startDownload(boolean needCloneInfo) {
    // ftp需要获取完成只任务信息才更新只任务数量
    getState().setSubSize(getWrapper().getSubTaskWrapper().size());
    try {
      LOCK.lock();
      condition.signalAll();
    } finally {
      LOCK.unlock();
    }
    for (DTaskWrapper wrapper : getWrapper().getSubTaskWrapper()) {
      if (needCloneInfo) {
        cloneInfo(wrapper);
      }
      if (wrapper.getState() != IEntity.STATE_COMPLETE) {
        startSubLoader(createSubLoader(wrapper, true));
      }
    }
  }

  private void cloneInfo(DTaskWrapper subWrapper) {
    FtpTaskOption option = (FtpTaskOption) getWrapper().getTaskOption();
    FtpUrlEntity urlEntity = option.getUrlEntity().clone();
    Uri uri = Uri.parse(subWrapper.getEntity().getUrl());
    String remotePath = uri.getPath();
    urlEntity.remotePath = TextUtils.isEmpty(remotePath) ? "/" : remotePath;

    FtpTaskOption subOption = new FtpTaskOption();
    subOption.setUrlEntity(urlEntity);
    subOption.setCharSet(option.getCharSet());
    subOption.setProxy(option.getProxy());
    subOption.setClientConfig(option.getClientConfig());
    subOption.setNewFileName(option.getNewFileName());
    subOption.setProxy(option.getProxy());
    subOption.setUploadInterceptor(option.getUploadInterceptor());

    subWrapper.setTaskOption(subOption);
  }
}
