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
package com.arialyy.aria.core.download;

import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.config.DownloadConfig;
import com.arialyy.aria.core.download.m3u8.M3U8TaskConfig;
import com.arialyy.aria.core.inf.AbsTaskWrapper;

/**
 * Created by lyy on 2017/1/23. 下载任务实体和下载实体为一对一关系，下载实体删除，任务实体自动删除
 */
public class DTaskWrapper extends AbsTaskWrapper<DownloadEntity> {

  /**
   * 所属的任务组组名，如果不属于任务组，则为null
   */
  private String groupHash;

  /**
   * 该任务是否属于任务组
   */
  private boolean isGroupTask = false;

  private M3U8TaskConfig m3u8TaskConfig;

  /**
   * 文件下载url的临时保存变量
   */
  private String mTempUrl;
  /**
   * 文件保存路径的临时变量
   */
  private String mTempFilePath;

  /**
   * {@code true}强制下载，不考虑文件路径是否被占用
   */
  private boolean forceDownload = false;

  public DTaskWrapper(DownloadEntity entity) {
    super(entity);
  }

  public M3U8TaskConfig asM3U8() {
    if (m3u8TaskConfig == null) {
      m3u8TaskConfig = new M3U8TaskConfig();
    }
    return m3u8TaskConfig;
  }

  /**
   * Task实体对应的key，下载url
   */
  @Override public String getKey() {
    return getEntity().getKey();
  }

  @Override public DownloadConfig getConfig() {
    if (isGroupTask) {
      return Configuration.getInstance().dGroupCfg.getSubConfig();
    } else {
      return Configuration.getInstance().downloadCfg;
    }
  }

  public String getGroupHash() {
    return groupHash;
  }

  public boolean isGroupTask() {
    return isGroupTask;
  }

  public void setGroupHash(String groupHash) {
    this.groupHash = groupHash;
  }

  public void setGroupTask(boolean groupTask) {
    isGroupTask = groupTask;
  }

  public String getmTempUrl() {
    return mTempUrl;
  }

  public void setmTempUrl(String mTempUrl) {
    this.mTempUrl = mTempUrl;
  }

  public String getmTempFilePath() {
    return mTempFilePath;
  }

  public void setmTempFilePath(String mTempFilePath) {
    this.mTempFilePath = mTempFilePath;
  }

  public boolean isForceDownload() {
    return forceDownload;
  }

  public void setForceDownload(boolean forceDownload) {
    this.forceDownload = forceDownload;
  }
}
