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
package com.arialyy.aria.util;

import android.text.TextUtils;
import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.ThreadRecord;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.M3U8Entity;
import com.arialyy.aria.core.loader.IRecordHandler;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.wrapper.ITaskWrapper;
import com.arialyy.aria.core.wrapper.RecordWrapper;
import com.arialyy.aria.orm.DbEntity;
import java.io.File;
import java.util.List;

/**
 * 任务记录处理工具
 */
public class RecordUtil {
  private static final String TAG = "RecordUtil";

  /**
   * 删除任务组记录
   *
   * @param removeFile {@code true} 无论任务是否完成，都会删除记录和文件；
   * {@code false} 如果任务已经完成，则只删除记录，不删除文件；任务未完成，记录和文件都会删除。
   */
  public static void delGroupTaskRecordByHash(String groupHash, boolean removeFile) {
    if (TextUtils.isEmpty(groupHash)) {
      ALog.e(TAG, "删除下载任务组记录失败，groupHash为null");
      return;
    }
    DownloadGroupEntity groupEntity = DbDataHelper.getDGEntityByHash(groupHash);

    delGroupTaskRecord(groupEntity, removeFile, true);
  }

  /**
   * 根据路径删除组合任务记录
   *
   * @param removeFile {@code true} 无论任务是否完成，都会删除记录和文件；
   * {@code false} 如果任务已经完成，则只删除记录，不删除文件；任务未完成，记录和文件都会删除。
   */
  public static void delGroupTaskRecordByPath(String dirPath, boolean removeFile) {
    if (TextUtils.isEmpty(dirPath)) {
      ALog.e(TAG, "删除下载任务组记录失败，组合任务路径为空");
      return;
    }
    DownloadGroupEntity groupEntity = DbDataHelper.getDGEntityByPath(dirPath);
    // 处理组任务存在，而子任务为空的情况
    if (groupEntity == null) {
      groupEntity = DbEntity.findFirst(DownloadGroupEntity.class, "dirPath=?", dirPath);
      if (groupEntity != null) {
        groupEntity.deleteData();
        DbEntity.deleteData(DownloadEntity.class, "groupHash=?", groupEntity.getGroupHash());
      }
      return;
    }

    delGroupTaskRecord(groupEntity, removeFile, true);
  }

  /**
   * 删除任务组记录
   *
   * @param removeFile {@code true} 无论任务是否完成，都会删除记录和文件；
   * {@code false} 如果任务已经完成，则只删除记录，不删除文件；任务未完成，记录和文件都会删除。
   */
  public static void delGroupTaskRecord(DownloadGroupEntity groupEntity, boolean removeFile,
      boolean removeEntity) {
    if (groupEntity == null) {
      ALog.e(TAG, "删除下载任务组记录失败，任务组实体为null");
      return;
    }
    List<RecordWrapper> records =
        DbEntity.findRelationData(RecordWrapper.class, "dGroupHash=?", groupEntity.getGroupHash());

    if (records == null || records.isEmpty()) {
      ALog.w(TAG, "组任务记录已删除");
    } else {
      for (RecordWrapper record : records) {
        if (record == null || record.taskRecord == null) {
          continue;
        }
        // 删除分块文件
        if (record.taskRecord.isBlock) {
          for (int i = 0, len = record.taskRecord.threadNum; i < len; i++) {
            File partFile =
                new File(String.format(IRecordHandler.SUB_PATH, record.taskRecord.filePath, i));
            if (partFile.exists()) {
              partFile.delete();
            }
          }
        }
        DbEntity.deleteData(ThreadRecord.class, "taskKey=?", record.taskRecord.filePath);
        record.taskRecord.deleteData();
      }
    }

    List<DownloadEntity> subs = groupEntity.getSubEntities();
    if (subs != null) {
      for (DownloadEntity sub : subs) {
        File file = new File(sub.getFilePath());
        if (file.exists() && (removeFile || !sub.isComplete())) {
          file.delete();
        }
      }
    }

    // 删除文件夹
    if (!TextUtils.isEmpty(groupEntity.getDirPath())) {
      File dir = new File(groupEntity.getDirPath());
      if (dir.exists() && (removeFile || !groupEntity.isComplete())) {
        dir.delete();
      }
    }
    if (removeEntity) {
      DbEntity.deleteData(DownloadEntity.class, "groupHash=?", groupEntity.getGroupHash());
      DbEntity.deleteData(DownloadGroupEntity.class, "groupHash=?", groupEntity.getGroupHash());
    }
  }

  /**
   * 删除任务记录，默认删除文件
   *
   * @param removeFile {@code true} 无论任务是否完成，都会删除记录和文件；
   * {@code false} 如果是下载任务，并且任务已经完成，则只删除记录，不删除文件；任务未完成，记录和文件都会删除。
   * 如果是上传任务，无论任务是否完成，都只删除记录
   */
  public static void delTaskRecord(AbsNormalEntity entity, boolean removeFile) {
    if (entity == null) return;
    String filePath;
    int type;
    if (entity instanceof DownloadEntity) {
      type = IRecordHandler.TYPE_DOWNLOAD;
      filePath = entity.getFilePath();
    } else if (entity instanceof UploadEntity) {
      type = IRecordHandler.TYPE_UPLOAD;
      filePath = entity.getFilePath();
    } else {
      ALog.w(TAG, "删除记录失败，未知类型");
      return;
    }
    File targetFile = new File(filePath);
    TaskRecord record = getTaskRecord(filePath);
    if (record == null) {
      if (removeFile) {
        removeTargetFile(targetFile);
      }
      removeRecord(filePath);
      removeEntity(type, filePath);
      return;
    }

    /*
     * 处理任务未完成的情况
     */
    if (!entity.isComplete()) {
      if (recordIsM3U8(record.taskType)) { // 删除ts分片文件
        removeTsCache(targetFile, record.bandWidth);
      } else if (record.isBlock) { // 删除分块文件
        removeBlockFile(record);
      }
      removeTargetFile(targetFile);
    } else if (removeFile) { // 处理任务完成情况
      if (recordIsM3U8(record.taskType)) {
        removeTsCache(targetFile, record.bandWidth);
      }
      removeTargetFile(targetFile);
    }

    // 成功与否都将删除记录
    removeRecord(filePath);
  }

  /**
   * 任务记录是否是m3u8记录
   *
   * @param recordType 任务记录类型
   * @return true 为m3u8任务
   */
  private static boolean recordIsM3U8(int recordType) {
    return recordType == ITaskWrapper.M3U8_VOD || recordType == ITaskWrapper.M3U8_LIVE;
  }

  /**
   * 删除任务记录，默认删除文件
   *
   * @param filePath 文件路径
   * @param removeFile {@code true} 无论任务是否完成，都会删除记录和文件；
   * {@code false} 如果是下载任务，并且任务已经完成，则只删除记录，不删除文件；任务未完成，记录和文件都会删除。
   * 如果是上传任务，无论任务是否完成，都只删除记录
   * @param type {@link IRecordHandler#TYPE_DOWNLOAD}下载任务的记录，{@link IRecordHandler#TYPE_UPLOAD}
   * 上传任务的记录
   * @param removeEntity {@code true} 删除任务实体，
   */
  public static void delTaskRecord(String filePath, int type, boolean removeFile,
      boolean removeEntity) {
    if (TextUtils.isEmpty(filePath)) {
      throw new NullPointerException("删除记录失败，文件路径为空");
    }
    if (!filePath.startsWith("/")) {
      throw new IllegalArgumentException(String.format("文件路径错误，filePath：%s", filePath));
    }
    if (type != IRecordHandler.TYPE_DOWNLOAD && type != IRecordHandler.TYPE_UPLOAD) {
      throw new IllegalArgumentException("任务记录类型错误");
    }

    AbsEntity entity;
    if (type == IRecordHandler.TYPE_DOWNLOAD) {
      entity = DbEntity.findFirst(DownloadEntity.class, "downloadPath=?", filePath);
    } else {
      entity = DbEntity.findFirst(UploadEntity.class, "filePath=?", filePath);
    }
    File targetFile = new File(filePath);
    TaskRecord record = getTaskRecord(filePath);
    if (entity == null || record == null) {
      if (removeFile) {
        removeTargetFile(targetFile);
      }
      removeRecord(filePath);
      removeEntity(type, filePath);
      return;
    }

    /*
     * 处理任务未完成的情况
     */
    if (!entity.isComplete()) {
      if (recordIsM3U8(record.taskType)) { // 删除ts分片文件
        removeTsCache(targetFile, record.bandWidth);
      } else if (record.isBlock) { // 删除分块文件
        removeBlockFile(record);
      }
      removeTargetFile(targetFile);
    } else if (removeFile) { // 处理任务完成情况
      if (recordIsM3U8(record.taskType)) {
        removeTsCache(targetFile, record.bandWidth);
      }
      removeTargetFile(targetFile);
    }

    // 成功与否都将删除记录
    removeRecord(filePath);

    if (removeEntity) {
      removeEntity(type, filePath);
    }
  }

  private static void removeTargetFile(File targetFile) {
    if (targetFile.exists()) {
      targetFile.delete();
    }
  }

  private static void removeRecord(String filePath) {
    ALog.i(TAG, "删除任务记录");
    DbEntity.deleteData(ThreadRecord.class, "taskKey=?", filePath);
    DbEntity.deleteData(TaskRecord.class, "filePath=?", filePath);
    // 处理m3u8实体的删除
    DbEntity.deleteData(M3U8Entity.class, "filePath=?", filePath);
  }

  /**
   * 删除实体
   *
   * @param type {@link IRecordHandler#TYPE_DOWNLOAD}下载任务的记录，{@link IRecordHandler#TYPE_UPLOAD}
   * 上传任务的记录
   */
  private static void removeEntity(int type, String filePath) {
    if (type == IRecordHandler.TYPE_DOWNLOAD) {
      DbEntity.deleteData(DownloadEntity.class, "downloadPath=?", filePath);
    } else {
      DbEntity.deleteData(UploadEntity.class, "filePath=?", filePath);
    }
  }

  /**
   * 根据文件路径获取任务记录
   */
  private static TaskRecord getTaskRecord(String filePath) {
    List<RecordWrapper> recordWrapper =
        DbEntity.findRelationData(RecordWrapper.class, "filePath=?", filePath);
    if (recordWrapper == null
        || recordWrapper.isEmpty()
        || recordWrapper.get(0) == null
        || recordWrapper.get(0).taskRecord == null) {
      return null;
    } else {
      return recordWrapper.get(0).taskRecord;
    }
  }

  /**
   * 删除多线程分块下载的分块文件
   */
  private static void removeBlockFile(TaskRecord record) {
    for (int i = 0, len = record.threadNum; i < len; i++) {
      File partFile = new File(String.format(IRecordHandler.SUB_PATH, record.filePath, i));
      if (partFile.exists()) {
        partFile.delete();
      }
    }
  }

  /**
   * 删除ts文件，和索引文件（如果有的话）
   */
  private static void removeTsCache(File targetFile, long bandWidth) {

    String cacheDir = null;
    if (!targetFile.isDirectory()) {
      cacheDir =
          String.format("%s/.%s_%s", targetFile.getParent(), targetFile.getName(), bandWidth);
    }

    if (!TextUtils.isEmpty(cacheDir)) {
      File cacheDirF = new File(cacheDir);
      if (!cacheDirF.exists()) {
        return;
      }
      File[] files = cacheDirF.listFiles();
      for (File f : files) {
        if (f.exists()) {
          f.delete();
        }
      }
      File cDir = new File(cacheDir);
      if (cDir.exists()) {
        cDir.delete();
      }
    }

    File indexFile = new File(String.format("%s.index", targetFile.getPath()));

    if (indexFile.exists()) {
      indexFile.delete();
    }
  }

  /**
   * 删除任务记录，默认删除文件，删除任务实体
   *
   * @param filePath 文件路径
   * @param type {@link IRecordHandler#TYPE_DOWNLOAD}下载任务的记录，{@link IRecordHandler#TYPE_UPLOAD}
   * 上传任务的记录
   */
  public static void delTaskRecord(String filePath, int type) {
    delTaskRecord(filePath, type, false, true);
  }

  /**
   * 修改任务路径，修改文件路径和任务记录信息。如果是分块任务，则修改分块文件的路径。
   *
   * @param oldPath 旧的文件路径
   * @param newPath 新的文件路径
   * @param taskType 任务类型{@link ITaskWrapper}
   */
  public static void modifyTaskRecord(String oldPath, String newPath, int taskType) {
    if (oldPath.equals(newPath)) {
      ALog.w(TAG, "修改任务记录失败，新文件路径和旧文件路径一致");
      return;
    }
    TaskRecord record = DbDataHelper.getTaskRecord(oldPath, taskType);
    if (record == null) {
      if (new File(oldPath).exists()) {
        ALog.w(TAG, "修改任务记录失败，文件【" + oldPath + "】对应的任务记录不存在");
      }
      return;
    }
    if (!record.isBlock) {
      File oldFile = new File(oldPath);
      if (oldFile.exists()) {
        oldFile.renameTo(new File(newPath));
      }
    }

    record.filePath = newPath;
    record.update();
    // 修改线程记录
    if (record.threadRecords != null && !record.threadRecords.isEmpty()) {
      for (ThreadRecord tr : record.threadRecords) {
        tr.taskKey = newPath;
        File blockFile = new File(String.format(IRecordHandler.SUB_PATH, oldPath, tr.threadId));
        if (blockFile.exists()) {
          blockFile.renameTo(
              new File(String.format(IRecordHandler.SUB_PATH, newPath, tr.threadId)));
        }
      }
      DbEntity.updateManyData(record.threadRecords);
    }
  }

  /**
   * 检查分块任务是否存在
   *
   * @param filePath 文件保存路径
   * @return {@code true} 分块文件存在
   */
  public static boolean blockTaskExists(String filePath) {
    return new File(String.format(IRecordHandler.SUB_PATH, filePath, 0)).exists();
  }

  /**
   * 获取分块文件的快大小
   *
   * @param fileLen 文件总长度
   * @param blockId 分块id
   * @param blockNum 分块数量
   * @return 分块长度
   */
  public static long getBlockLen(long fileLen, int blockId, int blockNum) {
    final long averageLen = fileLen / blockNum;
    return blockId == blockNum - 1 ? (fileLen - blockId * averageLen) : averageLen;
  }
}
