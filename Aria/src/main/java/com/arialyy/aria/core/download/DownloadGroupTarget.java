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
import android.support.annotation.NonNull;
import com.arialyy.aria.core.common.http.GetDelegate;
import com.arialyy.aria.core.common.http.HttpDelegate;
import com.arialyy.aria.core.common.http.PostDelegate;
import com.arialyy.aria.core.inf.IHttpFileLenAdapter;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.exception.ParamException;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Created by AriaL on 2017/6/29.
 * 下载任务组
 */
public class DownloadGroupTarget extends AbsDGTarget<DownloadGroupTarget> {
  private HttpDelegate<DownloadGroupTarget> mHttpDelegate;
  private HttpGroupConfigHandler mConfigHandler;

  DownloadGroupTarget(DownloadGroupEntity groupEntity, String targetName) {
    setTargetName(targetName);
    if (groupEntity.getUrls() != null && !groupEntity.getUrls().isEmpty()) {
      init(groupEntity.getUrls());
    } else {
      throw new ParamException("组合任务只任务下载地址为空");
    }
  }

  DownloadGroupTarget(List<String> urls, String targetName) {
    setTargetName(targetName);
    init(urls);
  }

  private void init(List<String> urls) {
    mConfigHandler = new HttpGroupConfigHandler(this,
        TaskWrapperManager.getInstance().getDGTaskWrapper(DGTaskWrapper.class, urls));
    mHttpDelegate = new HttpDelegate<>(this);
  }

  /**
   * Post处理
   */
  @CheckResult
  public PostDelegate asPost() {
    mHttpDelegate = new PostDelegate<>(this);
    return (PostDelegate) mHttpDelegate;
  }

  /**
   * get处理
   */
  @CheckResult
  public GetDelegate asGet() {
    mHttpDelegate = new GetDelegate<>(this);
    return (GetDelegate) mHttpDelegate;
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult
  public DownloadGroupTarget updateUrls(List<String> urls) {
    return mConfigHandler.updateUrls(urls);
  }

  /**
   * 任务组总任务大小，任务组是一个抽象的概念，没有真实的数据实体，任务组的大小是Aria动态获取子任务大小相加而得到的，
   * 如果你知道当前任务组总大小，你也可以调用该方法给任务组设置大小
   *
   * 为了更好的用户体验，组合任务最好设置文件大小，默认需要强制设置文件大小。如果无法获取到总长度，请调用{@link #unknownSize()}
   *
   * @param fileSize 任务组总大小
   */
  @CheckResult
  public DownloadGroupTarget setFileSize(long fileSize) {
    if (fileSize <= 0) {
      ALog.e(TAG, "文件大小不能小于 0");
      return this;
    }
    if (getEntity().getFileSize() <= 1 || getEntity().getFileSize() != fileSize) {
      getEntity().setFileSize(fileSize);
    }
    return this;
  }

  /**
   * 如果无法获取到组合任务到总长度，请调用该方法，
   * 请注意：
   * 1、如果组合任务到子任务数过多，请不要使用该标志位，否则Aria将需要消耗大量的时间获取组合任务的总长度，直到获取完成组合任务总长度后才会执行下载。
   * 2、如果你的知道组合任务的总长度，请使用{@link #setFileSize(long)}设置组合任务的长度。
   * 3、由于网络或其它原因的存在，这种方式获取的组合任务大小有可能是不准确的。
   */
  @CheckResult
  public DownloadGroupTarget unknownSize() {
    getTaskWrapper().setUnknownSize(true);
    return this;
  }

  /**
   * 如果你是使用{@link DownloadReceiver#load(DownloadGroupEntity)}进行下载操作，那么你需要设置任务组的下载地址
   */
  @CheckResult
  public DownloadGroupTarget setGroupUrl(List<String> urls) {
    return mConfigHandler.setGroupUrl(urls);
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   *
   * @deprecated {@link #setSubFileName(List)} 请使用该api
   */
  @CheckResult
  @Deprecated public DownloadGroupTarget setSubTaskFileName(List<String> subTaskFileName) {
    return setSubFileName(subTaskFileName);
  }

  /**
   * 设置任务组的文件夹路径，在Aria中，任务组的所有子任务都会下载到以任务组组名的文件夹中。
   * 如：groupDirPath = "/mnt/sdcard/download/group_test"
   * <pre>
   *   {@code
   *      + mnt
   *        + sdcard
   *          + download
   *            + group_test
   *              - task1.apk
   *              - task2.apk
   *              - task3.apk
   *              ....
   *
   *   }
   * </pre>
   *
   * @param dirPath 任务组保存文件夹路径
   */
  @CheckResult
  public DownloadGroupTarget setDirPath(String dirPath) {
    return mConfigHandler.setDirPath(dirPath);
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   */
  @CheckResult
  public DownloadGroupTarget setSubFileName(List<String> subTaskFileName) {
    return mConfigHandler.setSubFileName(subTaskFileName);
  }

  /**
   * 如果你需要使用header中特定的key来设置文件长度，或有定制文件长度的需要，那么你可以通过该方法自行处理文件长度
   */
  public DownloadGroupTarget setFileLenAdapter(IHttpFileLenAdapter adapter) {
    return mHttpDelegate.setFileLenAdapter(adapter);
  }

  @Override public int getTargetType() {
    return GROUP_HTTP;
  }

  @Override protected boolean checkEntity() {
    return mConfigHandler.checkEntity();
  }

  @Override public boolean isRunning() {
    return mConfigHandler.isRunning();
  }

  @Override public boolean taskExists() {
    return mConfigHandler.taskExists();
  }

  @CheckResult
  public DownloadGroupTarget addHeader(@NonNull String key, @NonNull String value) {
    return mHttpDelegate.addHeader(key, value);
  }

  @CheckResult
  public DownloadGroupTarget addHeaders(Map<String, String> headers) {
    return mHttpDelegate.addHeaders(headers);
  }

  @CheckResult
  public DownloadGroupTarget setUrlProxy(Proxy proxy) {
    return mHttpDelegate.setUrlProxy(proxy);
  }
}
