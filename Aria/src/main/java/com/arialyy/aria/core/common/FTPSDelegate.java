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

import android.text.TextUtils;
import androidx.annotation.CheckResult;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.ProtocolType;
import com.arialyy.aria.core.inf.Suggest;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IOptionConstant;
import com.arialyy.aria.util.ALog;

/**
 * D_FTP SSL/TSL 参数委托
 */
public class FTPSDelegate<TARGET extends AbsTarget> extends BaseOption<TARGET> {

  private FtpUrlEntity mUrlEntity;

  public FTPSDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
    mUrlEntity = (FtpUrlEntity) getTaskWrapper().getOptionParams()
        .getParam(IOptionConstant.ftpUrlEntity);
    if (mUrlEntity != null) {
      mUrlEntity.isFtps = true;
    }
  }

  /**
   * 设置协议类型
   *
   * @param protocol {@link ProtocolType}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setProtocol(@ProtocolType String protocol) {
    if (TextUtils.isEmpty(protocol)) {
      ALog.e(TAG, "设置协议失败，协议信息为空");
      return this;
    }
    mUrlEntity.protocol = protocol;
    return this;
  }

  /**
   * 设置证书别名
   *
   * @param keyAlias 别名
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setAlias(String keyAlias) {
    if (TextUtils.isEmpty(keyAlias)) {
      ALog.e(TAG, "设置证书别名失败，证书别名为空");
      return this;
    }
    mUrlEntity.keyAlias = keyAlias;
    return this;
  }

  /**
   * 设置证书密码
   *
   * @param storePass 私钥密码
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setStorePass(String storePass) {
    if (TextUtils.isEmpty(storePass)) {
      ALog.e(TAG, "设置证书密码失败，证书密码为空");
      return this;
    }
    mUrlEntity.storePass = storePass;
    return this;
  }

  /**
   * 设置证书路径
   *
   * @param storePath 证书路径
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setStorePath(String storePath) {
    if (TextUtils.isEmpty(storePath)) {
      ALog.e(TAG, "设置证书路径失败，证书路径为空");
      return this;
    }
    mUrlEntity.storePath = storePath;
    return this;
  }

  /**
   * 设置安全模式，默认true
   *
   * @param isImplicit true 隐式，false 显式
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setImplicit(boolean isImplicit) {
    mUrlEntity.isImplicit = isImplicit;
    return this;
  }
}
