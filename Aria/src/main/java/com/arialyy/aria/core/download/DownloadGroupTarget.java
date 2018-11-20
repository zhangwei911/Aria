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
package com.arialyy.aria.core.download;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.common.http.HttpHeaderDelegate;
import com.arialyy.aria.core.common.http.PostDelegate;
import com.arialyy.aria.core.inf.IHttpHeaderDelegate;
import com.arialyy.aria.core.manager.TEManager;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by AriaL on 2017/6/29.
 * 下载任务组
 */
public class DownloadGroupTarget extends BaseGroupTarget<DownloadGroupTarget> implements
    IHttpHeaderDelegate<DownloadGroupTarget> {
  private HttpHeaderDelegate<DownloadGroupTarget> mDelegate;
  /**
   * 子任务下载地址，
   */
  private List<String> mUrls = new ArrayList<>();

  /**
   * 子任务文件名
   */
  private List<String> mSubNameTemp = new ArrayList<>();

  public DownloadGroupTarget(DownloadGroupEntity groupEntity, String targetName) {
    this.mTargetName = targetName;
    if (groupEntity.getUrls() != null && !groupEntity.getUrls().isEmpty()) {
      this.mUrls.addAll(groupEntity.getUrls());
    }
    init();
  }

  DownloadGroupTarget(List<String> urls, String targetName) {
    this.mTargetName = targetName;
    this.mUrls = urls;
    init();
  }

  private void init() {
    mGroupName = CommonUtil.getMd5Code(mUrls);
    mTaskEntity = TEManager.getInstance().getGTEntity(DownloadGroupTaskEntity.class, mUrls);
    mEntity = mTaskEntity.getEntity();
    if (mEntity != null) {
      mDirPathTemp = mEntity.getDirPath();
    }
    mDelegate = new HttpHeaderDelegate<>(this);
  }

  /**
   * Post处理
   */
  public PostDelegate asPost() {
    return new PostDelegate<>(this);
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult
  public DownloadGroupTarget updateUrls(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      throw new NullPointerException("下载地址列表为空");
    }
    if (urls.size() != mUrls.size()) {
      throw new IllegalArgumentException("新下载地址数量和旧下载地址数量不一致");
    }
    mUrls.clear();
    mUrls.addAll(urls);
    mGroupName = CommonUtil.getMd5Code(urls);
    mEntity.setGroupName(mGroupName);
    mTaskEntity.setKey(mGroupName);
    mEntity.update();
    if (mEntity.getSubEntities() != null && !mEntity.getSubEntities().isEmpty()) {
      for (DownloadEntity de : mEntity.getSubEntities()) {
        de.setGroupName(mGroupName);
        de.update();
      }
    }
    return this;
  }

  /**
   * 任务组总任务大小，任务组是一个抽象的概念，没有真实的数据实体，任务组的大小是Aria动态获取子任务大小相加而得到的，
   * 如果你知道当前任务组总大小，你也可以调用该方法给任务组设置大小
   *
   * 为了更好的用户体验，建议直接设置任务组文件大小
   *
   * @param fileSize 任务组总大小
   */
  @CheckResult
  public DownloadGroupTarget setFileSize(long fileSize) {
    if (fileSize <= 0) {
      ALog.e(TAG, "文件大小不能小于 0");
      return this;
    }
    if (mEntity.getFileSize() <= 1 || mEntity.getFileSize() != fileSize) {
      mEntity.setFileSize(fileSize);
    }
    return this;
  }

  /**
   * 如果你是使用{@link DownloadReceiver#load(DownloadGroupEntity)}进行下载操作，那么你需要设置任务组的下载地址
   */
  @CheckResult
  public DownloadGroupTarget setGroupUrl(List<String> urls) {
    mUrls.clear();
    mUrls.addAll(urls);
    return this;
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   *
   * @deprecated {@link #setSubFileName(List)} 请使用该api
   */
  @CheckResult
  @Deprecated public DownloadGroupTarget setSubTaskFileName(List<String> subTaskFileName) {
    return setSubFileName(subTaskFileName);
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   */
  @CheckResult
  public DownloadGroupTarget setSubFileName(List<String> subTaskFileName) {
    if (subTaskFileName == null || subTaskFileName.isEmpty()) {
      ALog.e(TAG, "修改子任务的文件名失败：列表为null");
      return this;
    }
    if (subTaskFileName.size() != mTaskEntity.getSubTaskEntities().size()) {
      ALog.e(TAG, "修改子任务的文件名失败：子任务文件名列表数量和子任务的数量不匹配");
      return this;
    }
    mSubNameTemp.clear();
    mSubNameTemp.addAll(subTaskFileName);
    return this;
  }

  @Override protected int getTargetType() {
    return GROUP_HTTP;
  }

  @Override protected boolean checkEntity() {
    if (getTargetType() == GROUP_HTTP) {
      if (!checkDirPath()) {
        return false;
      }

      if (!checkSubName()) {
        return false;
      }

      if (!checkUrls()) {
        return false;
      }

      if (mTaskEntity.getRequestEnum() == RequestEnum.POST) {
        for (DownloadTaskEntity subTask : mTaskEntity.getSubTaskEntities()) {
          subTask.setRequestEnum(RequestEnum.POST);
        }
      }

      mEntity.save();
      mTaskEntity.save();

      if (needModifyPath) {
        reChangeDirPath(mDirPathTemp);
      }

      if (!mSubNameTemp.isEmpty()) {
        updateSingleSubFileName();
      }
      return true;
    }
    return false;
  }

  /**
   * 更新所有改动的子任务文件名
   */
  private void updateSingleSubFileName() {
    List<DownloadTaskEntity> entities = mTaskEntity.getSubTaskEntities();
    int i = 0;
    for (DownloadTaskEntity entity : entities) {
      if (i < mSubNameTemp.size()) {
        String newName = mSubNameTemp.get(i);
        updateSingleSubFileName(entity, newName);
      }
      i++;
    }
  }

  /**
   * 检查urls是否合法，并删除不合法的子任务
   *
   * @return {@code true} 合法
   */
  private boolean checkUrls() {
    if (mUrls.isEmpty()) {
      ALog.e(TAG, "下载失败，子任务下载列表为null");
      return false;
    }
    Set<Integer> delItem = new HashSet<>();

    int i = 0;
    for (String url : mUrls) {
      if (TextUtils.isEmpty(url)) {
        ALog.e(TAG, "子任务url为null，即将删除该子任务。");
        delItem.add(i);
        continue;
      } else if (!url.startsWith("http")) {
        //} else if (!url.startsWith("http") && !url.startsWith("ftp")) {
        ALog.e(TAG, "子任务url【" + url + "】错误，即将删除该子任务。");
        delItem.add(i);
        continue;
      }
      int index = url.indexOf("://");
      if (index == -1) {
        ALog.e(TAG, "子任务url【" + url + "】不合法，即将删除该子任务。");
        delItem.add(i);
        continue;
      }

      i++;
    }

    for (int index : delItem) {
      mUrls.remove(index);
      if (mSubNameTemp != null && !mSubNameTemp.isEmpty()) {
        mSubNameTemp.remove(index);
      }
    }

    mEntity.setGroupName(CommonUtil.getMd5Code(mUrls));

    return true;
  }

  /**
   * 更新单个子任务文件名
   */
  private void updateSingleSubFileName(DownloadTaskEntity taskEntity, String newName) {
    DownloadEntity entity = taskEntity.getEntity();
    if (!newName.equals(entity.getFileName())) {
      String oldPath = mEntity.getDirPath() + "/" + entity.getFileName();
      String newPath = mEntity.getDirPath() + "/" + newName;
      if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=? or isComplete='true'",
          newPath)) {
        ALog.w(TAG, String.format("更新文件名失败，路径【%s】已存在或文件已下载", newPath));
        return;
      }

      File oldFile = new File(oldPath);
      if (oldFile.exists()) {
        oldFile.renameTo(new File(newPath));
      }

      CommonUtil.modifyTaskRecord(oldFile.getPath(), newPath);
      entity.setDownloadPath(newPath);
      taskEntity.setKey(newPath);
      entity.setFileName(newName);
      entity.update();
    }
  }

  /**
   * 如果用户设置了子任务文件名，检查子任务文件名
   *
   * @return {@code true} 合法
   */
  private boolean checkSubName() {
    if (mSubNameTemp == null || mSubNameTemp.isEmpty()) {
      return true;
    }
    if (mUrls.size() != mSubNameTemp.size()) {
      ALog.e(TAG, "子任务文件名必须和子任务数量一致");
      return false;
    }

    return true;
  }

  @CheckResult
  @Override public DownloadGroupTarget addHeader(@NonNull String key, @NonNull String value) {
    for (DownloadTaskEntity subTask : mTaskEntity.getSubTaskEntities()) {
      mDelegate.addHeader(subTask, key, value);
    }
    return mDelegate.addHeader(key, value);
  }

  @CheckResult
  @Override public DownloadGroupTarget addHeaders(Map<String, String> headers) {
    for (DownloadTaskEntity subTask : mTaskEntity.getSubTaskEntities()) {
      mDelegate.addHeaders(subTask, headers);
    }
    return mDelegate.addHeaders(headers);
  }

  @CheckResult
  @Override public DownloadGroupTarget setUrlProxy(Proxy proxy) {
    return mDelegate.setUrlProxy(proxy);
  }
}
