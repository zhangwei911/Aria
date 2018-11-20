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
package com.arialyy.aria.core.common;

import android.os.Build;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Aria.Lao on 2017/7/10.
 * 代理参数获取
 */
public class ProxyHelper {
  /**
   * 普通下载任务类型
   */
  public static int PROXY_TYPE_DOWNLOAD = 0x01;
  /**
   * 组合下载任务类型
   */
  public static int PROXY_TYPE_DOWNLOAD_GROUP = 0x02;
  ///**
  // * 组合任务子任务类型
  // */
  //public static int PROXY_TYPE_DOWNLOAD_GROUP_SUB = 0x03;
  /**
   * 普通上传任务类型
   */
  public static int PROXY_TYPE_UPLOAD = 0x04;
  public Set<String> downloadCounter = new HashSet<>(), uploadCounter = new HashSet<>(),
      downloadGroupCounter = new HashSet<>(), downloadGroupSubCounter = new HashSet<>();
  public Map<String, Set<Integer>> mProxyCache = new ConcurrentHashMap<>();

  public static volatile ProxyHelper INSTANCE = null;
  private boolean canLoadClass = false;

  private ProxyHelper() {
    //init();
  }

  public static ProxyHelper getInstance() {
    if (INSTANCE == null) {
      synchronized (AriaManager.LOCK) {
        INSTANCE = new ProxyHelper();
      }
    }
    return INSTANCE;
  }

  /**
   * @since 3.4.6 版本开始，已经在ElementHandler中关闭了ProxyClassCounter对象的生成
   */
  @Deprecated
  private void init() {
    List<String> classes = CommonUtil.getPkgClassNames(AriaManager.APP,
        "com.arialyy.aria.ProxyClassCounter");
    canLoadClass = classes != null
        && !classes.isEmpty()
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    if (canLoadClass) {
      for (String className : classes) {
        count(className);
      }
    }
  }

  /**
   * 是否能读取到代理计数文件
   *
   * @return {@code true} 可以读取，{@code false} 不能读取
   */
  public boolean isCanLoadCountClass() {
    return canLoadClass;
  }

  /**
   * 检查观察者对象的代理文件类型
   *
   * @param clazz 观察者对象
   * @return {@link #PROXY_TYPE_DOWNLOAD}，如果没有实体对象则返回空的list
   */
  public Set<Integer> checkProxyType(Class clazz) {
    final String className = clazz.getName();
    Set<Integer> result = mProxyCache.get(clazz.getName());
    if (result != null) {
      return result;
    }
    result = new HashSet<>();
    try {
      if (Class.forName(className.concat("$$DownloadGroupListenerProxy")) != null) {
        result.add(PROXY_TYPE_DOWNLOAD_GROUP);
      }
    } catch (ClassNotFoundException e) {
      //e.printStackTrace();
    }
    try {
      if (Class.forName(className.concat("$$DownloadListenerProxy")) != null) {
        result.add(PROXY_TYPE_DOWNLOAD);
      }
    } catch (ClassNotFoundException e) {
      //e.printStackTrace();
    }

    try {
      if (Class.forName(className.concat("$$UploadListenerProxy")) != null) {
        result.add(PROXY_TYPE_UPLOAD);
      }
    } catch (ClassNotFoundException e) {
      //e.printStackTrace();
    }

    if (!result.isEmpty()) {
      mProxyCache.put(clazz.getName(), result);
    }
    return result;
  }

  @Deprecated
  private void count(String className) {
    try {
      Class clazz = Class.forName(className);
      Method download = clazz.getMethod("getDownloadCounter");
      Method downloadGroup = clazz.getMethod("getDownloadGroupCounter");
      Method downloadGroupSub = clazz.getMethod("getDownloadGroupSubCounter");
      Method upload = clazz.getMethod("getUploadCounter");
      Object object = clazz.newInstance();
      Object dc = download.invoke(object);
      if (dc != null) {
        downloadCounter.addAll((Set<String>) dc);
      }
      Object dgc = downloadGroup.invoke(object);
      if (dgc != null) {
        downloadGroupCounter.addAll((Set<String>) dgc);
      }
      Object dgsc = downloadGroupSub.invoke(object);
      if (dgsc != null) {
        downloadGroupSubCounter.addAll((Set<String>) dgsc);
      }
      Object uc = upload.invoke(object);
      if (uc != null) {
        uploadCounter.addAll((Set<String>) uc);
      }
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
