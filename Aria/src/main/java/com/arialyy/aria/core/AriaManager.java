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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.widget.PopupWindow;
import com.arialyy.aria.core.command.CommandManager;
import com.arialyy.aria.core.common.QueueMod;
import com.arialyy.aria.core.common.RecordHandler;
import com.arialyy.aria.core.config.AppConfig;
import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.config.DGroupConfig;
import com.arialyy.aria.core.config.DownloadConfig;
import com.arialyy.aria.core.config.UploadConfig;
import com.arialyy.aria.core.config.XMLReader;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DownloadReceiver;
import com.arialyy.aria.core.inf.AbsReceiver;
import com.arialyy.aria.core.inf.IReceiver;
import com.arialyy.aria.core.inf.ReceiverType;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.UploadReceiver;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.orm.DelegateWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.AriaCrashHandler;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.RecordUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Created by lyy on 2016/12/1. https://github.com/AriaLyy/Aria Aria管理器，任务操作在这里执行
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH) public class AriaManager {
  private static final String TAG = "AriaManager";
  private static final Object LOCK = new Object();
  public static final String DOWNLOAD_TEMP_DIR = "/Aria/temp/download/";
  public static final String UPLOAD_TEMP_DIR = "/Aria/temp/upload/";
  /**
   * 是否已经联网，true 已经联网
   */
  private static boolean isConnectedNet = true;

  @SuppressLint("StaticFieldLeak") private static volatile AriaManager INSTANCE = null;
  private Map<String, AbsReceiver> mReceivers = new ConcurrentHashMap<>();
  /**
   * activity 和其Dialog、Fragment的映射表
   */
  private Map<String, List<String>> mSubClass = new ConcurrentHashMap<>();
  public static Context APP;
  private DownloadConfig mDConfig;
  private UploadConfig mUConfig;
  private AppConfig mAConfig;
  private DGroupConfig mDGConfig;
  private Handler mAriaHandler;
  private DelegateWrapper mDbWrapper;

  private AriaManager(Context context) {
    APP = context.getApplicationContext();
    initDb(APP);
    regAppLifeCallback(context);
    initConfig();
    initAria();
    amendTaskState();
    regNetCallBack(context);
  }

  public static AriaManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (LOCK) {
        if (INSTANCE == null) {
          INSTANCE = new AriaManager(context);
        }
      }
    }
    return INSTANCE;
  }

  static AriaManager getInstance() {
    return INSTANCE;
  }

  /**
   * 注册网络监听，只有配置了检查网络{@link AppConfig#isNetCheck()}才会注册事件
   */
  private void regNetCallBack(Context context) {
    if (!getAppConfig().isNetCheck()) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      return;
    }
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      return;
    }

    NetworkRequest.Builder builder = new NetworkRequest.Builder();
    NetworkRequest request = builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build();

    cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {

      @Override public void onLost(Network network) {
        super.onLost(network);
        isConnectedNet = false;
        ALog.d(TAG, "onLost");
      }

      @Override public void onAvailable(Network network) {
        super.onAvailable(network);
        ALog.d(TAG, "onAvailable");
        isConnectedNet = true;
      }
    });
  }

  /**
   * 初始化数据库
   */
  private void initDb(Context context) {
    String oldDbName = "AriaLyyDb";
    File oldDbFile = context.getDatabasePath(oldDbName);
    if (oldDbFile != null && oldDbFile.exists()) {
      File dbConfig = new File(String.format("%s/%s", oldDbFile.getParent(), "AriaLyyDb-journal"));
      oldDbFile.renameTo(new File(String.format("%s/%s", oldDbFile.getParent(), "AndroidAria.db")));
      // 如果数据库是在/data/data/{packagename}/databases/下面，journal文件因权限问题将无法删除和重命名
      if (dbConfig.exists()) {
        dbConfig.delete();
      }
    }
    mDbWrapper = DelegateWrapper.init(context.getApplicationContext());
  }

  private void initAria() {
    if (mAConfig.getUseAriaCrashHandler()) {
      Thread.setDefaultUncaughtExceptionHandler(new AriaCrashHandler());
    }
    mAConfig.setLogLevel(mAConfig.getLogLevel());
    CommandManager.init();
  }

  /**
   * 修正任务状态
   */
  private void amendTaskState() {
    Class[] clazzs = new Class[] {
        DownloadEntity.class, UploadEntity.class, DownloadGroupEntity.class
    };
    String sql = "UPDATE %s SET state=2 WHERE state IN (3,4,5,6)";
    for (Class clazz : clazzs) {
      if (!mDbWrapper.tableExists(clazz)) {
        continue;
      }
      String temp = String.format(sql, clazz.getSimpleName());
      DbEntity.exeSql(temp);
    }
  }

  public synchronized Handler getAriaHandler() {
    if (mAriaHandler == null) {
      mAriaHandler = new Handler(Looper.getMainLooper());
    }
    return mAriaHandler;
  }

  public boolean isConnectedNet() {
    return isConnectedNet;
  }

  public Map<String, AbsReceiver> getReceiver() {
    return mReceivers;
  }

  /**
   * 设置上传任务的执行队列类型，后续版本会删除该api，请使用：
   * <pre>
   *   <code>
   *     Aria.get(this).getUploadConfig().setQueueMod(mod.tag)
   *   </code>
   * </pre>
   *
   * @param mod {@link QueueMod}
   * @deprecated 后续版本会删除该api
   */
  @Deprecated public AriaManager setUploadQueueMod(QueueMod mod) {
    mUConfig.setQueueMod(mod.tag);
    return this;
  }

  /**
   * 设置下载任务的执行队列类型，后续版本会删除该api，请使用：
   * <pre>
   *   <code>
   *     Aria.get(this).getDownloadConfig().setQueueMod(mod.tag)
   *   </code>
   * </pre>
   *
   * @param mod {@link QueueMod}
   * @deprecated 后续版本会删除该api
   */
  @Deprecated public AriaManager setDownloadQueueMod(QueueMod mod) {
    mDConfig.setQueueMod(mod.tag);
    return this;
  }

  /**
   * 如果需要在代码中修改下载配置，请使用以下方法
   * <pre>
   *   <code>
   *     //修改最大任务队列数
   *     Aria.get(this).getDownloadConfig().setMaxTaskNum(3);
   *   </code>
   * </pre>
   */
  public DownloadConfig getDownloadConfig() {
    return mDConfig;
  }

  /**
   * 如果需要在代码中修改下载配置，请使用以下方法
   * <pre>
   *   <code>
   *     //修改最大任务队列数
   *     Aria.get(this).getUploadConfig().setMaxTaskNum(3);
   *   </code>
   * </pre>
   */
  public UploadConfig getUploadConfig() {
    return mUConfig;
  }

  /**
   * 获取APP配置
   */
  public AppConfig getAppConfig() {
    return mAConfig;
  }

  /**
   * 如果需要在代码中修改下载类型的组合任务的配置，请使用以下方法
   * <pre>
   *   <code>
   *     //修改最大任务队列数
   *     Aria.get(this).getDownloadConfig().setMaxTaskNum(3);
   *   </code>
   * </pre>
   */
  public DGroupConfig getDGroupConfig() {
    return mDGConfig;
  }

  /**
   * 处理下载操作
   */
  DownloadReceiver download(Object obj) {
    IReceiver receiver = mReceivers.get(getKey(ReceiverType.DOWNLOAD, obj));
    if (receiver == null) {
      receiver = putReceiver(ReceiverType.DOWNLOAD, obj);
    }
    return (receiver instanceof DownloadReceiver) ? (DownloadReceiver) receiver : null;
  }

  /**
   * 处理上传操作
   */
  UploadReceiver upload(Object obj) {
    IReceiver receiver = mReceivers.get(getKey(ReceiverType.UPLOAD, obj));
    if (receiver == null) {
      receiver = putReceiver(ReceiverType.UPLOAD, obj);
    }
    return (receiver instanceof UploadReceiver) ? (UploadReceiver) receiver : null;
  }

  /**
   * 删除任务记录
   *
   * @param type 需要删除的任务类型，1、普通下载任务；2、组合任务；3、普通上传任务。
   * @param key type为1时，key为保存路径；type为2时，key为组合任务hash；type为3时，key为文件上传路径。
   * @param removeFile {@code true} 不仅删除任务数据库记录，还会删除已经删除完成的文件；{@code false}如果任务已经完成，只删除任务数据库记录。
   */
  public void delRecord(int type, String key, boolean removeFile) {
    switch (type) {
      case 1: // 删除普通任务记录
        RecordUtil.delTaskRecord(key, RecordHandler.TYPE_DOWNLOAD, removeFile, true);
        break;
      case 2:
        RecordUtil.delGroupTaskRecord(key, removeFile);
        break;
      case 3:
        RecordUtil.delTaskRecord(key, RecordHandler.TYPE_UPLOAD);
        break;
    }
  }

  private IReceiver putReceiver(@ReceiverType String type, Object obj) {
    final String key = getKey(type, obj);
    IReceiver receiver = mReceivers.get(key);
    boolean needRmReceiver = false;
    // 监控Dialog、fragment、popupWindow的生命周期
    final WidgetLiftManager widgetLiftManager = new WidgetLiftManager();
    if (obj instanceof Dialog) {
      needRmReceiver = widgetLiftManager.handleDialogLift((Dialog) obj);
    } else if (obj instanceof PopupWindow) {
      needRmReceiver = widgetLiftManager.handlePopupWindowLift((PopupWindow) obj);
    } else if (obj instanceof DialogFragment) {
      needRmReceiver = widgetLiftManager.handleDialogFragmentLift((DialogFragment) obj);
    } else if (obj instanceof android.app.DialogFragment) {
      needRmReceiver = widgetLiftManager.handleDialogFragmentLift((android.app.DialogFragment) obj);
    }

    if (receiver == null) {
      AbsReceiver absReceiver;
      switch (type) {
        case ReceiverType.DOWNLOAD:
          absReceiver = new DownloadReceiver();
          break;
        case ReceiverType.UPLOAD:
          absReceiver = new UploadReceiver();
          break;
        default:
          absReceiver = new DownloadReceiver();
          break;
      }
      absReceiver.targetName = obj.getClass().getName();
      AbsReceiver.OBJ_MAP.put(absReceiver.getKey(), obj);
      absReceiver.needRmListener = needRmReceiver;
      mReceivers.put(key, absReceiver);
      receiver = absReceiver;
    }
    return receiver;
  }

  /**
   * 根据功能类型和控件类型获取对应的key
   *
   * @param type {@link ReceiverType}
   * @param obj 观察者对象
   * @return {@link #createKey(String, Object)}
   */
  private String getKey(@ReceiverType String type, Object obj) {
    if (obj instanceof DialogFragment) {
      relateSubClass(type, obj, ((DialogFragment) obj).getActivity());
    } else if (obj instanceof android.app.DialogFragment) {
      relateSubClass(type, obj, ((android.app.DialogFragment) obj).getActivity());
    } else if (obj instanceof android.support.v4.app.Fragment) {
      relateSubClass(type, obj, ((Fragment) obj).getActivity());
    } else if (obj instanceof android.app.Fragment) {
      relateSubClass(type, obj, ((android.app.Fragment) obj).getActivity());
    } else if (obj instanceof Dialog) {
      Activity activity = ((Dialog) obj).getOwnerActivity();
      if (activity != null) {
        relateSubClass(type, obj, activity);
      }
    } else if (obj instanceof PopupWindow) {
      Context context = ((PopupWindow) obj).getContentView().getContext();
      if (context instanceof Activity) {
        relateSubClass(type, obj, (Activity) context);
      }
    }
    return createKey(type, obj);
  }

  /**
   * 关联Activity类和Fragment间的关系
   *
   * @param sub Frgament或dialog类
   * @param activity activity寄主类
   */
  private void relateSubClass(@ReceiverType String type, Object sub, Activity activity) {
    String key = createKey(type, activity);
    List<String> list = mSubClass.get(key);
    if (list == null) {
      list = new ArrayList<>();
      mSubClass.put(key, list);
    }
    list.add(createKey(type, sub));
  }

  /**
   * 根据功能类型和控件类型获取对应的key
   *
   * @param type {@link ReceiverType}
   * @param obj 观察者对象
   * @return key的格式为：{@code String.format("%s_%s_%s", obj.getClass().getName(), type,
   * obj.hashCode());}
   */
  private String createKey(@ReceiverType String type, Object obj) {
    return String.format("%s_%s_%s", obj.getClass().getName(), type, obj.hashCode());
  }

  /**
   * 初始化配置文件
   */
  private void initConfig() {
    mDConfig = Configuration.getInstance().downloadCfg;
    mUConfig = Configuration.getInstance().uploadCfg;
    mAConfig = Configuration.getInstance().appCfg;
    mDGConfig = Configuration.getInstance().dGroupCfg;

    File xmlFile = new File(APP.getFilesDir().getPath() + Configuration.XML_FILE);
    File tempDir = new File(APP.getFilesDir().getPath() + "/temp");
    if (!xmlFile.exists()) {
      loadConfig();
    } else {
      try {
        String md5Code = CommonUtil.getFileMD5(xmlFile);
        File file = new File(APP.getFilesDir().getPath() + "/temp.xml");
        if (file.exists()) {
          file.delete();
        }
        CommonUtil.createFileFormInputStream(APP.getAssets().open("aria_config.xml"),
            file.getPath());
        if (!CommonUtil.checkMD5(md5Code, file) || !Configuration.getInstance().configExists()) {
          loadConfig();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (tempDir.exists()) {
      File newDir = new File(APP.getFilesDir().getPath() + DOWNLOAD_TEMP_DIR);
      newDir.mkdirs();
      tempDir.renameTo(newDir);
    }
  }

  /**
   * 加载配置文件
   */
  private void loadConfig() {
    try {
      XMLReader helper = new XMLReader();
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser parser = factory.newSAXParser();
      parser.parse(APP.getAssets().open("aria_config.xml"), helper);
      CommonUtil.createFileFormInputStream(APP.getAssets().open("aria_config.xml"),
          APP.getFilesDir().getPath() + Configuration.XML_FILE);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      ALog.e(TAG, e.toString());
    }
  }

  /**
   * 注册APP生命周期回调
   */
  private void regAppLifeCallback(Context context) {
    Context app = context.getApplicationContext();
    if (app instanceof Application) {
      LifeCallback lifeCallback = new LifeCallback();
      ((Application) app).registerActivityLifecycleCallbacks(lifeCallback);
    }
  }

  /**
   * 移除指定对象的receiver
   */
  public void removeReceiver(Object obj) {
    if (obj == null) {
      ALog.e(TAG, "target obj is null");
      return;
    }
    List<String> temp = new ArrayList<>();
    // 移除寄主的receiver
    for (Iterator<Map.Entry<String, AbsReceiver>> iter = mReceivers.entrySet().iterator();
        iter.hasNext(); ) {
      Map.Entry<String, AbsReceiver> entry = iter.next();
      String key = entry.getKey();
      if (key.equals(getKey(ReceiverType.DOWNLOAD, obj)) || key.equals(
          getKey(ReceiverType.UPLOAD, obj))) {
        AbsReceiver receiver = mReceivers.get(key);
        List<String> subNames = mSubClass.get(key);
        if (subNames != null && !subNames.isEmpty()) {
          temp.addAll(subNames);
        }
        if (receiver != null) {
          receiver.destroy();
        }
        iter.remove();
      }
    }

    // 移除寄生的receiver
    if (!temp.isEmpty()) {
      for (Iterator<Map.Entry<String, AbsReceiver>> iter = mReceivers.entrySet().iterator();
          iter.hasNext(); ) {
        Map.Entry<String, AbsReceiver> entry = iter.next();
        if (temp.contains(entry.getKey())) {
          AbsReceiver receiver = mReceivers.get(entry.getKey());
          if (receiver != null) {
            receiver.destroy();
          }
          iter.remove();
        }
      }
    }
  }

  /**
   * Activity生命周期
   */
  private class LifeCallback implements Application.ActivityLifecycleCallbacks {

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override public void onActivityStarted(Activity activity) {

    }

    @Override public void onActivityResumed(Activity activity) {

    }

    @Override public void onActivityPaused(Activity activity) {

    }

    @Override public void onActivityStopped(Activity activity) {

    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override public void onActivityDestroyed(Activity activity) {
      removeReceiver(activity);
    }
  }
}
