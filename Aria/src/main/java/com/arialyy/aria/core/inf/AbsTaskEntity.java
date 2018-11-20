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
package com.arialyy.aria.core.inf;

import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.orm.annotation.Ignore;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lyy on 2017/2/23.
 * 所有任务实体的父类
 */
public abstract class AbsTaskEntity<ENTITY extends AbsEntity> extends DbEntity {
  /**
   * HTTP单任务载
   */
  public static final int D_HTTP = 0x11;
  /**
   * HTTP任务组下载
   */
  public static final int DG_HTTP = 0x12;

  /**
   * FTP单文件下载
   */
  public static final int D_FTP = 0x13;
  /**
   * FTP文件夹下载，为避免登录过多，子任务由单线程进行处理
   */
  public static final int D_FTP_DIR = 0x14;

  /**
   * HTTP单文件上传
   */
  public static final int U_HTTP = 0xA1;
  /**
   * FTP单文件上传
   */
  public static final int U_FTP = 0xA2;

  @Ignore private CookieManager cookieManager;

  /**
   * 账号和密码
   */
  @Ignore private FtpUrlEntity urlEntity;

  /**
   * 刷新信息 {@code true} 重新刷新下载信息
   */
  @Ignore private boolean refreshInfo = false;

  /**
   * 是否是新任务，{@code true} 新任务
   */
  @Ignore private boolean isNewTask = false;

  /**
   * 请求参数
   */
  @Ignore private Map<String, String> params;

  /**
   * 任务状态，和Entity的state同步
   */
  private int state = IEntity.STATE_WAIT;

  /**
   * 请求类型
   * {@link AbsTaskEntity#D_HTTP}、{@link AbsTaskEntity#D_FTP}、{@link AbsTaskEntity#D_FTP_DIR}。。。
   */
  private int requestType = D_HTTP;

  /**
   * http 请求头
   */
  private Map<String, String> headers = new HashMap<>();

  /**
   * 字符编码，默认为"utf-8"
   */
  private String charSet = "utf-8";

  /**
   * 网络请求类型
   */
  private RequestEnum requestEnum = RequestEnum.GET;

  /**
   * 是否使用服务器通过content-disposition传递的文件名，内容格式{@code attachment; filename="filename.jpg"}
   * {@code true} 使用
   */
  private boolean useServerFileName = false;

  /**
   * 重定向链接
   */
  private String redirectUrl = "";

  /**
   * 删除任务时，是否删除已下载完成的文件
   * 未完成的任务，不管true还是false，都会删除文件
   * {@code true}  删除任务数据库记录，并且删除已经下载完成的文件
   * {@code false} 如果任务已经完成，只删除任务数据库记录
   */
  @Ignore private boolean removeFile = false;

  /**
   * 是否支持断点, {@code true} 为支持断点
   */
  private boolean isSupportBP = true;

  /**
   * 状态码
   */
  private int code;

  @Ignore private Proxy proxy;

  public abstract ENTITY getEntity();

  public CookieManager getCookieManager() {
    return cookieManager;
  }

  public void setCookieManager(CookieManager cookieManager) {
    this.cookieManager = cookieManager;
  }

  /**
   * 获取任务下载状态
   *
   * @return {@link IEntity}
   */
  public int getState() {
    return getEntity().getState();
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public abstract String getKey();

  public abstract void setKey(String key);

  @Override public void update() {
    if (getEntity() != null) {
      getEntity().update();
    }
    super.update();
  }

  public FtpUrlEntity getUrlEntity() {
    return urlEntity;
  }

  public void setUrlEntity(FtpUrlEntity urlEntity) {
    this.urlEntity = urlEntity;
  }

  public boolean isRefreshInfo() {
    return refreshInfo;
  }

  public void setRefreshInfo(boolean refreshInfo) {
    this.refreshInfo = refreshInfo;
  }

  public boolean isNewTask() {
    return isNewTask;
  }

  public void setNewTask(boolean newTask) {
    isNewTask = newTask;
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getRequestType() {
    return requestType;
  }

  public void setRequestType(int requestType) {
    this.requestType = requestType;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getCharSet() {
    return charSet;
  }

  public void setCharSet(String charSet) {
    this.charSet = charSet;
  }

  public RequestEnum getRequestEnum() {
    return requestEnum;
  }

  public void setRequestEnum(RequestEnum requestEnum) {
    this.requestEnum = requestEnum;
  }

  public boolean isUseServerFileName() {
    return useServerFileName;
  }

  public void setUseServerFileName(boolean useServerFileName) {
    this.useServerFileName = useServerFileName;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public boolean isRemoveFile() {
    return removeFile;
  }

  public void setRemoveFile(boolean removeFile) {
    this.removeFile = removeFile;
  }

  public boolean isSupportBP() {
    return isSupportBP;
  }

  public void setSupportBP(boolean supportBP) {
    isSupportBP = supportBP;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }
}
