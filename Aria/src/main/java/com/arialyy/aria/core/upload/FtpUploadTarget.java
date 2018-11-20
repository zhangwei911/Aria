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
package com.arialyy.aria.core.upload;

import android.support.annotation.CheckResult;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.command.normal.NormalCmdFactory;
import com.arialyy.aria.core.common.ftp.FTPSConfig;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.util.CommonUtil;
import java.net.Proxy;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp单任务上传
 */
public class FtpUploadTarget extends BaseNormalTarget<FtpUploadTarget>
    implements IFtpTarget<FtpUploadTarget> {
  private FtpDelegate<FtpUploadTarget> mDelegate;

  private String mAccount, mUser, mPw;
  private boolean needLogin = false;

  FtpUploadTarget(String filePath, String targetName) {
    this.mTargetName = targetName;
    initTask(filePath);
  }

  private void initTask(String filePath) {
    initTarget(filePath);
    mTaskEntity.setRequestType(AbsTaskEntity.U_FTP);
    mDelegate = new FtpDelegate<>(this);
  }

  /**
   * 添加任务
   */
  public void add() {
    if (checkEntity()) {
      AriaManager.getInstance(AriaManager.APP)
          .setCmd(CommonUtil.createNormalCmd(mTaskEntity, NormalCmdFactory.TASK_CREATE,
              checkTaskType()))
          .exe();
    }
  }

  @Override protected boolean checkUrl() {
    boolean b = super.checkUrl();
    if (!b) {
      return false;
    }
    FtpUrlEntity temp = mTaskEntity.getUrlEntity();
    FtpUrlEntity newEntity = CommonUtil.getFtpUrlInfo(mTempUrl);
    if (temp != null) { //处理FTPS的信息
      newEntity.isFtps = temp.isFtps;
      newEntity.storePass = temp.storePass;
      newEntity.keyAlias = temp.keyAlias;
      newEntity.protocol = temp.protocol;
      newEntity.storePath = temp.storePath;
    }
    mTaskEntity.setUrlEntity(newEntity);
    mTaskEntity.getUrlEntity().account = mAccount;
    mTaskEntity.getUrlEntity().user = mUser;
    mTaskEntity.getUrlEntity().password = mPw;
    mTaskEntity.getUrlEntity().needLogin = needLogin;
    return true;
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSConfig#setStorePath(String)} 、{@link FTPSConfig#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSConfig<FtpUploadTarget> asFtps() {
    if (mTaskEntity.getUrlEntity() == null) {
      FtpUrlEntity urlEntity = new FtpUrlEntity();
      urlEntity.isFtps = true;
      mTaskEntity.setUrlEntity(urlEntity);
    }
    return new FTPSConfig<>(this);
  }

  @CheckResult
  @Override public FtpUploadTarget charSet(String charSet) {
    return mDelegate.charSet(charSet);
  }

  @Override public FtpUploadTarget login(String userName, String password) {
    needLogin = true;
    mUser = userName;
    mPw = password;
    return this;
  }

  @Override public FtpUploadTarget login(String userName, String password, String account) {
    needLogin = true;
    mUser = userName;
    mPw = password;
    mAccount = account;
    return this;
  }

  @Override public FtpUploadTarget setProxy(Proxy proxy) {
    return mDelegate.setProxy(proxy);
  }
}
