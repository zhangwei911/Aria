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
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by Lyy on 2016/9/23.
 * 检查帮助类
 */
public class CheckUtil {
  private static final String TAG = "CheckUtil";

  /**
   * 检查成员类是否是静态和public
   */
  public static void checkMemberClass(Class clazz) {
    int modifiers = clazz.getModifiers();
    if (!clazz.isMemberClass() || !Modifier.isStatic(modifiers) || Modifier.isPrivate(modifiers)) {
      ALog.e(TAG, "为了放置内存泄漏，请使用静态的成员类(public static class xxx)或文件类(A.java)");
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
   * 检测下载链接是否为null
   */
  public static void checkPath(String path) {
    if (TextUtils.isEmpty(path)) {
      throw new IllegalArgumentException("保存路径不能为null");
    }
  }

  /**
   * 检测url是否合法，如果url不合法，将抛异常
   */
  public static void checkTaskId(long taskId) {
    if (taskId < 0) {
      throw new IllegalArgumentException("任务id不能小于0");
    }
  }

  /**
   * 检测url是否合法，如果url不合法，将抛异常
   */
  public static void checkUrlInvalidThrow(String url) {
    if (TextUtils.isEmpty(url)) {
      throw new IllegalArgumentException("url不能为null");
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      throw new IllegalArgumentException("url错误");
    }
    int index = url.indexOf("://");
    if (index == -1) {
      throw new IllegalArgumentException("url不合法");
    }
  }

  /**
   * 检测url是否合法，如果url不合法，将抛出{@link IllegalArgumentException}异常
   */
  public static void checkUrl(String url) {
    if (TextUtils.isEmpty(url)) {
      throw new NullPointerException("url为空");
    } else if (!url.startsWith("http") && !url.startsWith("ftp") && !url.startsWith("sftp")) {
      throw new IllegalArgumentException(String.format("url【%s】错误", url));
    }
    int index = url.indexOf("://");
    if (index == -1) {
      throw new IllegalArgumentException(String.format("url【%s】不合法", url));
    }
  }

  /**
   * 检测url是否合法
   *
   * @return {@code true} 合法，{@code false} 非法
   */
  public static boolean checkUrlNotThrow(String url) {
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
   */
  public static void checkDownloadUrls(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      throw new IllegalArgumentException("链接组不能为null");
    }
  }

  /**
   * 检测上传地址是否为null
   */
  public static void checkUploadPath(String uploadPath) {
    if (TextUtils.isEmpty(uploadPath)) {
      throw new IllegalArgumentException("上传地址不能为null");
    }
    File file = new File(uploadPath);
    if (!file.exists()) {
      throw new IllegalArgumentException("上传文件不存在");
    }
  }

  /**
   * 检查任务实体
   */
  public static void checkTaskEntity(AbsTaskWrapper entity) {
    if (entity instanceof DTaskWrapper) {
      checkDownloadTaskEntity(((DTaskWrapper) entity).getEntity());
    } else if (entity instanceof UTaskWrapper) {
      checkUploadTaskEntity(((UTaskWrapper) entity).getEntity());
    }
  }

  /**
   * 检查命令实体
   *
   * @param checkType 删除命令和停止命令不需要检查下载链接和保存路径
   * @return {@code false}实体无效
   */
  public static boolean checkCmdEntity(AbsTaskWrapper entity, boolean checkType) {
    boolean b = false;
    if (entity instanceof DTaskWrapper) {
      DownloadEntity entity1 = ((DTaskWrapper) entity).getEntity();
      if (entity1 == null) {
        ALog.e(TAG, "下载实体不能为空");
      } else if (checkType && TextUtils.isEmpty(entity1.getUrl())) {
        ALog.e(TAG, "下载链接不能为空");
      } else if (checkType && TextUtils.isEmpty(entity1.getDownloadPath())) {
        ALog.e(TAG, "保存路径不能为空");
      } else {
        b = true;
      }
    } else if (entity instanceof UTaskWrapper) {
      UploadEntity entity1 = ((UTaskWrapper) entity).getEntity();
      if (entity1 == null) {
        ALog.e(TAG, "上传实体不能为空");
      } else if (TextUtils.isEmpty(entity1.getFilePath())) {
        ALog.e(TAG, "上传文件路径不能为空");
      } else {
        b = true;
      }
    }
    return b;
  }

  /**
   * 检查上传实体是否合法
   */
  private static void checkUploadTaskEntity(UploadEntity entity) {
    if (entity == null) {
      throw new NullPointerException("上传实体不能为空");
    } else if (TextUtils.isEmpty(entity.getFilePath())) {
      throw new IllegalArgumentException("上传文件路径不能为空");
    } else if (TextUtils.isEmpty(entity.getFileName())) {
      throw new IllegalArgumentException("上传文件名不能为空");
    }
  }

  /**
   * 检测下载实体是否合法
   * 合法(true)
   *
   * @param entity 下载实体
   */
  private static void checkDownloadTaskEntity(DownloadEntity entity) {
    if (entity == null) {
      throw new NullPointerException("下载实体不能为空");
    } else if (TextUtils.isEmpty(entity.getUrl())) {
      throw new IllegalArgumentException("下载链接不能为空");
    } else if (TextUtils.isEmpty(entity.getFileName())) {
      throw new NullPointerException("文件名不能为null");
    } else if (TextUtils.isEmpty(entity.getDownloadPath())) {
      throw new NullPointerException("文件保存路径不能为null");
    }
  }
}