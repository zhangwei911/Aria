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
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.downloader.HttpFileInfoThread;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组下载工具
 */
public class DownloadGroupUtil extends AbsGroupUtil implements IUtil {
  private static final String TAG = "DownloadGroupUtil";
  private final Object LOCK = new Object();
  private ExecutorService mPool = null;

  public DownloadGroupUtil(IDownloadGroupListener listener, DGTaskWrapper taskWrapper) {
    super(listener, taskWrapper);
  }

  @Override int getTaskType() {
    return HTTP_GROUP;
  }

  @Override public void onCancel() {
    super.onCancel();
  }

  @Override protected void onStop() {
    super.onStop();
  }

  @Override protected void onStart() {
    super.onStart();
    if (mState.getCompleteNum() == mState.getSubSize()) {
      mListener.onComplete();
    } else {
      // 处理组合任务大小未知的情况
      if (mGTWrapper.isUnknownSize() && mGTWrapper.getEntity().getFileSize() < 1) {
        mPool = Executors.newCachedThreadPool();
        getGroupSize();
        try {
          synchronized (LOCK) {
            LOCK.wait();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
          if (wrapper.getState() != IEntity.STATE_COMPLETE) {
            createSubLoader(wrapper);
          }
        }
      }
    }
  }

  /**
   * 获取组合任务大小，使用该方式获取到的组合任务大小，子任务不需要再重新获取文件大小
   */
  private void getGroupSize() {
    new Thread(new Runnable() {
      int count;

      @Override public void run() {
        for (DTaskWrapper dTaskWrapper : mGTWrapper.getSubTaskWrapper()) {
          mPool.submit(new HttpFileInfoThread(dTaskWrapper, new OnFileInfoCallback() {
            @Override public void onComplete(String url, CompleteInfo info) {
              createSubLoader((DTaskWrapper) info.wrapper, false);
              count++;
              checkGetSizeComplete(count);
            }

            @Override public void onFail(String url, BaseException e, boolean needRetry) {
              ALog.e(TAG, String.format("获取文件信息失败，url：%s", url));
              count++;
              checkGetSizeComplete(count);
            }
          }));
        }
      }
    }).start();
  }

  /**
   * 检查组合任务大小是否获取完成，获取完成后取消阻塞，并设置组合任务大小
   */
  private void checkGetSizeComplete(int count) {
    if (count == mGTWrapper.getSubTaskWrapper().size()) {
      long size = 0;
      for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
        size += wrapper.getEntity().getFileSize();
      }
      mGTWrapper.getEntity().setFileSize(size);

      synchronized (LOCK) {
        LOCK.notify();
      }
    }
  }
}