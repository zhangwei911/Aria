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
package com.arialyy.aria.core.common;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 线程任务管理器，用于处理多线程下载时任务的状态回调
 */
public class ThreadStateManager implements IThreadState {
  private final String TAG = "ThreadTaskStateManager";

  /**
   * 任务状态回调
   */
  private IEventListener mListener;
  private int mThreadNum;    // 启动的线程总数
  private int mCancelNum = 0; // 已经取消的线程的数
  private int mStopNum = 0;  // 已经停止的线程数
  private int mFailNum = 0;  // 失败的线程数
  private int mCompleteNum = 0;  // 完成的线程数
  private long mProgress; //当前总进度
  private TaskRecord mTaskRecord; // 任务记录
  private Looper mLooper;

  /**
   * @param taskRecord 任务记录
   * @param listener 任务事件
   */
  ThreadStateManager(Looper looper, TaskRecord taskRecord, IEventListener listener) {
    mLooper = looper;
    mTaskRecord = taskRecord;
    mThreadNum = mTaskRecord.threadNum;
    mListener = listener;
  }

  @Override public boolean handleMessage(Message msg) {
    switch (msg.what) {
      case STATE_STOP:
        mStopNum++;
        if (isStop()) {
          quitLooper();
        }
        break;
      case STATE_CANCEL:
        mCancelNum++;
        if (isCancel()) {
          quitLooper();
        }
        break;
      case STATE_FAIL:
        mFailNum++;
        if (isFail()) {
          Bundle b = msg.getData();
          mListener.onFail(b.getBoolean(KEY_RETRY, true),
              (BaseException) b.getSerializable(KEY_ERROR_INFO));
          quitLooper();
        }
        break;
      case STATE_COMPLETE:
        mCompleteNum++;
        if (isComplete()) {
          ALog.d(TAG, "isComplete, completeNum = " + mCompleteNum);
          if (mTaskRecord.isBlock) {
            if (mergeFile()) {
              mListener.onComplete();
            } else {
              mListener.onFail(false, null);
            }
          } else {
            mListener.onComplete();
          }
          quitLooper();
        }
        break;
      case STATE_RUNNING:
        if (msg.obj instanceof Long) {
          mProgress += (long) msg.obj;
        }
        break;
      case STATE_UPDATE_PROGRESS:
        if (msg.obj == null) {
          mProgress = updateBlockProgress();
        } else if (msg.obj instanceof Long) {
          mProgress = (long) msg.obj;
        }
        break;
    }
    return false;
  }

  /**
   * 退出looper循环
   */
  private void quitLooper() {
    mLooper.quit();
  }

  /**
   * 获取当前任务下载进度
   *
   * @return 当前任务下载进度
   */
  @Override
  public long getCurrentProgress() {
    return mProgress;
  }

  /**
   * 所有子线程是否都已经停止
   */
  public boolean isStop() {
    //ALog.d(TAG,
    //    String.format("isStop; stopNum: %s, cancelNum: %s, failNum: %s, completeNum: %s", mStopNum,
    //        mCancelNum, mFailNum, mCompleteNum));
    return mStopNum == mThreadNum || mStopNum + mCompleteNum == mThreadNum;
  }

  /**
   * 所有子线程是否都已经失败
   */
  @Override
  public boolean isFail() {
    //ALog.d(TAG,
    //    String.format("isFail; stopNum: %s, cancelNum: %s, failNum: %s, completeNum: %s", mStopNum,
    //        mCancelNum, mFailNum, mCompleteNum));
    return mCompleteNum != mThreadNum
        && (mFailNum == mThreadNum || mFailNum + mCompleteNum == mThreadNum);
  }

  /**
   * 所有子线程是否都已经完成
   */
  @Override
  public boolean isComplete() {
    //ALog.d(TAG,
    //    String.format("isComplete; stopNum: %s, cancelNum: %s, failNum: %s, completeNum: %s",
    //        mStopNum,
    //        mCancelNum, mFailNum, mCompleteNum));
    return mCompleteNum == mThreadNum;
  }

  /**
   * 所有子线程是否都已经取消
   */
  public boolean isCancel() {
    //ALog.d(TAG, String.format("isCancel; stopNum: %s, cancelNum: %s, failNum: %s, completeNum: %s",
    //    mStopNum,
    //    mCancelNum, mFailNum, mCompleteNum));
    return mCancelNum == mThreadNum;
  }

  /**
   * 更新分块任务s的真实进度
   */
  private long updateBlockProgress() {
    long size = 0;
    for (int i = 0, len = mTaskRecord.threadRecords.size(); i < len; i++) {
      File temp = new File(String.format(RecordHandler.SUB_PATH, mTaskRecord.filePath, i));
      if (temp.exists()) {
        size += temp.length();
      }
    }
    return size;
  }

  /**
   * 合并文件
   *
   * @return {@code true} 合并成功，{@code false}合并失败
   */
  private boolean mergeFile() {
    List<String> partPath = new ArrayList<>();
    for (int i = 0, len = mTaskRecord.threadNum; i < len; i++) {
      partPath.add(String.format(RecordHandler.SUB_PATH, mTaskRecord.filePath, i));
    }
    boolean isSuccess = FileUtil.mergeFile(mTaskRecord.filePath, partPath);
    if (isSuccess) {
      for (String pp : partPath) {
        File f = new File(pp);
        if (f.exists()) {
          f.delete();
        }
      }
      File targetFile = new File(mTaskRecord.filePath);
      if (targetFile.exists() && targetFile.length() > mTaskRecord.fileLength) {
        ALog.e(TAG, String.format("任务【%s】分块文件合并失败，下载长度超出文件真实长度，downloadLen: %s，fileSize: %s",
            targetFile.getName(), targetFile.length(), mTaskRecord.fileLength));
        return false;
      }
      return true;
    } else {
      ALog.e(TAG, "合并失败");
      return false;
    }
  }
}
