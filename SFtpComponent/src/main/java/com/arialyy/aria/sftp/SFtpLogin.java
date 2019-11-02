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
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.util.Properties;

/**
 * sftp登录
 *
 * @author lyy
 */
public class SFtpLogin {

  private String ip, userName, password;
  private int port;
  private Session session;
  private boolean isLogin = false;

  private SFtpLogin() {
    createClient();
  }

  /**
   * 创建客户端
   */
  private void createClient() {
    JSch jSch = new JSch();
    try {
      if (TextUtils.isEmpty(userName)) {
        session = jSch.getSession(userName, ip, port);
      } else {
        session = jSch.getSession(ip);
      }
      if (!TextUtils.isEmpty(password)) {
        session.setPassword(password);
      }
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);// 为Session对象设置properties
      session.setTimeout(3000);// 设置超时
      isLogin = true;
    } catch (JSchException e) {
      e.printStackTrace();
    }
  }

  /**
   * 执行登录
   */
  public Session login() {
    try {
      session.connect(); // 通过Session建立连接
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return session;
  }

  /**
   * 登出
   */
  public void logout() {
    if (session != null) {
      session.disconnect();
    }
    isLogin = false;
  }

  public static class Builder {
    private String ip, userName, password;
    private int port = 22;

    public Builder setIp(String ip) {
      this.ip = ip;
      return this;
    }

    public Builder setUserName(String userName) {
      this.userName = userName;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public SFtpLogin build() {
      SFtpLogin login = new SFtpLogin();
      login.ip = ip;
      login.userName = userName;
      login.password = password;
      login.port = port;
      if (TextUtils.isEmpty(ip)) {
        throw new IllegalArgumentException("ip不能为空");
      }
      if (port < 0 || port > 65534) {
        throw new IllegalArgumentException("端口错误");
      }
      return login;
    }
  }
}
