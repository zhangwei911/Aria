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
package com.arialyy.aria.http.download;

import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.core.group.AbsGroupUtil;
import com.arialyy.aria.core.group.AbsSubDLoadUtil;
import com.arialyy.aria.http.HttpFileInfoThread;
import com.arialyy.aria.http.HttpTaskOption;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组下载工具
 */
public class DGroupLoaderUtil extends AbsGroupUtil {
  private final Object LOCK = new Object();
  private ExecutorService mPool = null;
  private boolean getLenComplete = false;
  private List<DTaskWrapper> mTempWrapper = new ArrayList<>();

  public DGroupLoaderUtil(AbsTaskWrapper taskWrapper, IEventListener listener) {
    super(taskWrapper, listener);
    taskWrapper.generateTaskOption(HttpTaskOption.class);
  }

  @Override
  protected AbsSubDLoadUtil createSubLoader(DTaskWrapper wrapper, boolean needGetFileInfo) {
    return new HttpSubDLoaderUtil(getScheduler(), wrapper, needGetFileInfo);
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

  @Override protected boolean onStart() {
    super.onStart();
    initState();
    if (getState().getCompleteNum() == getState().getSubSize()) {
      mListener.onComplete();
    } else {
      // 处理组合任务大小未知的情况
      if (getWrapper().isUnknownSize() && getWrapper().getEntity().getFileSize() < 1) {
        mPool = Executors.newCachedThreadPool();
        getGroupSize();
        try {
          synchronized (LOCK) {
            LOCK.wait();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return getLenComplete;
      } else {
        for (DTaskWrapper wrapper : getWrapper().getSubTaskWrapper()) {
          if (wrapper.getState() != IEntity.STATE_COMPLETE) {
            startSubLoader(createSubLoader(wrapper, true));
          }
        }
      }
    }
    return true;
  }

  /**
   * 获取组合任务大小，使用该方式获取到的组合任务大小，子任务不需要再重新获取文件大小
   */
  private void getGroupSize() {
    new Thread(new Runnable() {
      int count;
      int failCount;

      @Override public void run() {
        for (DTaskWrapper dTaskWrapper : getWrapper().getSubTaskWrapper()) {
          cloneHeader(dTaskWrapper);
          mPool.submit(new HttpFileInfoThread(dTaskWrapper, new OnFileInfoCallback() {
            @Override public void onComplete(String url, CompleteInfo info) {
              if (!getWrapper().isUnknownSize()) {
                startSubLoader(createSubLoader((DTaskWrapper) info.wrapper, false));
              } else {
                mTempWrapper.add((DTaskWrapper) info.wrapper);
              }
              count++;
              checkGetSizeComplete(count, failCount);
              ALog.d(TAG, "获取子任务信息完成");
            }

            @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
              ALog.e(TAG, String.format("获取文件信息失败，url：%s", ((DownloadEntity) entity).getUrl()));
              count++;
              failCount++;
              mListener.onSubFail((DownloadEntity) entity, new AriaIOException(TAG,
                  String.format("子任务获取文件长度失败，url：%s", ((DownloadEntity) entity).getUrl())));
              checkGetSizeComplete(count, failCount);
              getState().countFailNum(entity.getKey());
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
    if (failCount == getWrapper().getSubTaskWrapper().size()) {
      getState().setRunning(false);
      mListener.onFail(false, new AriaIOException(TAG, "获取子任务长度失败"));
      notifyLock();
      return;
    }
    if (count == getWrapper().getSubTaskWrapper().size()) {
      long size = 0;
      for (DTaskWrapper wrapper : getWrapper().getSubTaskWrapper()) {
        size += wrapper.getEntity().getFileSize();
      }
      getWrapper().getEntity().setConvertFileSize(CommonUtil.formatFileSize(size));
      getWrapper().getEntity().setFileSize(size);
      getWrapper().getEntity().update();
      getLenComplete = true;
      ALog.d(TAG, String.format("获取组合任务长度完成，组合任务总长度：%s，失败的只任务数：%s", size, failCount));
      // 未知大小的组合任务，延迟下载
      if (getWrapper().isUnknownSize()) {
        for (DTaskWrapper wrapper : mTempWrapper) {
          startSubLoader(createSubLoader(wrapper, false));
        }
      }
      notifyLock();
    }
  }

  private void notifyLock() {
    synchronized (LOCK) {
      LOCK.notifyAll();
    }
  }

  /**
   * 子任务使用父包裹器的属性
   */
  private void cloneHeader(DTaskWrapper taskWrapper) {
    HttpTaskOption groupOption = (HttpTaskOption) getWrapper().getTaskOption();
    HttpTaskOption subOption = (HttpTaskOption) taskWrapper.getTaskOption();

    // 设置属性
    subOption.setFileLenAdapter(groupOption.getFileLenAdapter());
    subOption.setRequestEnum(groupOption.getRequestEnum());
    subOption.setHeaders(groupOption.getHeaders());
    subOption.setProxy(groupOption.getProxy());
    subOption.setParams(groupOption.getParams());
  }
}