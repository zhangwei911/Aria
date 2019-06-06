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

import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.m3u8.BaseM3U8Loader;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.DbDataHelper;
import com.arialyy.aria.util.RecordUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 处理任务记录，分配线程区间
 */
public class RecordHandler {
  private final String TAG = "RecordHandler";

  public static final int TYPE_DOWNLOAD = 1;
  public static final int TYPE_UPLOAD = 2;
  public static final int TYPE_M3U8_VOD = 3;
  public static final int TYPE_M3U8_LIVE = 4;

  private static final String STATE = "_state_";
  private static final String RECORD = "_record_";
  /**
   * 小于1m的文件不启用多线程
   */
  private static final long SUB_LEN = 1024 * 1024;

  /**
   * 分块文件路径
   */
  public static final String SUB_PATH = "%s.%s.part";

  @Deprecated private File mConfigFile;
  private TaskRecord mRecord;
  private AbsTaskWrapper mTaskWrapper;
  private AbsNormalEntity mEntity;

  RecordHandler(AbsTaskWrapper wrapper) {
    mTaskWrapper = wrapper;
    mEntity = (AbsNormalEntity) mTaskWrapper.getEntity();
  }

  /**
   * 获取任务记录，如果任务记录存在，检查任务记录
   * 检查记录 对于分块任务： 子分块不存在或被删除，子线程将重新下载
   * 对于普通任务： 预下载文件不存在，则任务任务呗删除
   * 如果任务记录不存在或线程记录不存在，初始化记录
   *
   * @return 任务记录
   */
  TaskRecord getRecord() {
    mConfigFile = new File(CommonUtil.getFileConfigPath(false, mEntity.getFileName()));
    if (mConfigFile.exists()) {
      convertDb();
    } else {
      mRecord = DbDataHelper.getTaskRecord(getFilePath());
      if (mRecord == null) {
        initRecord(true);
      } else {
        if (mRecord.threadRecords == null || mRecord.threadRecords.isEmpty()) {
          initRecord(false);
        }

        if (mTaskWrapper.getRequestType() == ITaskWrapper.M3U8_VOD) {
          handleM3U8Record();
        } else if (mTaskWrapper.getRequestType() == ITaskWrapper.M3U8_LIVE) {
          ALog.i(TAG, "直播下载不处理历史记录");
        } else {
          if (mRecord.isBlock) {
            handleBlockRecord();
          } else if (!mTaskWrapper.isSupportBP()) {
            handleNoSupportBPRecord();
          } else {
            handleSingleThreadRecord();
          }
        }
      }
    }
    saveRecord();
    return mRecord;
  }

  /**
   * 处理m3u8记录，
   * 1、如果分片文件存在，并且分片文件的记录没有完成，则需要删除该分片文件
   * 2、如果记录显示已完成，但是分片文件不存在，则重新开始该分片
   * 3、如果记录显示已完成，并且文件存在，记录当前任务进度
   */
  private void handleM3U8Record() {
    DTaskWrapper wrapper = (DTaskWrapper) mTaskWrapper;
    String cacheDir = wrapper.asM3U8().getCacheDir();
    long currentProgress = 0;
    int completeNum = 0;
    for (ThreadRecord record : mRecord.threadRecords) {
      File temp = new File(BaseM3U8Loader.getTsFilePath(cacheDir, record.threadId));
      if (!record.isComplete) {
        if (temp.exists()) {
          temp.delete();
        }
        record.startLocation = 0;
        //ALog.d(TAG, String.format("分片【%s】未完成，将重新下载该分片", record.threadId));
      } else {
        if (!temp.exists()) {
          record.startLocation = 0;
          record.isComplete = false;
          ALog.w(TAG, String.format("分片【%s】不存在，将重新下载该分片", record.threadId));
        } else {
          completeNum++;
          currentProgress += temp.length();
        }
      }
    }
    wrapper.asM3U8().setCompleteNum(completeNum);
    wrapper.getEntity().setCurrentProgress(currentProgress);
  }

  /**
   * 处理不支持断点的记录
   */
  private void handleNoSupportBPRecord() {
    ThreadRecord tr = mRecord.threadRecords.get(0);
    tr.startLocation = 0;
    tr.endLocation = mEntity.getFileSize();
    tr.key = mRecord.filePath;
    tr.blockLen = tr.endLocation;
    tr.isComplete = false;
  }

  /**
   * 处理单线程的任务的记录
   */
  private void handleSingleThreadRecord() {
    File file = new File(mRecord.filePath);
    ThreadRecord tr = mRecord.threadRecords.get(0);
    if (!file.exists()) {
      ALog.w(TAG, String.format("文件【%s】不存在，任务将重新开始", file.getPath()));
      tr.startLocation = 0;
      tr.isComplete = false;
      tr.endLocation = mEntity.getFileSize();
    } else if (mRecord.isOpenDynamicFile) {
      if (file.length() > mEntity.getFileSize()) {
        ALog.i(TAG, String.format("文件【%s】错误，任务重新开始", file.getPath()));
        file.delete();
        tr.startLocation = 0;
        tr.isComplete = false;
        tr.endLocation = mEntity.getFileSize();
      } else if (file.length() == mEntity.getFileSize()) {
        tr.isComplete = true;
      } else {
        if (file.length() != tr.startLocation) {
          ALog.i(TAG, String.format("修正【%s】的进度记录为：%s", file.getPath(), file.length()));
          tr.startLocation = file.length();
          tr.isComplete = false;
        }
      }
    }
    mTaskWrapper.setNewTask(false);
  }

  /**
   * 处理分块任务的记录，分块文件（blockFileLen）长度必须需要小于等于线程区间（threadRectLen）的长度
   */
  private void handleBlockRecord() {
    // 默认线程分块长度
    long normalRectLen = mEntity.getFileSize() / mRecord.threadRecords.size();
    for (ThreadRecord tr : mRecord.threadRecords) {
      long threadRect = tr.blockLen;

      File temp = new File(String.format(SUB_PATH, mRecord.filePath, tr.threadId));
      if (!temp.exists()) {
        ALog.i(TAG, String.format("分块文件【%s】不存在，该分块将重新开始", temp.getPath()));
        tr.isComplete = false;
        tr.startLocation = tr.threadId * normalRectLen;
      } else {
        if (!tr.isComplete) {
          ALog.i(TAG, String.format(
              "startLocation = %s; endLocation = %s; block = %s; tempLen = %s; threadId = %s",
              tr.startLocation, tr.endLocation, threadRect, temp.length(), tr.threadId));

          long blockFileLen = temp.length(); // 磁盘中的分块文件长度
          /*
           * 检查磁盘中的分块文件
           */
          if (blockFileLen > threadRect) {
            ALog.i(TAG, String.format("分块【%s】错误，分块长度【%s】 > 线程区间长度【%s】，将重新开始该分块",
                tr.threadId, blockFileLen, threadRect));
            temp.delete();
            tr.startLocation = tr.threadId * threadRect;
            continue;
          }

          long realLocation =
              tr.threadId * normalRectLen + blockFileLen; //正常情况下，该线程的startLocation的位置
          /*
           * 检查记录文件
           */
          if (blockFileLen == threadRect) {
            ALog.i(TAG, String.format("分块【%s】已完成，更新记录", temp.getPath()));
            tr.startLocation = blockFileLen;
            tr.isComplete = true;
          } else if (tr.startLocation != realLocation) { // 处理记录小于分块文件长度的情况
            ALog.i(TAG, String.format("修正分块【%s】的进度记录为：%s", temp.getPath(), realLocation));
            tr.startLocation = realLocation;
          }
        } else {
          ALog.i(TAG, String.format("分块【%s】已完成", temp.getPath()));
        }
      }
    }
    mTaskWrapper.setNewTask(false);
  }

  /**
   * convertDb 是兼容性代码 从3.4.1开始，线程配置信息将存储在数据库中。 将配置文件的内容复制到数据库中，并将配置文件删除
   */
  private void convertDb() {
    List<RecordWrapper> records =
        DbEntity.findRelationData(RecordWrapper.class, "TaskRecord.filePath=?",
            getFilePath());
    if (records == null || records.size() == 0) {
      Properties pro = CommonUtil.loadConfig(mConfigFile);
      if (pro.isEmpty()) {
        ALog.d(TAG, "老版本的线程记录为空，任务为新任务");
        initRecord(true);
        return;
      }

      Set<Object> keys = pro.keySet();
      // 老版本记录是5s存一次，但是5s中内，如果线程执行完成，record记录是没有的，只有state记录...
      // 第一步应该是record 和 state去重取正确的线程数
      Set<Integer> set = new HashSet<>();
      for (Object key : keys) {
        String str = String.valueOf(key);
        int i = Integer.parseInt(str.substring(str.length() - 1));
        set.add(i);
      }
      int threadNum = set.size();
      if (threadNum == 0) {
        ALog.d(TAG, "线程数为空，任务为新任务");
        initRecord(true);
        return;
      }
      mTaskWrapper.setNewTask(false);
      mRecord = createTaskRecord(threadNum);
      mRecord.isOpenDynamicFile = false;
      mRecord.isBlock = false;
      File tempFile = new File(getFilePath());
      for (int i = 0; i < threadNum; i++) {
        ThreadRecord tRecord = new ThreadRecord();
        tRecord.key = mRecord.filePath;
        Object state = pro.getProperty(tempFile.getName() + STATE + i);
        Object record = pro.getProperty(tempFile.getName() + RECORD + i);
        if (state != null && Integer.parseInt(String.valueOf(state)) == 1) {
          tRecord.isComplete = true;
          continue;
        }
        if (record != null) {
          long temp = Long.parseLong(String.valueOf(record));
          tRecord.startLocation = temp > 0 ? temp : 0;
        } else {
          tRecord.startLocation = 0;
        }
        mRecord.threadRecords.add(tRecord);
      }
      mConfigFile.delete();
    }
  }

  /**
   * 初始化任务记录，分配线程区间
   *
   * @param newRecord {@code true} 需要创建新{@link TaskRecord}
   */
  private void initRecord(boolean newRecord) {
    if (newRecord) {
      mRecord = createTaskRecord(getNewTaskThreadNum());
    }
    mTaskWrapper.setNewTask(true);
    int requestType = mTaskWrapper.getRequestType();
    if (requestType == ITaskWrapper.M3U8_LIVE) {
      return;
    }
    long blockSize = mEntity.getFileSize() / mRecord.threadNum;
    // 处理线程区间记录
    for (int i = 0; i < mRecord.threadNum; i++) {
      long startL = i * blockSize, endL = (i + 1) * blockSize;
      ThreadRecord tr;
      tr = new ThreadRecord();
      tr.key = mRecord.filePath;
      tr.threadId = i;
      tr.startLocation = startL;
      tr.isComplete = false;
      if (requestType == ITaskWrapper.M3U8_VOD) {
        tr.startLocation = 0;
        tr.threadType = TaskRecord.TYPE_M3U8_VOD;
        tr.tsUrl = ((DTaskWrapper) mTaskWrapper).asM3U8().getUrls().get(i);
      } else {
        tr.threadType = TaskRecord.TYPE_HTTP_FTP;
        //最后一个线程的结束位置即为文件的总长度
        if (i == (mRecord.threadNum - 1)) {
          endL = mEntity.getFileSize();
        }
        tr.endLocation = endL;
        tr.blockLen = RecordUtil.getBlockLen(mEntity.getFileSize(), i, mRecord.threadNum);
      }
      mRecord.threadRecords.add(tr);
    }
  }

  /**
   * 创建任务记录
   *
   * @param threadNum 线程总数
   */
  private TaskRecord createTaskRecord(int threadNum) {
    TaskRecord record = new TaskRecord();
    record.fileName = mEntity.getFileName();
    record.filePath = getFilePath();
    record.threadRecords = new ArrayList<>();
    record.threadNum = threadNum;
    int requestType = mTaskWrapper.getRequestType();
    if (requestType == ITaskWrapper.M3U8_VOD) {
      record.taskType = TaskRecord.TYPE_M3U8_VOD;
      record.isOpenDynamicFile = true;
    } else if (requestType == ITaskWrapper.M3U8_LIVE) {
      record.taskType = TaskRecord.TYPE_M3U8_LIVE;
      record.isOpenDynamicFile = true;
    } else {
      if (getRecordType() == TYPE_DOWNLOAD) {
        record.isBlock = threadNum > 1 && Configuration.getInstance().downloadCfg.isUseBlock();
        // 线程数为1，或者使用了分块，则认为是使用动态长度文件
        record.isOpenDynamicFile = threadNum == 1 || record.isBlock;
      } else {
        record.isBlock = false;
      }
      record.taskType = TaskRecord.TYPE_HTTP_FTP;
      record.isGroupRecord = mEntity.isGroupChild();
      if (record.isGroupRecord) {
        if (mEntity instanceof DownloadEntity) {
          record.dGroupHash = ((DownloadEntity) mEntity).getGroupHash();
        }
      }
    }

    return record;
  }

  /**
   * 保存任务记录
   */
  private void saveRecord() {
    mRecord.threadNum = mRecord.threadRecords.size();
    mRecord.save();
    if (mRecord.threadRecords != null && !mRecord.threadRecords.isEmpty()) {
      DbEntity.saveAll(mRecord.threadRecords);
    }
    ALog.d(TAG, String.format("保存记录，线程记录数：%s", mRecord.threadRecords.size()));
  }

  /**
   * 获取记录类型
   *
   * @return {@link #TYPE_DOWNLOAD}、{@link #TYPE_UPLOAD}
   */
  private int getRecordType() {
    if (mEntity instanceof DownloadEntity) {
      return TYPE_DOWNLOAD;
    } else {
      return TYPE_UPLOAD;
    }
  }

  /**
   * 获取任务路径
   *
   * @return 任务文件路径
   */
  private String getFilePath() {
    if (mEntity instanceof DownloadEntity) {
      return ((DownloadEntity) mTaskWrapper.getEntity()).getFilePath();
    } else {
      return ((UploadEntity) mTaskWrapper.getEntity()).getFilePath();
    }
  }

  /**
   * 小于1m的文件或是任务组的子任务、线程数强制为1
   * 不支持断点或chunked模式的线程数都为，线程数强制为1
   */
  private int getNewTaskThreadNum() {
    if (getRecordType() == TYPE_DOWNLOAD) {
      if (mTaskWrapper.getRequestType() == ITaskWrapper.M3U8_VOD) {
        return ((DTaskWrapper) mTaskWrapper).asM3U8().getUrls().size();
      }
      if (mTaskWrapper.getRequestType() == ITaskWrapper.M3U8_LIVE) {
        return 1;
      }
      if (!mTaskWrapper.isSupportBP() || mTaskWrapper.asHttp().isChunked()) {
        return 1;
      }
      int threadNum = Configuration.getInstance().downloadCfg.getThreadNum();
      return mEntity.getFileSize() <= SUB_LEN
          || mEntity.isGroupChild()
          || threadNum == 1
          ? 1
          : threadNum;
    } else {
      return 1;
    }
  }
}
