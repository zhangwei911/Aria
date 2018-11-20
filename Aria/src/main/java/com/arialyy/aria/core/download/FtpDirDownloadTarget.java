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
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.ftp.FTPSConfig;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.core.manager.TEManager;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;

/**
 * Created by Aria.Lao on 2017/7/26.
 * ftp文件夹下载
 */
public class FtpDirDownloadTarget extends BaseGroupTarget<FtpDirDownloadTarget>
    implements IFtpTarget<FtpDirDownloadTarget> {
  private FtpDelegate<FtpDirDownloadTarget> mDelegate;

  FtpDirDownloadTarget(String url, String targetName) {
    mTargetName = targetName;
    init(url);
  }

  private void init(String key) {
    mGroupName = key;
    mTaskEntity = TEManager.getInstance().getFDTEntity(DownloadGroupTaskEntity.class, key);
    mTaskEntity.setRequestType(AbsTaskEntity.D_FTP_DIR);
    mEntity = mTaskEntity.getEntity();
    if (mEntity != null) {
      mDirPathTemp = mEntity.getDirPath();
    }
    mDelegate = new FtpDelegate<>(this);
  }

  @Override protected int getTargetType() {
    return GROUP_FTP_DIR;
  }

  @Override protected boolean checkEntity() {
    boolean b = getTargetType() == GROUP_FTP_DIR && checkDirPath() && checkUrl();
    if (b) {
      mEntity.save();
      mTaskEntity.save();
      if (mTaskEntity.getSubTaskEntities() != null) {
        //初始化子项的登录信息
        FtpUrlEntity tUrlEntity = mTaskEntity.getUrlEntity();
        for (DownloadTaskEntity entity : mTaskEntity.getSubTaskEntities()) {
          FtpUrlEntity urlEntity = entity.getUrlEntity();
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
    if (mTaskEntity.getUrlEntity().isFtps) {
      if (TextUtils.isEmpty(mTaskEntity.getUrlEntity().storePath)) {
        ALog.e(TAG, "证书路径为空");
        return false;
      }
      if (TextUtils.isEmpty(mTaskEntity.getUrlEntity().keyAlias)) {
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
    final String url = mGroupName;
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

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSConfig#setStorePath(String)} 、{@link FTPSConfig#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSConfig<FtpDirDownloadTarget> asFtps() {
    mTaskEntity.getUrlEntity().isFtps = true;
    return new FTPSConfig<>(this);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget charSet(String charSet) {
    return mDelegate.charSet(charSet);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget login(String userName, String password) {
    return mDelegate.login(userName, password);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget login(String userName, String password, String account) {
    return mDelegate.login(userName, password, account);
  }

  @Override public FtpDirDownloadTarget setProxy(Proxy proxy) {
    return mDelegate.setProxy(proxy);
  }
}
