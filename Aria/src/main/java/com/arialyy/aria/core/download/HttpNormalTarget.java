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

import android.support.annotation.CheckResult;
import com.arialyy.aria.core.common.AbsNormalTarget;
import com.arialyy.aria.core.common.Suggest;
import com.arialyy.aria.core.common.http.HttpDelegate;
import com.arialyy.aria.core.download.m3u8.M3U8Delegate;

/**
 * Created by lyy on 2016/12/5.
 * https://github.com/AriaLyy/Aria
 */
public class HttpNormalTarget extends AbsNormalTarget<HttpNormalTarget> {
  private DNormalConfigHandler<HttpNormalTarget> mConfigHandler;

  HttpNormalTarget(long taskId, String targetName) {
    mConfigHandler = new DNormalConfigHandler<>(this, taskId, targetName);
  }

  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public M3U8Delegate<HttpNormalTarget> asM3U8() {
    return new M3U8Delegate<>(this, getTaskWrapper());
  }

  /**
   * 设置http请求参数，header等信息
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public HttpDelegate<HttpNormalTarget> option() {
    return new HttpDelegate<>(this, getTaskWrapper());
  }

  /**
   * 更新文件保存路径
   * 如：原文件路径 /mnt/sdcard/test.zip
   * 如果需要将test.zip改为game.zip，只需要重新设置文件路径为：/mnt/sdcard/game.zip
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public HttpNormalTarget modifyFilePath(String filePath) {
    mConfigHandler.setTempFilePath(filePath);
    return this;
  }

  /**
   * 从header中获取文件描述信息
   */
  public String getContentDisposition() {
    return getEntity().getDisposition();
  }

  /**
   * 更新下载地址
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public HttpNormalTarget updateUrl(String newUrl) {
    return mConfigHandler.updateUrl(newUrl);
  }

  @Override public DownloadEntity getEntity() {
    return (DownloadEntity) super.getEntity();
  }

  @Override public boolean isRunning() {
    return mConfigHandler.isRunning();
  }

  @Override public boolean taskExists() {
    return mConfigHandler.taskExists();
  }
}
