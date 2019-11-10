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
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * sftp工具类
 *
 * @author lyy
 */
public class SFtpUtil {
  private final String TAG = CommonUtil.getClassName(getClass());
  /**
   * 用于执行命令
   */
  public static final String CMD_TYPE_EXEC = "exec";
  /**
   * 用于处理文件
   */
  public static final String CMD_TYPE_SFTP = "sftp";
  private String ip, userName, password;
  private int port;
  private Session session;
  private boolean isLogin = false;

  private SFtpUtil() {
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
      login();
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

  public Session getSession() {
    return session;
  }

  /**
   * 执行命令
   *
   * @param cmd sftp命令
   */
  public void execCommand(String cmd) {
    if (TextUtils.isEmpty(cmd)) {
      ALog.e(TAG, "命令为空");
      return;
    }
    if (!isLogin) {
      ALog.e(TAG, "没有登录");
      return;
    }
    ChannelExec channel = null;
    try {
      channel = (ChannelExec) session.openChannel(CMD_TYPE_EXEC);
      channel.setCommand(cmd);
      channel.connect();
      String rst = getResult(channel.getInputStream());

      ALog.i(TAG, String.format("result: %s", rst));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSchException e) {
      e.printStackTrace();
    } finally {
      if (channel != null) {
        channel.disconnect();
      }
    }
  }

  /**
   * 执行命令后，获取服务器端返回的数据
   *
   * @return 服务器端返回的数据
   */
  private String getResult(InputStream in) throws IOException {
    if (in == null){
      ALog.e(TAG, "输入流为空");
      return null;
    }
    StringBuilder sb = new StringBuilder();
    BufferedReader isr = new BufferedReader(new InputStreamReader(in));
    String line;
    while ((line = isr.readLine()) != null) {
      sb.append(line);
    }
    in.close();
    isr.close();

    return sb.toString();
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

    public SFtpUtil build() {
      SFtpUtil login = new SFtpUtil();
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
