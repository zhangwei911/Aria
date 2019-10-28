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
package com.arialyy.aria.m3u8.vod;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;
import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.event.Event;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.event.PeerIndexEvent;
import com.arialyy.aria.core.inf.IThreadState;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.task.ThreadTask;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.m3u8.BaseM3U8Loader;
import com.arialyy.aria.core.processor.ITsMergeHandler;
import com.arialyy.aria.m3u8.M3U8Listener;
import com.arialyy.aria.m3u8.M3U8TaskOption;
import com.arialyy.aria.m3u8.M3U8ThreadTaskAdapter;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * M3U8点播文件下载器
 */
public class M3U8VodLoader extends BaseM3U8Loader {
  /**
   * 最大执行数
   */
  private int EXEC_MAX_NUM;
  private Handler mStateHandler;
  private ArrayBlockingQueue<TempFlag> mFlagQueue;
  private ArrayBlockingQueue<PeerIndexEvent> mJumpQueue;
  private ReentrantLock LOCK = new ReentrantLock();
  private ReentrantLock EVENT_LOCK = new ReentrantLock();
  private ReentrantLock JUMP_LOCK = new ReentrantLock();
  private Condition mWaitCondition = LOCK.newCondition();
  private Condition mEventQueueCondition = EVENT_LOCK.newCondition();
  private Condition mJumpCondition = JUMP_LOCK.newCondition();
  private SparseArray<ThreadRecord> mBeforePeer = new SparseArray<>();
  private SparseArray<ThreadRecord> mAfterPeer = new SparseArray<>();
  private VodStateManager mManager;
  private PeerIndexEvent mCurrentEvent;
  private String mCacheDir;
  private int aIndex = 0, bIndex = 0;
  private int mCurrentFlagSize;
  private boolean isJump = false, isDestroy = false;
  private int mCompleteNum = 0;
  private ExecutorService mJumpThreadPool;
  private Thread jumpThread = null;
  private M3U8TaskOption mM3U8Option;

  M3U8VodLoader(M3U8Listener listener, DTaskWrapper wrapper) {
    super(listener, wrapper);
    mM3U8Option = (M3U8TaskOption) wrapper.getM3u8Option();
    mFlagQueue = new ArrayBlockingQueue<>(mM3U8Option.getMaxTsQueueNum());
    EXEC_MAX_NUM = mM3U8Option.getMaxTsQueueNum();
    mJumpQueue = new ArrayBlockingQueue<>(10);
    EventMsgUtil.getDefault().register(this);
  }

  @Override protected IThreadState createStateManager(Looper looper) {
    mManager = new VodStateManager(looper, mRecord, mListener);
    mStateHandler = new Handler(looper, mManager);
    return mManager;
  }

  @Override public void onDestroy() {
    super.onDestroy();
    isDestroy = true;
    EventMsgUtil.getDefault().unRegister(this);
    if (mJumpThreadPool != null && !mJumpThreadPool.isShutdown()) {
      mJumpThreadPool.shutdown();
    }
  }

  @Override protected void onPostPre() {
    super.onPostPre();
    initData();
  }

  @Override public boolean isBreak() {
    return super.isBreak() || isDestroy;
  }

  @Override protected void handleTask() {
    Thread th = new Thread(new Runnable() {
      @Override public void run() {
        while (!isBreak()) {
          try {
            JUMP_LOCK.lock();
            if (isJump) {
              mJumpCondition.await(5, TimeUnit.SECONDS);
              isJump = false;
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            JUMP_LOCK.unlock();
          }

          try {
            LOCK.lock();
            while (mFlagQueue.size() < EXEC_MAX_NUM && !isBreak()) {
              if (mCompleteNum == mRecord.threadRecords.size()) {
                break;
              }

              ThreadRecord tr = getThreadRecord();
              if (tr == null || tr.isComplete) {
                ALog.d(TAG, "记录为空或记录已完成");
                break;
              }
              addTaskToQueue(tr);
            }
            if (mFlagQueue.size() > 0) {
              mWaitCondition.await();
            }
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            LOCK.unlock();
          }
        }
      }
    });
    th.start();
  }

  @Override public long getFileSize() {
    return getEntity().getFileSize();
  }

  /**
   * 获取线程记录
   */
  private ThreadRecord getThreadRecord() {
    ThreadRecord tr = null;
    try {
      // 优先下载peer指针之后的数据
      if (bIndex == 0 && aIndex < mAfterPeer.size()) {
        //ALog.d(TAG, String.format("afterArray size:%s, index:%s", mAfterPeer.size(), aIndex));
        tr = mAfterPeer.valueAt(aIndex);
        aIndex++;
      }

      // 如果指针之后的数组没有切片了，则重新初始化指针位置，并获取指针之前的数组获取切片进行下载
      if (mBeforePeer.size() > 0 && (tr == null || bIndex != 0) && bIndex < mBeforePeer.size()) {
        tr = mBeforePeer.valueAt(bIndex);
        bIndex++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return tr;
  }

  /**
   * 启动线程任务
   */
  private void addTaskToQueue(ThreadRecord tr) throws InterruptedException {
    ThreadTask task = createThreadTask(mCacheDir, tr, tr.threadId);
    getTaskList().put(tr.threadId, task);
    getEntity().getM3U8Entity().setPeerIndex(tr.threadId);
    TempFlag flag = startThreadTask(task, tr.threadId);
    if (flag != null) {
      mFlagQueue.put(flag);
    }
  }

  /**
   * 初始化数据
   */
  private void initData() {
    mCacheDir = getCacheDir();
    if (mM3U8Option.getJumpIndex() != 0) {
      mCurrentEvent = new PeerIndexEvent(mTaskWrapper.getKey(), mM3U8Option.getJumpIndex());
      resumeTask();
      return;
    }
    // 设置需要下载的切片
    mCompleteNum = 0;
    for (ThreadRecord tr : mRecord.threadRecords) {
      if (!tr.isComplete) {
        mAfterPeer.put(tr.threadId, tr);
      } else {
        mCompleteNum++;
      }
    }
    mManager.updateStateCount();
  }

  /**
   * 每隔几秒钟检查jump队列，取最新的事件处理
   */
  private synchronized void startJumpThread() {
    jumpThread = new Thread(new Runnable() {
      @Override public void run() {
        try {
          PeerIndexEvent event;
          while (!isBreak()) {
            try {
              EVENT_LOCK.lock();
              PeerIndexEvent temp = null;
              // 取最新的事件
              while ((event = mJumpQueue.poll(1, TimeUnit.SECONDS)) != null) {
                temp = event;
              }

              if (temp != null) {
                handleJump(temp);
              }
              mEventQueueCondition.await();
            } finally {
              EVENT_LOCK.unlock();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    jumpThread.start();
  }

  /**
   * 处理跳转
   */
  private void handleJump(PeerIndexEvent event) {
    if (isBreak()) {
      ALog.e(TAG, "任务已停止，处理跳转失败");
      return;
    }
    mCurrentEvent = event;
    if (mRecord == null || mRecord.threadRecords == null) {
      ALog.e(TAG, "跳到指定位置失败，记录为空");
      return;
    }
    if (event.peerIndex >= mRecord.threadRecords.size()) {
      ALog.e(TAG,
          String.format("切片索引设置错误，切片最大索引为：%s，当前设置的索引为：%s", mRecord.threadRecords.size(),
              event.peerIndex));
      return;
    }
    ALog.i(TAG, String.format("将优先下载索引【%s】之后的切片", event.peerIndex));

    isJump = true;
    notifyWaitLock(false);
    mCurrentFlagSize = mFlagQueue.size();
    // 停止所有正在执行的线程任务
    try {
      TempFlag flag;
      while ((flag = mFlagQueue.poll()) != null) {
        flag.threadTask.stop();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    ALog.d(TAG, "完成停止队列中的切片任务");
  }

  /**
   * 下载指定索引后面的切片
   * 如果指定的切片索引大于切片总数，则此操作无效
   * 如果指定的切片索引小于当前正在下载的切片索引，并且指定索引和当前索引区间内有未下载的切片，则优先下载该区间的切片；否则此操作无效
   * 如果指定索引后的切片已经全部下载完成，但是索引前有未下载的切片，间会自动下载未下载的切片
   */
  @Event
  public synchronized void jumpPeer(PeerIndexEvent event) {
    if (!event.key.equals(mTaskWrapper.getKey())) {
      return;
    }
    if (isBreak()) {
      ALog.e(TAG, "任务已停止，发送跳转事件失败");
      return;
    }
    if (jumpThread == null) {
      mJumpThreadPool = Executors.newSingleThreadExecutor();
      startJumpThread();
    }
    mJumpQueue.offer(event);
    mJumpThreadPool.submit(new Runnable() {
      @Override public void run() {
        try {
          Thread.sleep(1000);
          notifyJumpQueue();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void notifyJumpQueue() {
    try {
      EVENT_LOCK.lock();
      mEventQueueCondition.signalAll();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      EVENT_LOCK.unlock();
    }
  }

  /**
   * 从指定位置恢复任务
   */
  private synchronized void resumeTask() {
    if (isBreak()) {
      ALog.e(TAG, "任务已停止，恢复任务失败");
      return;
    }
    if (mJumpQueue.size() > 0) {
      ALog.d(TAG, "有新定位，取消上一次操作");
      notifyJumpQueue();
      return;
    }
    ALog.d(TAG, "恢复切片任务");
    // 重新初始化需要下载的分片
    mBeforePeer.clear();
    mAfterPeer.clear();
    mFlagQueue.clear();
    aIndex = 0;
    bIndex = 0;
    mCompleteNum = 0;
    for (ThreadRecord tr : mRecord.threadRecords) {
      if (tr.isComplete) {
        mCompleteNum++;
        continue;
      }
      if (tr.threadId < mCurrentEvent.peerIndex) {
        mBeforePeer.put(tr.threadId, tr);
      } else {
        mAfterPeer.put(tr.threadId, tr);
      }
    }

    ALog.i(TAG,
        String.format("beforeSize = %s, afterSize = %s, mCompleteNum = %s", mBeforePeer.size(),
            mAfterPeer.size(), mCompleteNum));
    ALog.i(TAG, String.format("完成处理数据的操作，将优先下载【%s】之后的切片", mCurrentEvent.peerIndex));
    mManager.updateStateCount();

    try {
      JUMP_LOCK.lock();
      mJumpCondition.signalAll();
    } finally {
      JUMP_LOCK.unlock();
    }
  }

  private M3U8Listener getListener() {
    return (M3U8Listener) mListener;
  }

  private void notifyWaitLock(boolean isComplete) {
    try {
      LOCK.lock();
      if (isComplete) {
        TempFlag flag = mFlagQueue.poll(1, TimeUnit.SECONDS);
        if (flag != null) {
          ALog.d(TAG, String.format("切片【%s】完成", flag.threadId));
        }
      }
      mWaitCondition.signalAll();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      LOCK.unlock();
    }
  }

  /**
   * 启动线程任务
   *
   * @return 线程唯一id标志
   */
  private TempFlag startThreadTask(ThreadTask task, int peerIndex) {
    if (isBreak()) {
      ALog.w(TAG, "任务已停止，启动线程任务失败");
      return null;
    }
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    getListener().onPeerStart(mTaskWrapper.getKey(), task.getConfig().tempFile.getPath(),
        peerIndex);
    TempFlag flag = new TempFlag();
    flag.threadTask = task;
    flag.threadId = peerIndex;
    return flag;
  }

  /**
   * 配置config
   */
  private ThreadTask createThreadTask(String cacheDir, ThreadRecord record, int index) {
    SubThreadConfig config = new SubThreadConfig();
    config.url = record.tsUrl;
    config.tempFile = new File(BaseM3U8Loader.getTsFilePath(cacheDir, record.threadId));
    config.isBlock = mRecord.isBlock;
    config.isOpenDynamicFile = mRecord.isOpenDynamicFile;
    config.taskWrapper = mTaskWrapper;
    config.record = record;
    config.stateHandler = mStateHandler;
    config.peerIndex = index;
    if (!config.tempFile.exists()) {
      FileUtil.createFile(config.tempFile.getPath());
    }
    ThreadTask threadTask = new ThreadTask(config);
    M3U8ThreadTaskAdapter adapter = new M3U8ThreadTaskAdapter(config);
    threadTask.setAdapter(adapter);
    return threadTask;
  }

  /**
   * M3U8线程状态管理
   */
  private class VodStateManager implements IThreadState {
    private final String TAG = "M3U8ThreadStateManager";

    /**
     * 任务状态回调
     */
    private IEventListener listener;
    private int startThreadNum;    // 启动的线程总数
    private int cancelNum = 0; // 已经取消的线程的数
    private int stopNum = 0;  // 已经停止的线程数
    private int failNum = 0;  // 失败的线程数
    private long progress; //当前总进度
    private TaskRecord taskRecord; // 任务记录
    private Looper looper;

    /**
     * @param taskRecord 任务记录
     * @param listener 任务事件
     */
    VodStateManager(Looper looper, TaskRecord taskRecord, IEventListener listener) {
      this.looper = looper;
      this.taskRecord = taskRecord;
      for (ThreadRecord record : taskRecord.threadRecords) {
        if (!record.isComplete) {
          startThreadNum++;
        }
      }
      this.listener = listener;
    }

    private void updateStateCount() {
      cancelNum = 0;
      stopNum = 0;
      failNum = 0;
    }

    /**
     * 退出looper循环
     */
    private void quitLooper() {
      ALog.d(TAG, "quitLooper");
      looper.quit();
    }

    @Override public boolean handleMessage(Message msg) {
      int peerIndex = msg.getData().getInt(ISchedulers.DATA_M3U8_PEER_INDEX);
      switch (msg.what) {
        case STATE_STOP:
          stopNum++;
          removeSignThread((ThreadTask) msg.obj);
          // 处理跳转位置后，恢复任务
          if (isJump && (stopNum == mCurrentFlagSize || mCurrentFlagSize == 0) && !isBreak()) {
            resumeTask();
            return true;
          }

          if (isBreak()) {
            ALog.d(TAG, String.format("vod任务【%s】停止", mTempFile.getName()));
            quitLooper();
          }
          break;
        case STATE_CANCEL:
          cancelNum++;
          removeSignThread((ThreadTask) msg.obj);

          if (isBreak()) {
            ALog.d(TAG, String.format("vod任务【%s】取消", mTempFile.getName()));
            quitLooper();
          }
          break;
        case STATE_FAIL:
          failNum++;
          for (ThreadRecord tr : mRecord.threadRecords) {
            if (tr.threadId == peerIndex) {
              mBeforePeer.put(peerIndex, tr);
              break;
            }
          }

          getListener().onPeerFail(mTaskWrapper.getKey(),
              msg.getData().getString(ISchedulers.DATA_M3U8_PEER_PATH), peerIndex);
          if (isFail()) {
            ALog.d(TAG, String.format("vod任务【%s】失败", mTempFile.getName()));
            Bundle b = msg.getData();
            listener.onFail(b.getBoolean(KEY_RETRY, true),
                (BaseException) b.getSerializable(KEY_ERROR_INFO));
            quitLooper();
          }
          break;
        case STATE_COMPLETE:
          if (isBreak()) {
            quitLooper();
          }
          mCompleteNum++;
          // 正在切换位置时，切片完成，队列减小
          if (isJump) {
            mCurrentFlagSize--;
            if (mCurrentFlagSize < 0) {
              mCurrentFlagSize = 0;
            }
          }

          removeSignThread((ThreadTask) msg.obj);
          getListener().onPeerComplete(mTaskWrapper.getKey(),
              msg.getData().getString(ISchedulers.DATA_M3U8_PEER_PATH), peerIndex);
          handlerPercent();
          if (!isJump) {
            notifyWaitLock(true);
          }
          if (isComplete()) {
            ALog.d(TAG, String.format(
                "startThreadNum = %s, stopNum = %s, cancelNum = %s, failNum = %s, completeNum = %s, flagQueueSize = %s",
                startThreadNum, stopNum, cancelNum, failNum, mCompleteNum, mFlagQueue.size()));
            ALog.d(TAG, String.format("vod任务【%s】完成", mTempFile.getName()));
            if (mM3U8Option.isMergeFile()) {
              if (mergeFile()) {
                listener.onComplete();
              } else {
                listener.onFail(false, null);
              }
            } else {
              listener.onComplete();
            }
            quitLooper();
          }
          break;
        case STATE_RUNNING:
          progress += (long) msg.obj;
          break;
      }
      return true;
    }

    private void removeSignThread(ThreadTask threadTask) {
      int index = getTaskList().indexOfValue(threadTask);
      if (index != -1) {
        getTaskList().removeAt(index);
      }
      ThreadTaskManager.getInstance().removeSingleTaskThread(mTaskWrapper.getKey(), threadTask);
    }

    /**
     * 设置进度
     */
    private void handlerPercent() {
      int completeNum = mM3U8Option.getCompleteNum();
      completeNum++;
      mM3U8Option.setCompleteNum(completeNum);
      int percent = completeNum * 100 / taskRecord.threadRecords.size();
      getEntity().setPercent(percent);
      getEntity().update();
    }

    @Override public boolean isFail() {
      printInfo("isFail");
      return failNum != 0 && failNum == mFlagQueue.size() && !isJump;
    }

    @Override public boolean isComplete() {
      return mCompleteNum == taskRecord.threadRecords.size() && !isJump;
    }

    @Override public long getCurrentProgress() {
      return progress;
    }

    private void printInfo(String tag) {
      if (false) {
        ALog.d(tag, String.format(
            "startThreadNum = %s, stopNum = %s, cancelNum = %s, failNum = %s, completeNum = %s, flagQueueSize = %s",
            startThreadNum, stopNum, cancelNum, failNum, mCompleteNum, mFlagQueue.size()));
      }
    }

    /**
     * 合并文件
     *
     * @return {@code true} 合并成功，{@code false}合并失败
     */
    private boolean mergeFile() {
      if (getEntity().getM3U8Entity().isGenerateIndexFile()) {
        return generateIndexFile();
      }
      ITsMergeHandler mergeHandler = mM3U8Option.getMergeHandler();
      String cacheDir = getCacheDir();
      List<String> partPath = new ArrayList<>();
      for (ThreadRecord tr : taskRecord.threadRecords) {
        partPath.add(BaseM3U8Loader.getTsFilePath(cacheDir, tr.threadId));
      }
      boolean isSuccess;
      if (mergeHandler != null) {
        isSuccess = mergeHandler.merge(getEntity().getM3U8Entity(), partPath);

        if (mergeHandler.getClass().isAnonymousClass()) {
          mM3U8Option.setMergeHandler(null);
        }
      } else {
        isSuccess = FileUtil.mergeFile(taskRecord.filePath, partPath);
      }
      if (isSuccess) {
        // 合并成功，删除缓存文件
        File[] files = new File(cacheDir).listFiles();
        for (File f : files) {
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
  }

  private static class TempFlag {
    ThreadTask threadTask;
    int threadId;
  }
}
