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
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IRecordHandler;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.orm.DbEntity;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by Lyy on 2016/9/23.
 * 检查帮助类
 */
public class CheckUtil {
  private static final String TAG = "CheckUtil";

  /**
   * 检查和处理路径冲突
   *
   * @param isForceDownload true，如果路径冲突，将删除其它任务的记录的
   * @param filePath 文件保存路径
   * @return false 任务不再执行，true 任务继续执行
   */
  public static boolean checkDownloadPathConflicts(boolean isForceDownload, String filePath) {
    if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=?", filePath)) {
      if (!isForceDownload) {
        ALog.e(TAG, String.format("下载失败，保存路径【%s】已经被其它任务占用，请设置其它保存路径", filePath));
        return false;
      } else {
        ALog.w(TAG, String.format("保存路径【%s】已经被其它任务占用，当前任务将覆盖该路径的文件", filePath));
        RecordUtil.delTaskRecord(filePath, IRecordHandler.TYPE_DOWNLOAD);
        return true;
      }
    }
    return true;
  }

  /**
   * 检查和处理路径冲突
   *
   * @param isForceUpload true，如果路径冲突，将删除其它任务的记录的
   * @param filePath 文件保存路径
   * @return false 任务不再执行，true 任务继续执行
   */
  public static boolean checkUploadPathConflicts(boolean isForceUpload, String filePath) {
    if (DbEntity.checkDataExist(UploadEntity.class, "filePath=?", filePath)) {
      if (!isForceUpload) {
        ALog.e(TAG, String.format("上传失败，文件路径【%s】已经被其它任务占用，请设置其它保存路径", filePath));
        return false;
      } else {
        ALog.w(TAG, String.format("文件路径【%s】已经被其它任务占用，当前任务将覆盖该路径的文件", filePath));
        RecordUtil.delTaskRecord(filePath, IRecordHandler.TYPE_UPLOAD);
        return true;
      }
    }
    return true;
  }

  /**
   * 检查成员类是否是静态和public
   */
  public static void checkMemberClass(Class clazz) {
    int modifiers = clazz.getModifiers();
    if (!clazz.isMemberClass() || !Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
      ALog.e(TAG, "为了防止内存泄漏，请使用静态的成员类(public static class xxx)或文件类(A.java)");
    }
  }

  /**
   * 检查分页数据，需要查询的页数，从1开始，如果page小于1 或 num 小于1，则抛出{@link NullPointerException}
   *
   * @param page 从1 开始
   * @param num 每页数量
   */
  public static void checkPageParams(int page, int num) {
    if (page < 1 || num < 1) throw new NullPointerException("page和num不能小于1");
  }

  /**
   * 检测url是否合法
   *
   * @return {@code true} 合法，{@code false} 非法
   */
  public static boolean checkUrl(String url) {
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "url不能为null");
      return false;
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      ALog.e(TAG, "url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "url【" + url + "】不合法");
    }
    return true;
  }

  /**
   * 检测下载链接组是否为null
   *
   * @return true 组合任务url为空
   */
  public static boolean checkDownloadUrlsIsEmpty(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      ALog.e(TAG, "链接组不能为null");
      return true;
    }
    return false;
  }

  /**
   * 检测上传地址是否为null
   */
  public static void checkUploadPathIsEmpty(String uploadPath) {
    if (TextUtils.isEmpty(uploadPath)) {
      throw new IllegalArgumentException("上传地址不能为null");
    }
  }
}