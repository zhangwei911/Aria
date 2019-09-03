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
import aria.apache.commons.net.ftp.FTPClientConfig;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;

/**
 * Created by laoyuyu on 2018/3/9.
 */
public class FtpDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {
  private static final String TAG = "FtpDelegate";

  public FtpDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);
  }

  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FtpDelegate<TARGET> charSet(String charSet) {
    if (TextUtils.isEmpty(charSet)) {
      throw new NullPointerException("字符编码为空");
    }
    getTaskWrapper().asFtp().setCharSet(charSet);
    return this;
  }

  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FtpDelegate<TARGET> login(String userName, String password) {
    return login(userName, password, null);
  }

  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FtpDelegate<TARGET> login(String userName, String password, String account) {
    if (TextUtils.isEmpty(userName)) {
      ALog.e(TAG, "用户名不能为null");
      return this;
    } else if (TextUtils.isEmpty(password)) {
      ALog.e(TAG, "密码不能为null");
      return this;
    }
    // urlEntity 不能在构造函数中获取，因为ftp上传时url是后于构造函数的
    FtpUrlEntity urlEntity = getTaskWrapper().asFtp().getUrlEntity();
    urlEntity.needLogin = true;
    urlEntity.user = userName;
    urlEntity.password = password;
    urlEntity.account = account;
    return this;
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSDelegate#setStorePath(String)} 、{@link FTPSDelegate#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FTPSDelegate<TARGET> asFtps() {
    return new FTPSDelegate<>(mTarget, mWrapper);
  }

  /**
   * 配置ftp客户端信息
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public FtpDelegate<TARGET> setFtpClentConfig(FTPClientConfig config) {
    getTaskWrapper().asFtp().setClientConfig(config);
    return this;
  }

  //@Override public TARGET setProxy(Proxy proxy) {
  //  mTarget.getTaskWrapper().asFtp().setProxy(proxy);
  //  return mTarget;
  //}
}
