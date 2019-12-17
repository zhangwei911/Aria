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
package com.arialyy.aria.m3u8.live;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.IThreadState;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.processor.ITsMergeHandler;
import com.arialyy.aria.core.task.ThreadTask;
import com.arialyy.aria.m3u8.BaseM3U8Loader;
import com.arialyy.aria.m3u8.IdGenerator;
import com.arialyy.aria.m3u8.M3U8Listener;
import com.arialyy.aria.m3u8.M3U8TaskOption;
import com.arialyy.aria.m3u8.M3U8ThreadTaskAdapter;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.FileUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static com.arialyy.aria.m3u8.M3U8InfoThread.M3U8_INDEX_FORMAT;

/**
 * M3U8点播文件下载器
 */
public class M3U8LiveLoader extends BaseM3U8Loader {
  /**
   * 最大执行数
   */
  private static int EXEC_MAX_NUM = 4;
  private Handler mStateHandler;
  private ArrayBlockingQueue<Long> mFlagQueue = new ArrayBlockingQueue<>(EXEC_MAX_NUM);
  private ReentrantLock LOCK = new ReentrantLock();
  private Condition mCondition = LOCK.newCondition();
  private LinkedBlockingQueue<ExtInfo> mPeerQueue = new LinkedBlockingQueue<>();
  private ExtInfo mCurExtInfo;
  private FileOutputStream mIndexFos;

  M3U8LiveLoader(M3U8Listener listener, DTaskWrapper wrapper) {
    super(listener, wrapper);
    if (((M3U8TaskOption) wrapper.getM3u8Option()).isGenerateIndexFile()) {
      ALog.i(TAG, "直播文件下载，创建索引文件的操作将导致只能同时下载一个切片");
      EXEC_MAX_NUM = 1;
    }
  }

  @Override protected IThreadState createStateManager(Looper looper) {
    LiveStateManager manager = new LiveStateManager(looper, mListener);
    mStateHandler = new Handler(looper, manager);
    return manager;
  }

  void offerPeer(ExtInfo extInfo) {
    mPeerQueue.offer(extInfo);
  }

  @Override protected void handleTask() {

    new Thread(new Runnable() {
      @Override public void run() {
        String cacheDir = getCacheDir();
        int index = 0;
        while (!isBreak()) {
          try {
            LOCK.lock();
            while (mFlagQueue.size() < EXEC_MAX_NUM) {
              ExtInfo extInfo = mPeerQueue.poll();
              if (extInfo == null) {
                break;
              }
              mCurExtInfo = extInfo;
              ThreadTask task = createThreadTask(cacheDir, index, extInfo.url);
              getTaskList().put(index, task);
              mFlagQueue.offer(startThreadTask(task, task.getConfig().peerIndex));
              index++;
            }
            if (mFlagQueue.size() > 0) {
              mCondition.await();
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            LOCK.unlock();
          }
        }
      }
    }).start();
  }

  @Override public long getFileSize() {
    return mTempFile.length();
  }

  private void notifyLock(boolean success, int peerId) {
    try {
      LOCK.lock();
      long id = mFlagQueue.take();
      if (success) {
        ALog.d(TAG, String.format("切片【%s】下载成功", peerId));
      } else {
        ALog.e(TAG, String.format("切片【%s】下载失败", peerId));
      }
      mCondition.signalAll();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      LOCK.unlock();
    }
  }

  @Override protected void onPostStop() {
    super.onPostStop();
    if (mIndexFos != null) {
      try {
        mIndexFos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 启动线程任务
   *
   * @return 线程唯一id标志
   */
  private long startThreadTask(ThreadTask task, int indexId) {
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    ((M3U8Listener) mListener).onPeerStart(mTaskWrapper.getKey(),
        task.getConfig().tempFile.getPath(),
        indexId);
    return IdGenerator.getInstance().nextId();
  }

  /**
   * 配置config
   */
  private ThreadTask createThreadTask(String cacheDir, int indexId, String tsUrl) {
    ThreadRecord record = new ThreadRecord();
    record.taskKey = mRecord.filePath;
    record.isComplete = false;
    record.tsUrl = tsUrl;
    record.threadType = getEntity().getTaskType();
    record.threadId = indexId;
    mRecord.threadRecords.add(record);

    SubThreadConfig config = new SubThreadConfig();
    config.url = tsUrl;
    config.tempFile = new File(getTsFilePath(cacheDir, indexId));
    config.isBlock = mRecord.isBlock;
    config.taskWrapper = mTaskWrapper;
    config.record = record;
    config.stateHandler = mStateHandler;
    config.peerIndex = indexId;

    if (!config.tempFile.exists()) {
      FileUtil.createFile(config.tempFile);
    }
    ThreadTask threadTask = new ThreadTask(config);
    M3U8ThreadTaskAdapter adapter = new M3U8ThreadTaskAdapter(config);
    threadTask.setAdapter(adapter);
    return threadTask;
  }

  /**
   * 合并文件
   *
   * @return {@code true} 合并成功，{@code false}合并失败
   */
  boolean mergeFile() {
    ITsMergeHandler mergeHandler = mM3U8Option.getMergeHandler();
    String cacheDir = getCacheDir();
    List<String> partPath = new ArrayList<>();
    String[] tsNames = new File(cacheDir).list(new FilenameFilter() {
      @Override public boolean accept(File dir, String name) {
        return name.endsWith(".ts");
      }
    });
    for (String tsName : tsNames) {
      partPath.add(cacheDir + "/" + tsName);
    }

    boolean isSuccess;
    if (mergeHandler != null) {
      isSuccess = mergeHandler.merge(getEntity().getM3U8Entity(), partPath);
    } else {
      isSuccess = FileUtil.mergeFile(getEntity().getFilePath(), partPath);
    }
    if (isSuccess) {
      // 合并成功，删除缓存文件
      for (String pp : partPath) {
        File f = new File(pp);
        if (f.exists()) {
          f.delete();
        }
      }
      File cDir = new File(cacheDir);
      if (cDir.exists()) {
        cDir.delete();
      }
      return true;
    } else {
      ALog.e(TAG, "合并失败");
      return false;
    }
  }

  /**
   * M3U8线程状态管理，直播不处理停止状态、删除、失败的状态
   */
  private class LiveStateManager implements IThreadState {
    private final String TAG = "M3U8ThreadStateManager";

    /**
     * 任务状态回调
     */
    private M3U8Listener mListener;
    private long mProgress; //当前总进度
    private Looper mLooper;

    /**
     * @param listener 任务事件
     */
    LiveStateManager(Looper looper, IEventListener listener) {
      mLooper = looper;
      mListener = (M3U8Listener) listener;
    }

    /**
     * 退出looper循环
     */
    private void quitLooper() {
      ALog.d(TAG, "quitLooper");
      mLooper.quit();
    }

    @Override public boolean handleMessage(Message msg) {
      int peerIndex = msg.getData().getInt(ISchedulers.DATA_M3U8_PEER_INDEX);
      switch (msg.what) {
        case STATE_STOP:
          if (isBreak()) {
            ALog.d(TAG, "任务停止");
            quitLooper();
          }
          break;
        case STATE_CANCEL:
          if (isBreak()) {
            ALog.d(TAG, "任务取消");
            quitLooper();
          }
          break;
        case STATE_COMPLETE:
          notifyLock(true, peerIndex);
          if (mM3U8Option.isGenerateIndexFile() && !isBreak()) {
            addExtInf(mCurExtInfo.url, mCurExtInfo.extInf);
          }
          mListener.onPeerComplete(mTaskWrapper.getKey(),
              msg.getData().getString(ISchedulers.DATA_M3U8_PEER_PATH), peerIndex);
          break;
        case STATE_RUNNING:
          mProgress += (long) msg.obj;
          break;
        case STATE_FAIL:
          notifyLock(false, peerIndex);
          mListener.onPeerFail(mTaskWrapper.getKey(),
              msg.getData().getString(ISchedulers.DATA_M3U8_PEER_PATH), peerIndex);
          break;
      }
      return false;
    }

    /**
     * 给索引文件添加extInfo信息
     */
    private void addExtInf(String url, String extInf) {
      File indexFile =
          new File(String.format(M3U8_INDEX_FORMAT, getEntity().getFilePath()));
      if (!indexFile.exists()) {
        ALog.e(TAG, String.format("索引文件【%s】不存在，添加peer的extInf失败", indexFile.getPath()));
        return;
      }
      try {
        if (mIndexFos == null) {
          mIndexFos = new FileOutputStream(indexFile, true);
        }
        mIndexFos.write(extInf.concat("\r\n").getBytes(Charset.forName("UTF-8")));
        mIndexFos.write(url.concat("\r\n").getBytes(Charset.forName("UTF-8")));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override public boolean isFail() {
      return false;
    }

    @Override public boolean isComplete() {
      return false;
    }

    @Override public long getCurrentProgress() {
      return mProgress;
    }
  }

  static class ExtInfo {
    String url;
    String extInf;

    ExtInfo(String url, String extInf) {
      this.url = url;
      this.extInf = extInf;
    }
  }
}
