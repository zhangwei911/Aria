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
import com.arialyy.aria.util.ErrorHelp;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Properties;

/**
 * Created by lyy on 2016/12/8.
 * 信息配置
 */
public class Configuration {
  static final String TAG = "Configuration";
  static final String DOWNLOAD_CONFIG_FILE = "/Aria/DownloadConfig.properties";
  static final String UPLOAD_CONFIG_FILE = "/Aria/UploadConfig.properties";
  static final String APP_CONFIG_FILE = "/Aria/AppConfig.properties";
  static final String XML_FILE = "/Aria/aria_config.xml";
  static final int TYPE_DOWNLOAD = 1;
  static final int TYPE_UPLOAD = 2;
  static final int TYPE_APP = 3;

  abstract static class BaseConfig {

    /**
     * 类型
     *
     * @return {@link #TYPE_DOWNLOAD}、{@link #TYPE_UPLOAD}、{@link #TYPE_APP}
     */
    abstract int getType();

    /**
     * 加载配置
     */
    void loadConfig() {
      String path = null;
      Class clazz = null;
      switch (getType()) {
        case TYPE_DOWNLOAD:
          path = DOWNLOAD_CONFIG_FILE;
          clazz = DownloadConfig.class;
          break;
        case TYPE_UPLOAD:
          path = UPLOAD_CONFIG_FILE;
          clazz = UploadConfig.class;
          break;
        case TYPE_APP:
          path = APP_CONFIG_FILE;
          clazz = AppConfig.class;
          break;
      }
      if (TextUtils.isEmpty(path)) {
        ALog.e(TAG, "读取配置失败：未知文件类型");
        ErrorHelp.saveError(TAG, "读取配置失败：未知文件类型", "");
        return;
      }

      File file = new File(AriaManager.APP.getFilesDir().getPath() + path);
      if (file.exists()) {
        Properties properties = CommonUtil.loadConfig(file);
        List<Field> fields = CommonUtil.getAllFields(clazz);
        try {
          for (Field field : fields) {
            int m = field.getModifiers();
            String fileName = field.getName();
            if (fileName.equals("oldMaxTaskNum")
                || field.isSynthetic()
                || Modifier.isFinal(m)
                || Modifier.isStatic(m)
                || fileName.equals("shadow$_klass_")
                || fileName.equals("shadow$_monitor_")) {
              continue;
            }
            field.setAccessible(true);
            String value = properties.getProperty(field.getName());
            if (TextUtils.isEmpty(value) || value.equalsIgnoreCase("null")) continue;
            Class<?> type = field.getType();
            if (type == String.class) {
              field.set(this, value);
            } else if (type == int.class || type == Integer.class) {
              if (fileName.equalsIgnoreCase("maxSpeed")) { //兼容以前版本，以前maxSpeed是double类型的
                Double d = Double.parseDouble(value);
                field.setInt(this, (int) d.doubleValue());
              } else {
                field.setInt(this, Integer.parseInt(value));
              }
            } else if (type == float.class || type == Float.class) {
              field.setFloat(this, Float.parseFloat(value));
            } else if (type == double.class || type == Double.class) {
              if (TextUtils.isEmpty(value)) {
                value = "0";
              }
              field.setDouble(this, Double.parseDouble(value));
            } else if (type == long.class || type == Long.class) {
              field.setLong(this, Long.parseLong(value));
            } else if (type == boolean.class || type == Boolean.class) {
              field.setBoolean(this, Boolean.parseBoolean(value));
            }
          }
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * 保存key
     */
    void saveKey(String key, String value) {
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
      File file = new File(
          AriaManager.APP.getFilesDir().getPath() + path);
      if (file.exists()) {
        Properties properties = CommonUtil.loadConfig(file);
        properties.setProperty(key, value);
        CommonUtil.saveConfig(file, properties);
      }
    }

    /**
     * 保存配置
     */
    void saveAll() {
      List<Field> fields = CommonUtil.getAllFields(getClass());
      try {
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
        File file = new File(
            AriaManager.APP.getFilesDir().getPath() + path);
        Properties properties = CommonUtil.loadConfig(file);
        for (Field field : fields) {
          int m = field.getModifiers();
          if (field.isSynthetic() || Modifier.isFinal(m) || Modifier.isStatic(m) || field.getName()
              .equals("shadow$_klass_") || field.getName().equals("shadow$_monitor_")) {
            continue;
          }
          field.setAccessible(true);
          properties.setProperty(field.getName(), field.get(this) + "");
        }
        CommonUtil.saveConfig(file, properties);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 通用任务配置
   */
  abstract static class BaseTaskConfig extends BaseConfig {

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

    public int getMaxSpeed() {
      return maxSpeed;
    }

    public BaseTaskConfig setMaxSpeed(int maxSpeed) {
      this.maxSpeed = maxSpeed;
      saveKey("maxSpeed", String.valueOf(maxSpeed));
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
      saveKey("updateInterval", String.valueOf(updateInterval));
      return this;
    }

    public String getQueueMod() {
      return queueMod;
    }

    public BaseTaskConfig setQueueMod(String queueMod) {
      this.queueMod = queueMod;
      saveKey("queueMod", queueMod);
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
      saveKey("reTryNum", String.valueOf(reTryNum));
      return this;
    }

    public int getReTryInterval() {
      return reTryInterval;
    }

    public BaseTaskConfig setReTryInterval(int reTryInterval) {
      this.reTryInterval = reTryInterval;
      saveKey("reTryInterval", String.valueOf(reTryInterval));
      return this;
    }

    public boolean isConvertSpeed() {
      return isConvertSpeed;
    }

    public BaseTaskConfig setConvertSpeed(boolean convertSpeed) {
      isConvertSpeed = convertSpeed;
      saveKey("isConvertSpeed", String.valueOf(isConvertSpeed));
      return this;
    }

    public int getConnectTimeOut() {
      return connectTimeOut;
    }

    public BaseTaskConfig setConnectTimeOut(int connectTimeOut) {
      this.connectTimeOut = connectTimeOut;
      saveKey("connectTimeOut", String.valueOf(connectTimeOut));
      return this;
    }

    public boolean isNotNetRetry() {
      return notNetRetry;
    }

    public BaseTaskConfig setNotNetRetry(boolean notNetRetry) {
      this.notNetRetry = notNetRetry;
      saveKey("notNetRetry", String.valueOf(notNetRetry));
      return this;
    }

    public int getIOTimeOut() {
      return iOTimeOut;
    }

    public BaseTaskConfig setIOTimeOut(int iOTimeOut) {
      this.iOTimeOut = iOTimeOut;
      saveKey("iOTimeOut", String.valueOf(iOTimeOut));
      return this;
    }

    public int getBuffSize() {
      return buffSize;
    }

    public BaseTaskConfig setBuffSize(int buffSize) {
      this.buffSize = buffSize;
      saveKey("buffSize", String.valueOf(buffSize));
      return this;
    }
  }

  /**
   * 下载配置
   */
  public static class DownloadConfig extends BaseTaskConfig {

    /**
     * 设置https ca 证书信息；path 为assets目录下的CA证书完整路径
     */
    String caPath;
    /**
     * name 为CA证书名
     */
    String caName;
    /**
     * 下载线程数，下载线程数不能小于1
     * 注意：
     * 1、线程下载数改变后，新的下载任务才会生效；
     * 2、如果任务大小小于1m，该设置不会生效；
     * 3、从3.4.1开始，如果线程数为1，文件初始化时将不再预占用对应长度的空间，下载多少byte，则占多大的空间；
     * 对于采用多线程的任务或旧任务，依然采用原来的文件空间占用方式；
     */
    int threadNum = 3;

    /**
     * 多线程下载是否使用块下载模式，{@code true}使用，{@code false}不使用
     * 注意：
     * 1、使用分块模式，在读写性能底下的手机上，合并文件需要的时间会更加长；
     * 2、优点是使用多线程的块下载，初始化时，文件初始化时将不会预占用对应长度的空间；
     * 3、只对新的多线程下载任务有效
     * 4、只对多线程的任务有效
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
      saveKey("useBlock", String.valueOf(useBlock));
      return this;
    }

    public DownloadConfig setMaxTaskNum(int maxTaskNum) {
      oldMaxTaskNum = this.maxTaskNum;
      this.maxTaskNum = maxTaskNum;
      saveKey("maxTaskNum", String.valueOf(maxTaskNum));
      DownloadTaskQueue.getInstance().setMaxTaskNum(maxTaskNum);
      return this;
    }

    public DownloadConfig setThreadNum(int threadNum) {
      this.threadNum = threadNum;
      saveKey("threadNum", String.valueOf(threadNum));
      return this;
    }

    public String getCaPath() {
      return caPath;
    }

    public DownloadConfig setCaPath(String caPath) {
      this.caPath = caPath;
      saveKey("caPath", caPath);
      return this;
    }

    public String getCaName() {
      return caName;
    }

    public DownloadConfig setCaName(String caName) {
      this.caName = caName;
      saveKey("caName", caName);
      return this;
    }

    public int getThreadNum() {
      return threadNum;
    }

    private DownloadConfig() {
      loadConfig();
    }

    private static DownloadConfig INSTANCE = null;

    static DownloadConfig getInstance() {
      if (INSTANCE == null) {
        synchronized (DownloadConfig.class) {
          INSTANCE = new DownloadConfig();
        }
      }
      return INSTANCE;
    }

    @Override int getType() {
      return TYPE_DOWNLOAD;
    }
  }

  /**
   * 上传配置
   */
  public static class UploadConfig extends BaseTaskConfig {
    private static UploadConfig INSTANCE = null;

    private UploadConfig() {
      loadConfig();
    }

    @Override public UploadConfig setMaxSpeed(int maxSpeed) {
      super.setMaxSpeed(maxSpeed);
      UploadTaskQueue.getInstance().setMaxSpeed(maxSpeed);
      return this;
    }

    public UploadConfig setMaxTaskNum(int maxTaskNum) {
      oldMaxTaskNum = this.maxTaskNum;
      this.maxTaskNum = maxTaskNum;
      saveKey("maxTaskNum", String.valueOf(maxTaskNum));
      UploadTaskQueue.getInstance().setMaxTaskNum(maxTaskNum);
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
    private static AppConfig INSTANCE = null;
    /**
     * 是否使用{@link AriaCrashHandler}来捕获异常
     * {@code true} 使用；{@code false} 不使用
     */
    boolean useAriaCrashHandler;

    /**
     * 设置Aria的日志级别
     *
     * {@link ALog#LOG_LEVEL_VERBOSE}
     */
    int logLevel;

    AppConfig() {
      loadConfig();
    }

    static AppConfig getInstance() {
      if (INSTANCE == null) {
        synchronized (AppConfig.class) {
          INSTANCE = new AppConfig();
        }
      }
      return INSTANCE;
    }

    public AppConfig setLogLevel(int level) {
      this.logLevel = level;
      ALog.LOG_LEVEL = level;
      saveKey("logLevel", String.valueOf(logLevel));
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
      saveKey("useAriaCrashHandler", String.valueOf(useAriaCrashHandler));
      if (useAriaCrashHandler) {
        Thread.setDefaultUncaughtExceptionHandler(new AriaCrashHandler());
      } else {
        Thread.setDefaultUncaughtExceptionHandler(null);
      }
      return this;
    }

    @Override int getType() {
      return TYPE_APP;
    }
  }
}
