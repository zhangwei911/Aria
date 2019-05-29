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
import com.arialyy.aria.core.common.http.HttpTaskConfig;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.downloader.HttpFileInfoThread;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
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
  private boolean getLenComplete = false;

  public DownloadGroupUtil(IDownloadGroupListener listener, DGTaskWrapper taskWrapper) {
    super(listener, taskWrapper);
  }

  @Override int getTaskType() {
    return HTTP_GROUP;
  }

  @Override public void onPreCancel() {
    super.onPreCancel();
  }

  @Override protected boolean onPreStop() {
    // 如果是isUnknownSize()标志，并且获取大小没有完成，则直接回调onStop
    if (mPool != null && !getLenComplete) {
      ALog.d(TAG, "获取长度未完成的情况下，停止组合任务");
      mPool.shutdown();
      mListener.onStop(0);
      return true;
    }
    return false;
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
            createAndStartSubLoader(wrapper);
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
      int failCount;

      @Override public void run() {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        for (DTaskWrapper dTaskWrapper : mGTWrapper.getSubTaskWrapper()) {
          cloneHeader(dTaskWrapper);
          mPool.submit(new HttpFileInfoThread(dTaskWrapper, new OnFileInfoCallback() {
            @Override public void onComplete(String url, CompleteInfo info) {
              createAndStartSubLoader((DTaskWrapper) info.wrapper, false);
              count++;
              checkGetSizeComplete(count, failCount);
            }

            @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
              ALog.e(TAG, String.format("获取文件信息失败，url：%s", ((DownloadEntity) entity).getUrl()));
              count++;
              failCount++;
              mListener.onSubFail((DownloadEntity) entity, new AriaIOException(TAG,
                  String.format("子任务获取文件长度失败，url：%s", ((DownloadEntity) entity).getUrl())));
              checkGetSizeComplete(count, failCount);
            }
          }));
        }
      }
    }).start();
  }

  /**
   * 检查组合任务大小是否获取完成，获取完成后取消阻塞，并设置组合任务大小
   */
  private void checkGetSizeComplete(int count, int failCount) {
    if (count == mGTWrapper.getSubTaskWrapper().size()) {
      long size = 0;
      for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
        size += wrapper.getEntity().getFileSize();
      }
      mGTWrapper.getEntity().setConvertFileSize(CommonUtil.formatFileSize(size));
      mGTWrapper.getEntity().setFileSize(size);
      mGTWrapper.getEntity().update();
      getLenComplete = true;
      ALog.d(TAG, String.format("获取组合任务长度完成，len：%s", size));
    } else if (failCount == mGTWrapper.getSubTaskWrapper().size()) {
      mListener.onFail(true, new AriaIOException(TAG, "获取子任务长度失败"));
    }

    mGTWrapper.asHttp().setFileLenAdapter(null);
    synchronized (LOCK) {
      LOCK.notifyAll();
    }
  }

  /**
   * 子任务使用父包裹器的属性
   */
  private void cloneHeader(DTaskWrapper taskWrapper) {
    HttpTaskConfig groupDelegate = mGTWrapper.asHttp();
    HttpTaskConfig subDelegate = taskWrapper.asHttp();

    // 设置属性
    subDelegate.setFileLenAdapter(groupDelegate.getFileLenAdapter());
    subDelegate.setRequestEnum(groupDelegate.getRequestEnum());
    subDelegate.setHeaders(groupDelegate.getHeaders());
    subDelegate.setProxy(groupDelegate.getProxy());
    subDelegate.setParams(groupDelegate.getParams());
  }
}