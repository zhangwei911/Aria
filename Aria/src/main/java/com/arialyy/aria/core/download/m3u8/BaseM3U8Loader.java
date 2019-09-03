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

import com.arialyy.aria.core.common.AbsFileer;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.FileUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public abstract class BaseM3U8Loader extends AbsFileer<DownloadEntity, DTaskWrapper> {

  BaseM3U8Loader(IEventListener listener, DTaskWrapper wrapper) {
    super(listener, wrapper);
    mTempFile = new File(wrapper.getEntity().getFilePath());
  }

  @Override protected long delayTimer() {
    return 1000;
  }

  /**
   * 获取ts文件保存路径
   *
   * @param dirCache 缓存目录
   * @param threadId ts文件名
   */
  public static String getTsFilePath(String dirCache, int threadId) {
    return String.format("%s/%s.ts", dirCache, threadId);
  }

  String getCacheDir() {
    String cacheDir = mTaskWrapper.asM3U8().getCacheDir();
    if (!new File(cacheDir).exists()) {
      FileUtil.createDir(cacheDir);
    }
    return cacheDir;
  }

  /**
   * 创建索引文件
   */
  boolean generateIndexFile() {
    File tempFile = new File(M3U8InfoThread.M3U8_INDEX_FORMAT, getEntity().getFilePath());
    if (!tempFile.exists()) {
      ALog.e(TAG, "源索引文件不存在");
      return false;
    }
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try {
      String cacheDir = getCacheDir();
      fis = new FileInputStream(tempFile);
      fos = new FileOutputStream(getEntity().getFilePath());
      BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
      String line;
      int i = 0;
      while ((line = reader.readLine()) != null) {
        byte[] bytes;
        if (line.startsWith("EXTINF")) {
          String tsPath = getTsFilePath(cacheDir, mRecord.threadRecords.get(i).threadId);
          bytes = tsPath.concat("\r\n").getBytes(Charset.forName("UTF-8"));
          i++;
        } else if (line.startsWith("EXT-X-KEY")) {
          M3U8Entity m3U8Entity = getEntity().getM3U8Entity();
          String keyInfo = String.format("#EXT-X-KEY:METHOD=%s,URI=%s,IV=%s\r\n", m3U8Entity.method,
              m3U8Entity.keyPath, m3U8Entity.iv);
          bytes = keyInfo.getBytes(Charset.forName("UTF-8"));
        } else {
          bytes = line.getBytes(Charset.forName("UTF-8"));
        }
        fos.write(bytes, 0, bytes.length);
      }
      fos.flush();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
        if (fos != null) {
          fos.close();
        }
        if (tempFile.exists()) {
          FileUtil.deleteFile(tempFile);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return false;
  }
}
