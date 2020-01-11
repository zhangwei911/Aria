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
package com.arialyy.aria.sftp;

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.IdEntity;
import com.arialyy.aria.core.loader.IInfoTask;
import com.arialyy.aria.core.loader.ILoaderVisitor;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.ftp.FtpTaskOption;
import com.arialyy.aria.util.CommonUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * 进行登录，获取session，获取文件信息
 */
public abstract class AbsSFtpInfoTask<WP extends AbsTaskWrapper> implements IInfoTask {
  protected String TAG = CommonUtil.getClassName(this);
  protected Callback callback;
  private WP wrapper;
  private FtpTaskOption option;

  public AbsSFtpInfoTask(WP wp) {
    this.wrapper = wp;
    this.option = (FtpTaskOption) wrapper.getTaskOption();
  }

  protected abstract void getFileInfo(Session session)
      throws JSchException, UnsupportedEncodingException, SftpException;

  @Override public void run() {
    try {
      FtpUrlEntity entity = option.getUrlEntity();
      String key = CommonUtil.getStrMd5(entity.hostName + entity.port + entity.user);
      Session session = SFtpSessionManager.getInstance().getSession(key);
      if (session == null) {
        session = login(entity);
      }
      getFileInfo(session);
    } catch (JSchException e) {
      e.printStackTrace();
      fail(false, null);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      fail(false, null);
    } catch (SftpException e) {
      e.printStackTrace();
      fail(false, null);
    }
  }

  private Session login(FtpUrlEntity entity) throws JSchException, UnsupportedEncodingException {
    JSch jSch = new JSch();

    IdEntity idEntity = entity.idEntity;

    if (idEntity.prvKey != null) {
      if (idEntity.pubKey == null) {
        jSch.addIdentity(idEntity.prvKey,
            entity.password == null ? null : entity.password.getBytes("UTF-8"));
      } else {
        jSch.addIdentity(idEntity.prvKey, idEntity.pubKey,
            entity.password == null ? null : entity.password.getBytes("UTF-8"));
      }
    }

    Session session;
    if (TextUtils.isEmpty(entity.user)) {
      session = jSch.getSession(entity.url, entity.hostName, Integer.parseInt(entity.port));
    } else {
      session = jSch.getSession(entity.hostName);
    }
    if (!TextUtils.isEmpty(entity.password)) {
      session.setPassword(entity.password);
    }
    Properties config = new Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);// 为Session对象设置properties
    session.setTimeout(3000);// 设置超时
    session.setIdentityRepository(jSch.getIdentityRepository());
    session.connect();
    return session;
  }

  protected FtpTaskOption getOption() {
    return option;
  }

  protected WP getWrapper() {
    return wrapper;
  }

  protected void fail(boolean needRetry, BaseException e) {
    callback.onFail(getWrapper().getEntity(), e, needRetry);
  }

  @Override public void setCallback(Callback callback) {
    this.callback = callback;
  }

  @Override public void accept(ILoaderVisitor visitor) {
    visitor.addComponent(this);
  }
}
