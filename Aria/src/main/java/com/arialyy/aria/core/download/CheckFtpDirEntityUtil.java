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

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import java.io.File;

public class CheckFtpDirEntityUtil {
  private final String TAG = "CheckFtpDirEntityUtil";
  private DGTaskWrapper mWrapper;
  private DownloadGroupEntity mEntity;

  public static CheckFtpDirEntityUtil newInstance(DGTaskWrapper wrapper) {
    return new CheckFtpDirEntityUtil(wrapper);
  }

  private CheckFtpDirEntityUtil(DGTaskWrapper wrapper) {
    mWrapper = wrapper;
    mEntity = mWrapper.getEntity();
  }

  /**
   * 检查并设置文件夹路径
   *
   * @return {@code true} 合法
   */
  private boolean checkDirPath() {
    if (TextUtils.isEmpty(mWrapper.getDirPathTemp())) {
      ALog.e(TAG, "文件夹路径不能为null");
      return false;
    } else if (!mWrapper.getDirPathTemp().startsWith("/")) {
      ALog.e(TAG, String.format("文件夹路径【%s】错误", mWrapper.getDirPathTemp()));
      return false;
    }
    File file = new File(mWrapper.getDirPathTemp());
    if (file.isFile()) {
      ALog.e(TAG, String.format("路径【%s】是文件，请设置文件夹路径", mWrapper.getDirPathTemp()));
      return false;
    }

    if ((mEntity.getDirPath() == null || !mEntity.getDirPath().equals(mWrapper.getDirPathTemp()))
        && DbEntity.checkDataExist(DownloadGroupEntity.class, "dirPath=?",
        mWrapper.getDirPathTemp())) {
      ALog.e(TAG, String.format("文件夹路径【%s】已被其它任务占用，请重新设置文件夹路径", mWrapper.getDirPathTemp()));
      return false;
    }

    if (TextUtils.isEmpty(mEntity.getDirPath()) || !mEntity.getDirPath()
        .equals(mWrapper.getDirPathTemp())) {
      if (!file.exists()) {
        file.mkdirs();
      }
      mEntity.setDirPath(mWrapper.getDirPathTemp());
      ALog.i(TAG, String.format("文件夹路径改变，将更新文件夹路径为：%s", mWrapper.getDirPathTemp()));
    }
    return true;
  }

  public boolean checkEntity() {
    boolean b = checkDirPath() && checkUrl();
    if (b) {
      mEntity.save();
      if (mWrapper.getSubTaskWrapper() != null) {
        //初始化子项的登录信息
        FtpUrlEntity tUrlEntity = mWrapper.asFtp().getUrlEntity();
        for (DTaskWrapper wrapper : mWrapper.getSubTaskWrapper()) {
          FtpUrlEntity urlEntity = wrapper.asFtp().getUrlEntity();
          urlEntity.needLogin = tUrlEntity.needLogin;
          urlEntity.account = tUrlEntity.account;
          urlEntity.user = tUrlEntity.user;
          urlEntity.password = tUrlEntity.password;
          // 处理ftps详细
          if (tUrlEntity.isFtps) {
            urlEntity.isFtps = true;
            urlEntity.protocol = tUrlEntity.protocol;
            urlEntity.storePath = tUrlEntity.storePath;
            urlEntity.storePass = tUrlEntity.storePass;
            urlEntity.keyAlias = tUrlEntity.keyAlias;
          }
        }
      }
    }
    if (mWrapper.asFtp().getUrlEntity().isFtps) {
      if (TextUtils.isEmpty(mWrapper.asFtp().getUrlEntity().storePath)) {
        ALog.e(TAG, "证书路径为空");
        return false;
      }
      if (TextUtils.isEmpty(mWrapper.asFtp().getUrlEntity().keyAlias)) {
        ALog.e(TAG, "证书别名为空");
        return false;
      }
    }
    return b;
  }

  /**
   * 检查普通任务的下载地址
   *
   * @return {@code true}地址合法
   */
  private boolean checkUrl() {
    final String url = mEntity.getKey();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "下载失败，url为null");
      return false;
    } else if (!url.startsWith("ftp")) {
      ALog.e(TAG, "下载失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "下载失败，url【" + url + "】不合法");
      return false;
    }
    return true;
  }
}
