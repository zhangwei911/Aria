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
package com.arialyy.aria.core.common.controller;

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.download.CheckDEntityUtil;
import com.arialyy.aria.core.download.CheckDGEntityUtil;
import com.arialyy.aria.core.download.CheckFtpDirEntityUtil;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.ICheckEntityUtil;
import com.arialyy.aria.core.inf.ITask;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.core.scheduler.DownloadGroupSchedulers;
import com.arialyy.aria.core.scheduler.DownloadSchedulers;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.core.scheduler.UploadSchedulers;
import com.arialyy.aria.core.upload.CheckUEntityUtil;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 功能控制器
 */
public abstract class FeatureController {

  private AbsTaskWrapper mTaskWrapper;

  FeatureController(AbsTaskWrapper wrapper) {
    mTaskWrapper = wrapper;
  }

  /**
   * 使用对应等控制器，注意：
   * 1、对于不存在的任务（第一次下载），只能使用{@link ControllerType#START_CONTROLLER}
   * 2、对于已存在的任务，只能使用{@link ControllerType#NORMAL_CONTROLLER}
   *
   * @param clazz {@link ControllerType#START_CONTROLLER}、{@link ControllerType#NORMAL_CONTROLLER}
   */
  public static <T extends FeatureController> T newInstance(@ControllerType Class<T> clazz,
      AbsTaskWrapper wrapper) {
    if (wrapper.getEntity().getId() == -1 && clazz != ControllerType.START_CONTROLLER) {
      throw new IllegalArgumentException("对于不存在的任务（第一次下载），只能使用\"ControllerType.START_CONTROLLER\"");
    }
    if (wrapper.getEntity().getId() != -1 && clazz != ControllerType.NORMAL_CONTROLLER) {
      throw new IllegalArgumentException(
          "对于已存在的任务，只能使用\" ControllerType.NORMAL_CONTROLLER\"，请检查是否重复调用#start()方法");
    }

    Class[] paramTypes = { AbsTaskWrapper.class };
    Object[] params = { wrapper };
    try {
      Constructor<T> con = clazz.getConstructor(paramTypes);
      return con.newInstance(params);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  protected AbsTaskWrapper getTaskWrapper() {
    return mTaskWrapper;
  }

  protected AbsEntity getEntity() {
    return mTaskWrapper.getEntity();
  }

  int checkTaskType() {
    int taskType = 0;
    if (mTaskWrapper instanceof DTaskWrapper) {
      taskType = ITask.DOWNLOAD;
    } else if (mTaskWrapper instanceof DGTaskWrapper) {
      taskType = ITask.DOWNLOAD_GROUP;
    } else if (mTaskWrapper instanceof UTaskWrapper) {
      taskType = ITask.UPLOAD;
    }
    return taskType;
  }

  /**
   * 如果检查实体失败，将错误回调
   */
  boolean checkConfig() {
    boolean b = checkEntity();
    ISchedulers schedulers = getScheduler();
    if (!b && schedulers != null) {
      new Handler(Looper.getMainLooper(), schedulers).obtainMessage(ISchedulers.CHECK_FAIL,
          checkTaskType(), -1, null).sendToTarget();
    }

    return b;
  }

  private ISchedulers getScheduler() {
    if (mTaskWrapper instanceof DTaskWrapper) {
      return DownloadSchedulers.getInstance();
    }
    if (mTaskWrapper instanceof UTaskWrapper) {
      return UploadSchedulers.getInstance();
    }
    if (mTaskWrapper instanceof DGTaskWrapper) {
      return DownloadGroupSchedulers.getInstance();
    }
    return null;
  }

  private boolean checkEntity() {
    ICheckEntityUtil checkUtil = null;
    if (mTaskWrapper instanceof DTaskWrapper) {
      checkUtil = CheckDEntityUtil.newInstance((DTaskWrapper) mTaskWrapper);
    } else if (mTaskWrapper instanceof DGTaskWrapper) {
      if (mTaskWrapper.getRequestType() == ITaskWrapper.D_FTP_DIR) {
        checkUtil = CheckFtpDirEntityUtil.newInstance((DGTaskWrapper) mTaskWrapper);
      } else if (mTaskWrapper.getRequestType() == ITaskWrapper.DG_HTTP) {
        checkUtil = CheckDGEntityUtil.newInstance((DGTaskWrapper) mTaskWrapper);
      }
    } else if (mTaskWrapper instanceof UTaskWrapper) {
      checkUtil = CheckUEntityUtil.newInstance((UTaskWrapper) mTaskWrapper);
    }
    return checkUtil != null && checkUtil.checkEntity();
  }
}
