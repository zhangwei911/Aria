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
import com.arialyy.aria.core.common.ftp.FTPSConfig;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.net.Proxy;

/**
 * Created by lyy on 2016/12/5.
 * https://github.com/AriaLyy/Aria
 */
public class FtpDownloadTarget extends BaseNormalTarget<FtpDownloadTarget>
    implements IFtpTarget<FtpDownloadTarget> {
  private FtpDelegate<FtpDownloadTarget> mDelegate;

  public FtpDownloadTarget(DownloadEntity entity, String targetName) {
    this(entity.getUrl(), targetName);
  }

  FtpDownloadTarget(String url, String targetName) {
    initTarget(url, targetName);
    init();
  }

  private void init() {
    int lastIndex = url.lastIndexOf("/");
    mEntity.setFileName(url.substring(lastIndex + 1, url.length()));
    mTaskEntity.setUrlEntity(CommonUtil.getFtpUrlInfo(url));
    mTaskEntity.setRequestType(AbsTaskEntity.D_FTP);

    mDelegate = new FtpDelegate<>(this);
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSConfig#setStorePath(String)} 、{@link FTPSConfig#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSConfig<FtpDownloadTarget> asFtps() {
    mTaskEntity.getUrlEntity().isFtps = true;
    return new FTPSConfig<>(this);
  }

  @Override protected boolean checkEntity() {
    if (mTaskEntity.getUrlEntity().isFtps){
      if (TextUtils.isEmpty(mTaskEntity.getUrlEntity().storePath)){
        ALog.e(TAG, "证书路径为空");
        return false;
      }
      if (TextUtils.isEmpty(mTaskEntity.getUrlEntity().keyAlias)){
        ALog.e(TAG, "证书别名为空");
        return false;
      }
    }
    return super.checkEntity();
  }

  /**
   * 设置文件保存文件夹路径
   *
   * @param filePath 文件保存路径
   * @deprecated {@link #setFilePath(String)} 请使用这个api
   */
  @Deprecated
  @CheckResult
  public FtpDownloadTarget setDownloadPath(@NonNull String filePath) {
    return setFilePath(filePath);
  }

  /**
   * 设置文件保存文件夹路径
   * 关于文件名：
   * 1、如果保存路径是该文件的保存路径，如：/mnt/sdcard/file.zip，则使用路径中的文件名file.zip
   * 2、如果保存路径是文件夹路径，如：/mnt/sdcard/，则使用FTP服务器该文件的文件名
   */
  @CheckResult
  public FtpDownloadTarget setFilePath(@NonNull String filePath) {
    mTempFilePath = filePath;
    return this;
  }

  @Override protected int getTargetType() {
    return FTP;
  }

  @CheckResult
  @Override public FtpDownloadTarget charSet(String charSet) {
    return mDelegate.charSet(charSet);
  }

  @CheckResult
  @Override public FtpDownloadTarget login(String userName, String password) {
    return mDelegate.login(userName, password);
  }

  @CheckResult
  @Override public FtpDownloadTarget login(String userName, String password, String account) {
    return mDelegate.login(userName, password, account);
  }

  @CheckResult
  @Override public FtpDownloadTarget setProxy(Proxy proxy) {
    return mDelegate.setProxy(proxy);
  }
}
