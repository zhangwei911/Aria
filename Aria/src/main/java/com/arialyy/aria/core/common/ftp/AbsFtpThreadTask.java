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
import aria.apache.commons.net.ftp.FTP;
import aria.apache.commons.net.ftp.FTPClient;
import aria.apache.commons.net.ftp.FTPReply;
import aria.apache.commons.net.ftp.FTPSClient;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.AbsThreadTask;
import com.arialyy.aria.core.common.ProtocolType;
import com.arialyy.aria.core.common.StateConstance;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskEntity;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.SSLContextUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.net.ssl.SSLContext;

/**
 * Created by lyy on 2017/9/26.
 * FTP单任务父类
 */
public abstract class AbsFtpThreadTask<ENTITY extends AbsNormalEntity, TASK_ENTITY extends AbsTaskEntity<ENTITY>>
    extends AbsThreadTask<ENTITY, TASK_ENTITY> {
  private final String TAG = "AbsFtpThreadTask";
  protected String charSet;
  /**
   * D_FTP 服务器编码
   */
  public static String SERVER_CHARSET = "ISO-8859-1";

  protected AbsFtpThreadTask(StateConstance constance, IEventListener listener,
      SubThreadConfig<TASK_ENTITY> info) {
    super(constance, listener, info);
  }

  /**
   * 构建FTP客户端
   */
  protected FTPClient createClient() {
    FTPClient client = null;
    final FtpUrlEntity urlEntity = mTaskEntity.getUrlEntity();
    if (urlEntity.validAddr == null) {
      try {
        InetAddress[] ips = InetAddress.getAllByName(urlEntity.hostName);
        client = connect(newInstanceClient(urlEntity), ips, 0, Integer.parseInt(urlEntity.port));
        if (client == null) {
          return null;
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    } else {
      client = newInstanceClient(urlEntity);
      try {
        client.connect(urlEntity.validAddr, Integer.parseInt(urlEntity.port));
      } catch (java.io.IOException e) {
        ALog.e(TAG, ALog.getExceptionString(e));
        return null;
      }
    }

    if (client == null) {
      return null;
    }

    try {
      if (urlEntity.isFtps) {
        int code = ((FTPSClient) client).execAUTH(
            TextUtils.isEmpty(urlEntity.protocol) ? ProtocolType.TLS : urlEntity.protocol);
        ALog.d(TAG, String.format("cod：%s，msg：%s", code, client.getReplyString()));
      }

      if (urlEntity.needLogin) {
        if (TextUtils.isEmpty(urlEntity.account)) {
          client.login(urlEntity.user, urlEntity.password);
        } else {
          client.login(urlEntity.user, urlEntity.password, urlEntity.account);
        }
      }
      int reply = client.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        client.disconnect();
        fail(mChildCurrentLocation, new AriaIOException(TAG,
            String.format("无法连接到ftp服务器，错误码为：%s，msg:%s", reply, client.getReplyString())), false);
        return null;
      }
      // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码
      charSet = "UTF-8";
      if (reply != FTPReply.COMMAND_IS_SUPERFLUOUS) {
        if (!TextUtils.isEmpty(mTaskEntity.getCharSet())) {
          charSet = mTaskEntity.getCharSet();
        }
      }
      client.setControlEncoding(charSet);
      client.setDataTimeout(mReadTimeOut);
      client.setConnectTimeout(mConnectTimeOut);
      client.enterLocalPassiveMode();
      client.setFileType(FTP.BINARY_FILE_TYPE);
      client.setControlKeepAliveTimeout(5000);
      if (mTaskEntity.getUrlEntity().isFtps) {
        ((FTPSClient) client).execPROT("P");
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    return client;
  }

  /**
   * 创建FTP/FTPS客户端
   */
  private FTPClient newInstanceClient(FtpUrlEntity urlEntity) {
    FTPClient temp;
    if (urlEntity.isFtps) {
      SSLContext sslContext =
          SSLContextUtil.getSSLContext(urlEntity.keyAlias, urlEntity.storePath, urlEntity.protocol);
      if (sslContext == null) {
        sslContext = SSLContextUtil.getDefaultSLLContext(urlEntity.protocol);
      }
      temp = new FTPSClient(true, sslContext);
    } else {
      temp = new FTPClient();
    }

    return temp;
  }

  /**
   * 连接到ftp服务器
   */
  private FTPClient connect(FTPClient client, InetAddress[] ips, int index, int port) {
    try {
      client.connect(ips[index], port);
      mTaskEntity.getUrlEntity().validAddr = ips[index];
      return client;
    } catch (java.io.IOException e) {
      try {
        if (client.isConnected()) {
          client.disconnect();
        }
      } catch (java.io.IOException e1) {
        e1.printStackTrace();
      }
      if (index + 1 >= ips.length) {
        ALog.w(TAG, "遇到[ECONNREFUSED-连接被服务器拒绝]错误，已没有其他地址，链接失败");
        return null;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      ALog.w(TAG, "遇到[ECONNREFUSED-连接被服务器拒绝]错误，正在尝试下一个地址");
      return connect(new FTPClient(), ips, index + 1, port);
    }
  }
}
