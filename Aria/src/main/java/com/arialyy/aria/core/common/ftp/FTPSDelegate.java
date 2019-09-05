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
package com.arialyy.aria.core.common.ftp;

import android.text.TextUtils;
import androidx.annotation.CheckResult;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.common.ProtocolType;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * D_FTP SSL/TSL 参数委托
 */
public class FTPSDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {

  private FtpUrlEntity mUrlEntity;

  public FTPSDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
    mUrlEntity = getTaskWrapper().asFtp().getUrlEntity();
    mUrlEntity.isFtps = true;
  }

  /**
   * 设置协议类型
   *
   * @param protocol {@link ProtocolType}
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> setProtocol(@ProtocolType String protocol) {
    if (TextUtils.isEmpty(protocol)) {
      throw new NullPointerException("协议为空");
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
      throw new NullPointerException("别名为空");
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
      throw new NullPointerException("证书密码为空");
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
      throw new NullPointerException("证书路径为空");
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
