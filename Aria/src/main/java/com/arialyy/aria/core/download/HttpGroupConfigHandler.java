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
import android.text.TextUtils;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by lyy on 2019/4/9.
 *
 * http组合任务功能代理
 */
class HttpGroupConfigHandler extends AbsGroupConfigHandler<DownloadGroupTarget> {

  /**
   * 子任务下载地址，
   */
  private List<String> mUrls = new ArrayList<>();

  /**
   * 子任务文件名
   */
  private List<String> mSubNameTemp = new ArrayList<>();

  HttpGroupConfigHandler(DownloadGroupTarget target, DGTaskWrapper wrapper) {
    super(target, wrapper);
    mUrls.addAll(wrapper.getEntity().getUrls());
  }

  @CheckResult
  DownloadGroupTarget setGroupUrl(List<String> urls) {
    mUrls.clear();
    mUrls.addAll(urls);
    return getTarget();
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   */
  @CheckResult
  DownloadGroupTarget setSubFileName(List<String> subTaskFileName) {
    if (subTaskFileName == null || subTaskFileName.isEmpty()) {
      ALog.w(TAG, "修改子任务的文件名失败：列表为null");
      return getTarget();
    }
    if (subTaskFileName.size() != getTaskWrapper().getSubTaskWrapper().size()) {
      ALog.w(TAG, "修改子任务的文件名失败：子任务文件名列表数量和子任务的数量不匹配");
      return getTarget();
    }
    mSubNameTemp.clear();
    mSubNameTemp.addAll(subTaskFileName);
    return getTarget();
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult
  DownloadGroupTarget updateUrls(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      throw new NullPointerException("下载地址列表为空");
    }
    if (urls.size() != mUrls.size()) {
      throw new IllegalArgumentException("新下载地址数量和旧下载地址数量不一致");
    }
    mUrls.clear();
    mUrls.addAll(urls);
    String newHash = CommonUtil.getMd5Code(urls);
    setGroupHash(newHash);
    getEntity().setGroupHash(newHash);
    getEntity().update();
    if (getEntity().getSubEntities() != null && !getEntity().getSubEntities().isEmpty()) {
      for (DownloadEntity de : getEntity().getSubEntities()) {
        de.setGroupHash(newHash);
        de.update();
      }
    }
    return getTarget();
  }

  @Override public boolean checkEntity() {
    if (!checkDirPath()) {
      return false;
    }

    if (!checkSubName()) {
      return false;
    }

    if (!checkUrls()) {
      return false;
    }

    if (!getTaskWrapper().isUnknownSize() && getTaskWrapper().getEntity().getFileSize() == 0) {
      ALog.e(TAG, "组合任务必须设置文件文件大小，默认需要强制设置文件大小。如果无法获取到总长度，请调用#unknownSize()来标志该组合任务");
      return false;
    }

    if (getTaskWrapper().asHttp().getRequestEnum() == RequestEnum.POST) {
      for (DTaskWrapper subTask : getTaskWrapper().getSubTaskWrapper()) {
        subTask.asHttp().setRequestEnum(RequestEnum.POST);
      }
    }

    if (isNeedModifyPath()) {
      reChangeDirPath(getDirPathTemp());
    }

    if (!mSubNameTemp.isEmpty()) {
      updateSingleSubFileName();
    }
    saveEntity();
    return true;
  }

  private void saveEntity() {
    getEntity().save();
    DbEntity.saveAll(getEntity().getSubEntities());
  }

  /**
   * 更新所有改动的子任务文件名
   */
  private void updateSingleSubFileName() {
    List<DTaskWrapper> entities = getTaskWrapper().getSubTaskWrapper();
    int i = 0;
    for (DTaskWrapper taskWrapper : entities) {
      if (i < mSubNameTemp.size()) {
        String newName = mSubNameTemp.get(i);
        DownloadEntity entity = taskWrapper.getEntity();
        if (!newName.equals(entity.getFileName())) {
          String oldPath = getEntity().getDirPath() + "/" + entity.getFileName();
          String newPath = getEntity().getDirPath() + "/" + newName;
          if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=? or isComplete='true'",
              newPath)) {
            ALog.w(TAG, String.format("更新文件名失败，路径【%s】已存在或文件已下载", newPath));
            return;
          }

          CommonUtil.modifyTaskRecord(oldPath, newPath);
          entity.setDownloadPath(newPath);
          entity.setFileName(newName);
        }
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

    Set<String> repeated = new HashSet<>();
    List<String> results = new ArrayList<>();
    for (String url : mUrls) {
      if (!repeated.add(url)) {
        results.add(url);
      }
    }
    if (!results.isEmpty()) {
      ALog.e(TAG, String.format("组合任务中有url重复，重复的url：%s", Arrays.toString(results.toArray())));
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

    getEntity().setGroupHash(CommonUtil.getMd5Code(mUrls));

    return true;
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
}
