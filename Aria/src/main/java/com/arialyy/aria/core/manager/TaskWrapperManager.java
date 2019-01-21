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
package com.arialyy.aria.core.manager;

import android.support.v4.util.LruCache;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Aria.Lao on 2017/11/1. 任务实体管理器
 */
public class TaskWrapperManager {
  private static final String TAG = "TaskManager";
  private static volatile TaskWrapperManager INSTANCE = null;
  private LruCache<String, AbsTaskWrapper> cache = new LruCache<>(1024);
  private Lock lock;

  public static TaskWrapperManager getInstance() {
    if (INSTANCE == null) {
      synchronized (TaskWrapperManager.class) {
        INSTANCE = new TaskWrapperManager();
      }
    }
    return INSTANCE;
  }

  private TaskWrapperManager() {
    lock = new ReentrantLock();
  }

  private IGTEFactory chooseGroupFactory(Class clazz) {
    if (clazz == DGTaskWrapper.class) {
      return DGTaskWrapperFactory.getInstance();
    }
    return null;
  }

  private INormalTEFactory chooseNormalFactory(Class clazz) {
    if (clazz == DTaskWrapper.class) {
      return DTaskWrapperFactory.getInstance();
    } else if (clazz == UTaskWrapper.class) {
      return UTaskWrapperFactory.getInstance();
    }
    return null;
  }

  /**
   * 从缓存中获取单任务实体，如果任务实体不存在，则创建任务实体
   *
   * @param key 下载任务，key为下载地址；上传任务，key为本地文件路径
   * @return 创建失败，返回null；成功返回{@link DTaskWrapper}或者{@link UTaskWrapper}
   */
  public <TE extends AbsTaskWrapper> TE getHttpTaskWrapper(Class<TE> clazz, String key) {
    final Lock lock = this.lock;
    lock.lock();
    try {
      AbsTaskWrapper wrapper = cache.get(convertKey(key));
      if (wrapper == null || wrapper.getClass() != clazz) {
        INormalTEFactory factory = chooseNormalFactory(clazz);
        if (factory == null) {
          ALog.e(TAG, "任务实体创建失败");
          return null;
        }
        wrapper = factory.create(key);
        cache.put(convertKey(key), wrapper);
      }
      return (TE) wrapper;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 从缓存中获取FTP文件夹任务实体，如果任务实体不存在，则创建任务实体
   *
   * @param key 下载任务，key为下载地址；上传任务，key为本地文件路径
   * @return 创建失败，返回null；成功返回{@link DTaskWrapper}，
   */
  public <TE extends AbsTaskWrapper> TE getFtpTaskWrapper(Class<TE> clazz, String key) {
    final Lock lock = this.lock;
    lock.lock();
    try {
      AbsTaskWrapper tEntity = cache.get(convertKey(key));
      if (tEntity == null || tEntity.getClass() != clazz) {
        IGTEFactory factory = chooseGroupFactory(clazz);
        if (factory == null) {
          ALog.e(TAG, "任务实体创建失败");
          return null;
        }
        tEntity = factory.getFTE(key);
        cache.put(convertKey(key), tEntity);
      }
      return (TE) tEntity;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 从缓存中获取HTTP任务组的任务实体，如果任务实体不存在，则创建任务实体 获取{}
   *
   * @param urls HTTP任务组的子任务下载地址列表
   * @return 地址列表为null或创建实体失败，返回null；成功返回{@link DGTaskWrapper}
   */
  public <TE extends AbsTaskWrapper> TE getDGTaskWrapper(Class<TE> clazz, List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      ALog.e(TAG, "获取HTTP任务组实体失败：任务组的子任务下载地址列表为null");
      return null;
    }
    final Lock lock = this.lock;
    lock.lock();
    try {
      String groupHash = CommonUtil.getMd5Code(urls);
      AbsTaskWrapper tWrapper = cache.get(convertKey(groupHash));
      if (tWrapper == null || tWrapper.getClass() != clazz) {
        IGTEFactory factory = chooseGroupFactory(clazz);
        if (factory == null) {
          ALog.e(TAG, "任务实体创建失败");
          return null;
        }
        tWrapper = factory.getGTE(groupHash, urls);
        cache.put(convertKey(groupHash), tWrapper);
      }
      return (TE) tWrapper;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 更新任务Wrapper
   */
  public void putTaskWrapper(String key, AbsTaskWrapper tEntity) {
    final Lock lock = this.lock;
    lock.lock();
    try {
      cache.put(convertKey(key), tEntity);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 向管理器中增加任务实体
   *
   * @return {@code false} 实体为null，添加失败
   */
  public boolean addTaskWrapper(AbsTaskWrapper te) {
    if (te == null) {
      ALog.e(TAG, "任务实体添加失败");
      return false;
    }
    final Lock lock = this.lock;
    lock.lock();
    try {
      return cache.put(convertKey(te.getKey()), te) != null;
    } finally {
      lock.unlock();
    }
  }

  /**
   * 通过key删除任务实体 当任务complete或删除记录时将删除缓存
   */
  public AbsTaskWrapper removeTaskWrapper(String key) {
    final Lock lock = this.lock;
    lock.lock();
    try {
      return cache.remove(convertKey(key));
    } finally {
      lock.unlock();
    }
  }

  private String convertKey(String key) {
    key = key.trim();
    final Lock lock = this.lock;
    lock.lock();
    try {
      return CommonUtil.keyToHashKey(key);
    } finally {
      lock.unlock();
    }
  }
}
