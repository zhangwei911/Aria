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
import android.os.Process;
import android.text.TextUtils;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.common.http.HttpTaskConfig;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.downloader.ConnectionHelp;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 解析url中获取到到m3u信息
 * https://www.cnblogs.com/renhui/p/10351870.html
 */
public class M3U8FileInfoThread implements Runnable {
  private final String TAG = "M3U8FileInfoThread";
  private DownloadEntity mEntity;
  private DTaskWrapper mTaskWrapper;
  private int mConnectTimeOut;
  private OnFileInfoCallback onFileInfoCallback;
  private HttpTaskConfig mTaskDelegate;

  public M3U8FileInfoThread(DTaskWrapper taskWrapper, OnFileInfoCallback callback) {
    this.mTaskWrapper = taskWrapper;
    mEntity = taskWrapper.getEntity();
    mConnectTimeOut =
        AriaManager.getInstance(AriaManager.APP).getDownloadConfig().getConnectTimeOut();
    onFileInfoCallback = callback;
    mTaskDelegate = taskWrapper.asHttp();
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
      Map<String, List<String>> head = conn.getHeaderFields();
      handleConnect(conn);
    } catch (IOException e) {
      e.printStackTrace();
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
      boolean isDes = false;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#EXT-X-ENDLIST")) {
          break;
        }
        if (line.startsWith("#EXTINF")) {
          isDes = true;
        } else if (isDes) {
          extInf.add(line);
          isDes = false;
        }
      }
      if (extInf.isEmpty()) {
        failDownload("获取M3U8下载地址列表失败", false);
        return;
      }
      CompleteInfo info = new CompleteInfo();
      info.obj = extInf;
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
   * 处理30x跳转
   */
  private void handleUrlReTurn(HttpURLConnection conn, String newUrl) throws IOException {
    ALog.d(TAG, "30x跳转，新url为【" + newUrl + "】");
    if (TextUtils.isEmpty(newUrl) || newUrl.equalsIgnoreCase("null") || !newUrl.startsWith(
        "http")) {
      if (onFileInfoCallback != null) {
        onFileInfoCallback.onFail(mEntity, new TaskException(TAG, "获取重定向链接失败"), false);
      }
      return;
    }
    if (!CheckUtil.checkUrlNotThrow(newUrl)) {
      failDownload("下载失败，重定向url错误", false);
      return;
    }
    mTaskDelegate.setRedirectUrl(newUrl);
    mEntity.setRedirect(true);
    mEntity.setRedirectUrl(newUrl);
    String cookies = conn.getHeaderField("Set-Cookie");
    URL url = ConnectionHelp.handleUrl(newUrl, mTaskDelegate);
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
}
