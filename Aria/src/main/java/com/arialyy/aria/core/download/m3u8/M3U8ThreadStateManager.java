///*
// * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.arialyy.aria.core.download.m3u8;
//
//import android.os.Bundle;
//import android.os.Looper;
//import android.os.Message;
//import com.arialyy.aria.core.common.IThreadState;
//import com.arialyy.aria.core.common.TaskRecord;
//import com.arialyy.aria.core.download.DTaskWrapper;
//import com.arialyy.aria.core.inf.IEventListener;
//import com.arialyy.aria.exception.BaseException;
//import com.arialyy.aria.util.ALog;
//
///**
// * M3U8线程状态管理
// */
//public class M3U8ThreadStateManager implements IThreadState {
//  private final String TAG = "M3U8ThreadStateManager";
//
//  /**
//   * 任务状态回调
//   */
//  private IEventListener mListener;
//  private int mThreadNum;    // 启动的线程总数
//  private int mCancelNum = 0; // 已经取消的线程的数
//  private int mStopNum = 0;  // 已经停止的线程数
//  private int mFailNum = 0;  // 失败的线程数
//  private int mCompleteNum = 0;  // 完成的线程数
//  private long mProgress; //当前总进度
//  private TaskRecord mTaskRecord; // 任务记录
//  private Looper mLooper;
//
//  /**
//   * @param taskRecord 任务记录
//   * @param listener 任务事件
//   */
//  M3U8ThreadStateManager(Looper looper, TaskRecord taskRecord, IEventListener listener) {
//    mLooper = looper;
//    mTaskRecord = taskRecord;
//    mThreadNum = mTaskRecord.threadNum;
//    mListener = listener;
//  }
//
//  /**
//   * 退出looper循环
//   */
//  private void quitLooper() {
//    mLooper.quit();
//  }
//
//  @Override public boolean handleMessage(Message msg) {
//    switch (msg.what) {
//      case STATE_STOP:
//        mStopNum++;
//        if (isStop()) {
//          mListener.onStop(mProgress);
//          quitLooper();
//        }
//        break;
//      case STATE_CANCEL:
//        mCancelNum++;
//        if (isCancel()) {
//          ALog.d(TAG, "icCancel");
//          mListener.onCancel();
//          quitLooper();
//        }
//        break;
//      case STATE_FAIL:
//        mFailNum++;
//        if (isFail()) {
//          Bundle b = msg.getData();
//          mListener.onFail(b.getBoolean(KEY_RETRY, true),
//              (BaseException) b.getSerializable(KEY_ERROR_INFO));
//          quitLooper();
//        }
//        break;
//      case STATE_COMPLETE:
//        mCompleteNum++;
//        if (isComplete()) {
//          ALog.d(TAG, "isComplete, completeNum = " + mCompleteNum);
//          if (mTaskRecord.isBlock) {
//            if (mergeFile()) {
//              mListener.onComplete();
//            } else {
//              mListener.onFail(false, null);
//            }
//          } else {
//            mListener.onComplete();
//          }
//          quitLooper();
//        }
//        break;
//      case STATE_RUNNING:
//        mProgress += (long) msg.obj;
//        break;
//      case STATE_UPDATE_PROGRESS:
//        if (msg.obj == null) {
//          mProgress = updateBlockProgress();
//        } else {
//          mProgress = (long) msg.obj;
//        }
//        break;
//    }
//    return false;
//  }
//
//  @Override public boolean isStop() {
//    return false;
//  }
//
//  @Override public boolean isFail() {
//    return false;
//  }
//
//  @Override public boolean isComplete() {
//    return false;
//  }
//
//  @Override public boolean isCancel() {
//    return false;
//  }
//
//  @Override public long getCurrentProgress() {
//    return mProgress;
//  }
//}
