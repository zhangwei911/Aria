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
package com.arialyy.aria.core.download.m3u8;

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.common.http.HttpTaskConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.downloader.ConnectionHelp;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.FileUtil;
import com.arialyy.aria.util.Regular;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析url中获取到到m3u8文件信息
 * https://www.cnblogs.com/renhui/p/10351870.html
 * https://blog.csdn.net/Guofengpu/article/details/54922865
 */
final class M3U8InfoThread implements Runnable {
  public static final String M3U8_INDEX_FORMAT = "%s.index";
  private final String TAG = "M3U8InfoThread";
  private DownloadEntity mEntity;
  private DTaskWrapper mTaskWrapper;
  private int mConnectTimeOut;
  private OnFileInfoCallback onFileInfoCallback;
  private OnGetLivePeerCallback onGetPeerCallback;
  private HttpTaskConfig mTaskDelegate;
  /**
   * 是否停止获取切片信息，{@code true}停止获取切片信息
   */
  private boolean isStop = false;
  /**
   * m3u8文件信息
   */
  private List<String> mInfos = new ArrayList<>();

  interface OnGetLivePeerCallback {
    void onGetPeer(String url);
  }

  M3U8InfoThread(DTaskWrapper taskWrapper, OnFileInfoCallback callback) {
    this.mTaskWrapper = taskWrapper;
    mEntity = taskWrapper.getEntity();
    mConnectTimeOut =
        AriaManager.getInstance().getDownloadConfig().getConnectTimeOut();
    onFileInfoCallback = callback;
    mTaskDelegate = taskWrapper.asHttp();
    mEntity.getM3U8Entity().setLive(mTaskWrapper.getRequestType() == AbsTaskWrapper.M3U8_LIVE);
  }

  @Override public void run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    TrafficStats.setThreadStatsTag(UUID.randomUUID().toString().hashCode());
    HttpURLConnection conn = null;
    try {
      URL url = ConnectionHelp.handleUrl(mEntity.getUrl(), mTaskDelegate);
      conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
      ConnectionHelp.setConnectParam(mTaskDelegate, conn);
      conn.setConnectTimeout(mConnectTimeOut);
      conn.connect();
      handleConnect(conn);
    } catch (IOException e) {
      failDownload(e.getMessage(), false);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private void handleConnect(HttpURLConnection conn) throws IOException {
    int code = conn.getResponseCode();
    if (code == HttpURLConnection.HTTP_OK) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line = reader.readLine();
      if (TextUtils.isEmpty(line) || !line.equalsIgnoreCase("#EXTM3U")) {
        failDownload("读取M3U8信息失败，读取不到#EXTM3U标签", false);
        return;
      }
      List<String> extInf = new ArrayList<>();
      boolean isLive = mTaskWrapper.getRequestType() == ITaskWrapper.M3U8_LIVE;
      boolean isGenerateIndexFile = mTaskWrapper.getEntity().getM3U8Entity().isGenerateIndexFile();
      if (isGenerateIndexFile) {
        mInfos.add(line);
      }
      while ((line = reader.readLine()) != null) {
        if (isStop) {
          break;
        }
        if (isGenerateIndexFile) {
          mInfos.add(line);
        }
        if (line.startsWith("#EXT-X-ENDLIST")) {
          break;
        }
        //ALog.d(TAG, line);
        if (line.startsWith("#EXTINF")) {
          String info = reader.readLine();
          mInfos.add(info);
          if (isLive) {
            if (onGetPeerCallback != null) {
              onGetPeerCallback.onGetPeer(info);
            }
          } else {
            extInf.add(info);
          }
        } else if (line.startsWith("#EXT-X-STREAM-INF")) {
          int setBand = mTaskWrapper.asM3U8().getBandWidth();
          int bandWidth = getBandWidth(line);
          // 多码率的m3u8配置文件，清空信息
          if (isGenerateIndexFile && mInfos != null) {
            mInfos.clear();
          }
          if (setBand == 0) {
            handleBandWidth(conn, reader.readLine());
          } else if (bandWidth == setBand) {
            handleBandWidth(conn, reader.readLine());
          } else {
            failDownload(String.format("【%s】码率不存在", bandWidth), false);
          }
          return;
        } else if (line.startsWith("EXT-X-KEY")) {
          getKeyInfo(line);
        }
      }

      if (!isLive && extInf.isEmpty()) {
        failDownload(String.format("获取M3U8下载地址列表失败，url: %s", mEntity.getUrl()), false);
        return;
      }
      if (!isLive && mEntity.getM3U8Entity().getPeerNum() == 0) {
        mEntity.getM3U8Entity().setPeerNum(extInf.size());
        mEntity.getM3U8Entity().update();
      }
      CompleteInfo info = new CompleteInfo();
      info.obj = extInf;
      generateIndexFile();
      onFileInfoCallback.onComplete(mEntity.getKey(), info);
    } else if (code == HttpURLConnection.HTTP_MOVED_TEMP
        || code == HttpURLConnection.HTTP_MOVED_PERM
        || code == HttpURLConnection.HTTP_SEE_OTHER
        || code == HttpURLConnection.HTTP_CREATED // 201 跳转
        || code == 307) {
      handleUrlReTurn(conn, conn.getHeaderField("Location"));
    } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
      failDownload("404错误", false);
    } else {
      failDownload(String.format("不支持的响应，code: %s", code), true);
    }
  }

  /**
   * 创建索引文件
   */
  private void generateIndexFile() {
    if (mTaskWrapper.getEntity().getM3U8Entity().isGenerateIndexFile()) {

      String indexPath = String.format(M3U8_INDEX_FORMAT, mEntity.getFilePath());
      File indexFile = new File(indexPath);
      if (indexFile.exists()) {
        FileUtil.deleteFile(indexPath);
      }
      FileUtil.createFile(indexPath);

      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(indexFile);
        for (String str : mInfos) {
          byte[] by = str.concat("\r\n").getBytes(Charset.forName("UTF-8"));
          fos.write(by, 0, by.length);
        }
        fos.flush();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (fos != null) {
          try {
            fos.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * 是否停止获取切片信息，{@code true}停止获取切片信息
   */
  public void setStop(boolean isStop) {
    this.isStop = isStop;
  }

  /**
   * 直播切片信息获取回调
   */
  public void setOnGetPeerCallback(OnGetLivePeerCallback peerCallback) {
    onGetPeerCallback = peerCallback;
  }

  /**
   * 获取加密的密钥信息
   */
  private void getKeyInfo(String line) {
    String temp = line.substring(line.indexOf(":") + 1);
    String[] params = temp.split(",");
    M3U8Entity m3U8Entity = mEntity.getM3U8Entity();
    for (String param : params) {
      if (param.startsWith("METHOD")) {
        m3U8Entity.method = param.split("=")[1];
      } else if (param.startsWith("URI")) {
        m3U8Entity.keyUrl = param.split("=")[1].replaceAll("\"", "");
        m3U8Entity.keyPath = new File(mEntity.getFilePath()).getParent() + "/" + CommonUtil.getStrMd5(
            m3U8Entity.keyUrl) + ".key";
      } else if (param.startsWith("IV")) {
        m3U8Entity.iv = param.split("=")[1];
      }
    }
    downloadKey(m3U8Entity);
  }

  /**
   * 读取bandwidth
   */
  private int getBandWidth(String line) {
    Pattern p = Pattern.compile(Regular.BANDWIDTH);
    Matcher m = p.matcher(line);
    if (m.find()) {
      return Integer.parseInt(m.group());
    }
    return 0;
  }

  /**
   * 处理30x跳转
   */
  private void handleUrlReTurn(HttpURLConnection conn, String newUrl) throws IOException {
    ALog.d(TAG, "30x跳转，新url为【" + newUrl + "】");
    if (TextUtils.isEmpty(newUrl) || newUrl.equalsIgnoreCase("null")) {
      if (onFileInfoCallback != null) {
        onFileInfoCallback.onFail(mEntity, new TaskException(TAG, "获取重定向链接失败"), false);
      }
      return;
    }

    if (newUrl.startsWith("/")) {
      Uri uri = Uri.parse(mEntity.getUrl());
      newUrl = uri.getHost() + newUrl;
    }

    if (!CheckUtil.checkUrlNotThrow(newUrl)) {
      failDownload("下载失败，重定向url错误", false);
      return;
    }
    mTaskDelegate.setRedirectUrl(newUrl);
    mEntity.setRedirect(true);
    mEntity.setRedirectUrl(newUrl);
    String cookies = conn.getHeaderField("Set-Cookie");
    conn.disconnect(); // 关闭上一个连接
    URL url = ConnectionHelp.handleUrl(newUrl, mTaskDelegate);
    conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
    ConnectionHelp.setConnectParam(mTaskDelegate, conn);
    conn.setRequestProperty("Cookie", cookies);
    conn.setConnectTimeout(mConnectTimeOut);
    conn.connect();
    handleConnect(conn);
    conn.disconnect();
  }

  /**
   * 处理码率
   */
  private void handleBandWidth(HttpURLConnection conn, String bandWidthM3u8Url) throws IOException {
    IBandWidthUrlConverter converter = mTaskWrapper.asM3U8().getBandWidthUrlConverter();
    if (converter != null) {
      bandWidthM3u8Url = converter.convert(bandWidthM3u8Url);
      if (!bandWidthM3u8Url.startsWith("http")) {
        failDownload(String.format("码率转换器转换后的url地址无效，转换后的url：%s", bandWidthM3u8Url), false);
        return;
      }
    } else {
      ALog.d(TAG, "没有设置码率转换器");
    }
    mTaskWrapper.asM3U8().setBandWidthUrl(bandWidthM3u8Url);
    ALog.d(TAG, String.format("新码率url：%s", bandWidthM3u8Url));
    String cookies = conn.getHeaderField("Set-Cookie");
    conn.disconnect();    // 关闭上一个连接
    URL url = ConnectionHelp.handleUrl(bandWidthM3u8Url, mTaskDelegate);
    conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
    ConnectionHelp.setConnectParam(mTaskDelegate, conn);
    conn.setRequestProperty("Cookie", cookies);
    conn.setConnectTimeout(mConnectTimeOut);
    conn.connect();
    handleConnect(conn);
    conn.disconnect();
  }

  private void failDownload(String errorInfo, boolean needRetry) {
    onFileInfoCallback.onFail(mEntity, new M3U8Exception(TAG, errorInfo), needRetry);
  }

  /**
   * 密钥不存在，下载密钥
   */
  private void downloadKey(M3U8Entity info) {
    HttpURLConnection conn = null;
    FileOutputStream fos = null;
    try {
      File keyF = new File(info.keyPath);
      if (!keyF.exists()) {
        ALog.d(TAG, "密钥不存在，下载密钥");
        FileUtil.createFile(keyF.getPath());
      } else {
        return;
      }
      URL url = ConnectionHelp.handleUrl(info.keyUrl, mTaskDelegate);
      conn = ConnectionHelp.handleConnection(url, mTaskDelegate);
      ConnectionHelp.setConnectParam(mTaskDelegate, conn);
      conn.setConnectTimeout(mConnectTimeOut);
      conn.connect();
      InputStream is = conn.getInputStream();
      fos = new FileOutputStream(keyF);
      byte[] buffer = new byte[1024];
      int len;
      while ((len = is.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
        if (conn != null) {
          conn.disconnect();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
