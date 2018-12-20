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
package com.arialyy.aria.core;

import android.text.TextUtils;
import com.arialyy.aria.core.common.QueueMod;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.AriaCrashHandler;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.io.Serializable;

/**
 * Created by lyy on 2016/12/8. 信息配置 kotlin 方式有bug，不能将public去掉
 */
public final class Configuration {
  private static final String TAG = "Configuration";
  private static final String DOWNLOAD_CONFIG_FILE = "/Aria/AriaDownload.cfg";
  private static final String UPLOAD_CONFIG_FILE = "/Aria/AriaUpload.cfg";
  private static final String APP_CONFIG_FILE = "/Aria/AriaApp.cfg";
  private static final int TYPE_DOWNLOAD = 1;
  private static final int TYPE_UPLOAD = 2;
  private static final int TYPE_APP = 3;
  private static volatile Configuration INSTANCE = null;
  static final String XML_FILE = "/Aria/aria_config.xml";
  DownloadConfig downloadCfg;
  UploadConfig uploadCfg;
  AppConfig appCfg;

  private Configuration() {
    //删除老版本的配置文件
    String basePath = AriaManager.APP.getFilesDir().getPath();
    File oldDCfg = new File(String.format("%s/Aria/DownloadConfig.properties", basePath));
    if (oldDCfg.exists()) { // 只需要判断一个
      File oldUCfg = new File(String.format("%s/Aria/UploadConfig.properties", basePath));
      File oldACfg = new File(String.format("%s/Aria/AppConfig.properties", basePath));
      oldDCfg.delete();
      oldUCfg.delete();
      oldACfg.delete();
      // 删除配置触发更新
      File temp = new File(String.format("%s%s", basePath, XML_FILE));
      if (temp.exists()) {
        temp.delete();
      }
    }
    File newDCfg = new File(String.format("%s%s", basePath, DOWNLOAD_CONFIG_FILE));
    File newUCfg = new File(String.format("%s%s", basePath, UPLOAD_CONFIG_FILE));
    File newACfg = new File(String.format("%s%s", basePath, APP_CONFIG_FILE));
    // 加载下载配置
    if (newDCfg.exists()) {
      downloadCfg = (DownloadConfig) CommonUtil.readObjFromFile(newDCfg.getPath());
    }
    if (downloadCfg == null) {
      downloadCfg = new DownloadConfig();
    }
    // 加载上传配置
    if (newUCfg.exists()) {
      uploadCfg = (UploadConfig) CommonUtil.readObjFromFile(newUCfg.getPath());
    }
    if (uploadCfg == null) {
      uploadCfg = new UploadConfig();
    }
    // 加载app配置
    if (newACfg.exists()) {
      appCfg = (AppConfig) CommonUtil.readObjFromFile(newACfg.getPath());
    }
    if (appCfg == null) {
      appCfg = new AppConfig();
    }
  }

  static Configuration getInstance() {
    if (INSTANCE == null) {
      synchronized (AppConfig.class) {
        INSTANCE = new Configuration();
      }
    }
    return INSTANCE;
  }

  /**
   * 检查配置文件是否存在，只要{@link DownloadConfig}、{@link UploadConfig}、{@link AppConfig}其中一个不存在 则任务配置文件不存在
   *
   * @return {@code true}配置存在，{@code false}配置不存在
   */
  boolean configExists() {
    String basePath = AriaManager.APP.getFilesDir().getPath();
    return (new File(String.format("%s%s", basePath, DOWNLOAD_CONFIG_FILE))).exists()
        && (new File(String.format("%s%s", basePath, UPLOAD_CONFIG_FILE))).exists()
        && (new File(String.format("%s%s", basePath, APP_CONFIG_FILE))).exists();
  }

  abstract static class BaseConfig implements Serializable {

    /**
     * 类型
     *
     * @return {@link #TYPE_DOWNLOAD}、{@link #TYPE_UPLOAD}、{@link #TYPE_APP}
     */
    abstract int getType();

    /**
     * 保存配置
     */
    void save() {
      String basePath = AriaManager.APP.getFilesDir().getPath();
      String path = null;
      switch (getType()) {
        case TYPE_DOWNLOAD:
          path = DOWNLOAD_CONFIG_FILE;
          break;
        case TYPE_UPLOAD:
          path = UPLOAD_CONFIG_FILE;
          break;
        case TYPE_APP:
          path = APP_CONFIG_FILE;
          break;
      }
      if (!TextUtils.isEmpty(path)) {
        String tempPath = String.format("%s%s", basePath, path);
        CommonUtil.deleteFile(tempPath);
        CommonUtil.writeObjToFile(tempPath, this);
      } else {
        ALog.e(TAG, String.format("保存配置失败，配置类型：%s，原因：路径错误", getType()));
      }
    }
  }

  /**
   * 通用任务配置
   */
  abstract static class BaseTaskConfig extends BaseConfig implements Serializable {

    /**
     * 设置写文件buff大小，该数值大小不能小于2048，数值变小，下载速度会变慢
     */
    int buffSize = 8192;

    /**
     * 进度刷新间隔，默认1秒
     */
    long updateInterval = 1000;

    /**
     * 旧任务数
     */
    public int oldMaxTaskNum = 2;

    /**
     * 任务队列最大任务数， 默认为2
     */
    int maxTaskNum = 2;
    /**
     * 下载失败，重试次数，默认为10
     */
    int reTryNum = 10;
    /**
     * 设置重试间隔，单位为毫秒，默认2000毫秒
     */
    int reTryInterval = 2000;
    /**
     * 设置url连接超时时间，单位为毫秒，默认5000毫秒
     */
    int connectTimeOut = 5000;

    /**
     * 是否需要转换速度单位，转换完成后为：1b/s、1k/s、1m/s、1g/s、1t/s，如果不需要将返回byte长度
     */
    boolean isConvertSpeed = false;

    /**
     * 执行队列类型
     *
     * @see QueueMod
     */
    String queueMod = "wait";

    /**
     * 断网的时候是否重试，{@code true}断网也重试；{@code false}断网不重试，直接走失败的回调
     */
    boolean notNetRetry = false;

    /**
     * 设置IO流读取时间，单位为毫秒，默认20000毫秒，该时间不能少于10000毫秒
     */
    int iOTimeOut = 20 * 1000;

    /**
     * 设置最大下载/上传速度，单位：kb, 为0表示不限速
     */
    int maxSpeed = 0;

    /**
     * 是否使用广播 除非无法使用注解，否则不建议使用广播来接受任务 {@code true} 使用广播，{@code false} 不适用广播
     */
    boolean useBroadcast = false;

    public boolean isUseBroadcast() {
      return useBroadcast;
    }

    public BaseTaskConfig setUseBroadcast(boolean useBroadcast) {
      this.useBroadcast = useBroadcast;
      save();
      return this;
    }

    public int getMaxSpeed() {
      return maxSpeed;
    }

    public BaseTaskConfig setMaxSpeed(int maxSpeed) {
      this.maxSpeed = maxSpeed;
      save();
      return this;
    }

    public long getUpdateInterval() {
      return updateInterval;
    }

    /**
     * 设置进度更新间隔，该设置对正在运行的任务无效，默认为1000毫秒
     *
     * @param updateInterval 不能小于0
     */
    public BaseTaskConfig setUpdateInterval(long updateInterval) {
      if (updateInterval <= 0) {
        ALog.w("Configuration", "进度更新间隔不能小于0");
        return this;
      }
      this.updateInterval = updateInterval;
      save();
      return this;
    }

    public String getQueueMod() {
      return queueMod;
    }

    public BaseTaskConfig setQueueMod(String queueMod) {
      this.queueMod = queueMod;
      save();
      return this;
    }

    public int getMaxTaskNum() {
      return maxTaskNum;
    }

    public int getReTryNum() {
      return reTryNum;
    }

    public BaseTaskConfig setReTryNum(int reTryNum) {
      this.reTryNum = reTryNum;
      save();
      return this;
    }

    public int getReTryInterval() {
      return reTryInterval;
    }

    public BaseTaskConfig setReTryInterval(int reTryInterval) {
      this.reTryInterval = reTryInterval;
      save();
      return this;
    }

    public boolean isConvertSpeed() {
      return isConvertSpeed;
    }

    public BaseTaskConfig setConvertSpeed(boolean convertSpeed) {
      isConvertSpeed = convertSpeed;
      save();
      return this;
    }

    public int getConnectTimeOut() {
      return connectTimeOut;
    }

    public BaseTaskConfig setConnectTimeOut(int connectTimeOut) {
      this.connectTimeOut = connectTimeOut;
      save();
      return this;
    }

    public boolean isNotNetRetry() {
      return notNetRetry;
    }

    public BaseTaskConfig setNotNetRetry(boolean notNetRetry) {
      this.notNetRetry = notNetRetry;
      save();
      return this;
    }

    public int getIOTimeOut() {
      return iOTimeOut;
    }

    public BaseTaskConfig setIOTimeOut(int iOTimeOut) {
      this.iOTimeOut = iOTimeOut;
      save();
      return this;
    }

    public int getBuffSize() {
      return buffSize;
    }

    public BaseTaskConfig setBuffSize(int buffSize) {
      this.buffSize = buffSize;
      save();
      return this;
    }
  }

  /**
   * 下载配置
   */
  public static class DownloadConfig extends BaseTaskConfig implements Serializable {

    /**
     * 设置https ca 证书信息；path 为assets目录下的CA证书完整路径
     */
    String caPath;
    /**
     * name 为CA证书名
     */
    String caName;
    /**
     * 下载线程数，下载线程数不能小于1 注意： 1、线程下载数改变后，新的下载任务才会生效； 2、如果任务大小小于1m，该设置不会生效；
     * 3、从3.4.1开始，如果线程数为1，文件初始化时将不再预占用对应长度的空间，下载多少byte，则占多大的空间； 对于采用多线程的任务或旧任务，依然采用原来的文件空间占用方式；
     */
    int threadNum = 3;

    /**
     * 多线程下载是否使用块下载模式，{@code true}使用，{@code false}不使用 注意： 1、使用分块模式，在读写性能底下的手机上，合并文件需要的时间会更加长；
     * 2、优点是使用多线程的块下载，初始化时，文件初始化时将不会预占用对应长度的空间； 3、只对新的多线程下载任务有效 4、只对多线程的任务有效
     */
    boolean useBlock = false;

    public boolean isUseBlock() {
      return useBlock;
    }

    @Override public DownloadConfig setMaxSpeed(int maxSpeed) {
      super.setMaxSpeed(maxSpeed);
      DownloadTaskQueue.getInstance().setMaxSpeed(maxSpeed);
      DownloadGroupTaskQueue.getInstance().setMaxSpeed(maxSpeed);
      return this;
    }

    public DownloadConfig setUseBlock(boolean useBlock) {
      this.useBlock = useBlock;
      save();
      return this;
    }

    public DownloadConfig setMaxTaskNum(int maxTaskNum) {
      oldMaxTaskNum = this.maxTaskNum;
      this.maxTaskNum = maxTaskNum;
      DownloadTaskQueue.getInstance().setMaxTaskNum(maxTaskNum);
      save();
      return this;
    }

    public DownloadConfig setThreadNum(int threadNum) {
      this.threadNum = threadNum;
      save();
      return this;
    }

    public String getCaPath() {
      return caPath;
    }

    public DownloadConfig setCaPath(String caPath) {
      this.caPath = caPath;
      save();
      return this;
    }

    public String getCaName() {
      return caName;
    }

    public DownloadConfig setCaName(String caName) {
      this.caName = caName;
      save();
      return this;
    }

    public int getThreadNum() {
      return threadNum;
    }

    private DownloadConfig() {
    }

    @Override int getType() {
      return TYPE_DOWNLOAD;
    }
  }

  /**
   * 上传配置
   */
  public static class UploadConfig extends BaseTaskConfig implements Serializable {
    private static UploadConfig INSTANCE = null;

    private UploadConfig() {
      //loadConfig();
    }

    @Override public UploadConfig setMaxSpeed(int maxSpeed) {
      super.setMaxSpeed(maxSpeed);
      UploadTaskQueue.getInstance().setMaxSpeed(maxSpeed);
      return this;
    }

    public UploadConfig setMaxTaskNum(int maxTaskNum) {
      oldMaxTaskNum = this.maxTaskNum;
      this.maxTaskNum = maxTaskNum;
      UploadTaskQueue.getInstance().setMaxTaskNum(maxTaskNum);
      save();
      return this;
    }

    static UploadConfig getInstance() {
      if (INSTANCE == null) {
        synchronized (DownloadConfig.class) {
          INSTANCE = new UploadConfig();
        }
      }
      return INSTANCE;
    }

    @Override int getType() {
      return TYPE_UPLOAD;
    }
  }

  /**
   * 应用配置
   */
  public static class AppConfig extends BaseConfig {
    /**
     * 是否使用{@link AriaCrashHandler}来捕获异常 {@code true} 使用；{@code false} 不使用
     */
    boolean useAriaCrashHandler;

    /**
     * 设置Aria的日志级别
     *
     * {@link ALog#LOG_LEVEL_VERBOSE}
     */
    int logLevel;

    /**
     * 是否检查网络，{@code true}检查网络
     */
    boolean netCheck = true;

    public boolean isNetCheck() {
      return netCheck;
    }

    public AppConfig setNetCheck(boolean netCheck) {
      this.netCheck = netCheck;
      save();
      return this;
    }

    public AppConfig setLogLevel(int level) {
      this.logLevel = level;
      ALog.LOG_LEVEL = level;
      save();
      return this;
    }

    public int getLogLevel() {
      return logLevel;
    }

    public boolean getUseAriaCrashHandler() {
      return useAriaCrashHandler;
    }

    public AppConfig setUseAriaCrashHandler(boolean useAriaCrashHandler) {
      this.useAriaCrashHandler = useAriaCrashHandler;
      if (useAriaCrashHandler) {
        Thread.setDefaultUncaughtExceptionHandler(new AriaCrashHandler());
      } else {
        Thread.setDefaultUncaughtExceptionHandler(null);
      }
      save();
      return this;
    }

    @Override int getType() {
      return TYPE_APP;
    }
  }
}
