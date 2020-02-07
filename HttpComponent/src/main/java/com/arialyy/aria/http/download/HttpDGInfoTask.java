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
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.listener.DownloadGroupListener;
import com.arialyy.aria.core.loader.IInfoTask;
import com.arialyy.aria.core.loader.ILoaderVisitor;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.http.HttpTaskOption;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 组合任务文件信息，用于获取长度未知时，组合任务的长度
 */
public final class HttpDGInfoTask implements IInfoTask {
    private String TAG = CommonUtil.getClassName(this);
    private Callback callback;
    private DGTaskWrapper wrapper;
    private final Object LOCK = new Object();
    private ExecutorService mPool = null;
    private boolean getLenComplete = false;
    private int count;
    private int failCount;
    private DownloadGroupListener listener;

    /**
     * 子任务回调
     */
    private Callback subCallback = new Callback() {
        @Override
        public void onSucceed(String url, CompleteInfo info) {
            count++;
            checkGetSizeComplete(count, failCount);
            ALog.d(TAG, "获取子任务信息完成");
        }

        @Override
        public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
            ALog.e(TAG, String.format("获取文件信息失败，url：%s", ((DownloadEntity) entity).getUrl()));
            count++;
            failCount++;
            listener.onSubFail((DownloadEntity) entity, new AriaIOException(TAG,
                    String.format("子任务获取文件长度失败，url：%s", ((DownloadEntity) entity).getUrl())));
            checkGetSizeComplete(count, failCount);
        }
    };

    HttpDGInfoTask(DGTaskWrapper wrapper, DownloadGroupListener listener) {
        this.wrapper = wrapper;
        this.listener = listener;
    }

    @Override
    public void run() {
        // 如果是isUnknownSize()标志，并且获取大小没有完成，则直接回调onStop
        if (mPool != null && !getLenComplete) {
            ALog.d(TAG, "获取长度未完成的情况下，停止组合任务");
            mPool.shutdown();
            listener.onStop(0);
            return;
        }
        // 处理组合任务大小未知的情况
        if (wrapper.isUnknownSize() && wrapper.getEntity().getFileSize() < 1) {
            mPool = Executors.newCachedThreadPool();
            getGroupSize();
            try {
                synchronized (LOCK) {
                    LOCK.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!mPool.isShutdown()) {
                mPool.shutdown();
            }
        } else {
            for (DTaskWrapper wrapper : wrapper.getSubTaskWrapper()) {
                cloneHeader(wrapper);
            }
            callback.onSucceed(wrapper.getKey(), new CompleteInfo());
        }
    }

    /*
     * 获取组合任务大小，使用该方式获取到的组合任务大小，子任务不需要再重新获取文件大小
     */
    private void getGroupSize() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (DTaskWrapper dTaskWrapper : wrapper.getSubTaskWrapper()) {
                    cloneHeader(dTaskWrapper);
                    HttpDFileInfoTask infoTask = new HttpDFileInfoTask(dTaskWrapper);
                    infoTask.setCallback(subCallback);
                    mPool.execute(infoTask);
                }
            }
        }).start();
    }

    /**
     * 检查组合任务大小是否获取完成，获取完成后取消阻塞，并设置组合任务大小
     */
    private void checkGetSizeComplete(int count, int failCount) {
        if (failCount == wrapper.getSubTaskWrapper().size()) {
            callback.onFail(wrapper.getEntity(), new AriaIOException(TAG, "获取子任务长度失败"), false);
            notifyLock();
            return;
        }
        if (count == wrapper.getSubTaskWrapper().size()) {
            long size = 0;
            for (DTaskWrapper wrapper : wrapper.getSubTaskWrapper()) {
                size += wrapper.getEntity().getFileSize();
            }
            wrapper.getEntity().setConvertFileSize(CommonUtil.formatFileSize(size));
            wrapper.getEntity().setFileSize(size);
            wrapper.getEntity().update();
            getLenComplete = true;
            ALog.d(TAG, String.format("获取组合任务长度完成，组合任务总长度：%s，失败的子任务数：%s", size, failCount));
            callback.onSucceed(wrapper.getKey(), new CompleteInfo());
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
        HttpTaskOption groupOption = (HttpTaskOption) wrapper.getTaskOption();
        HttpTaskOption subOption = new HttpTaskOption();

        // 设置属性
        subOption.setFileLenAdapter(groupOption.getFileLenAdapter());
        subOption.setRequestEnum(groupOption.getRequestEnum());
        subOption.setHeaders(groupOption.getHeaders());
        subOption.setProxy(groupOption.getProxy());
        subOption.setParams(groupOption.getParams());
        taskWrapper.setTaskOption(subOption);
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void accept(ILoaderVisitor visitor) {
        visitor.addComponent(this);
    }
}
