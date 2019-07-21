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

import android.support.annotation.CheckResult;
import com.arialyy.aria.core.common.controller.ControllerType;
import com.arialyy.aria.core.common.controller.FeatureController;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.CommonUtil;

public abstract class BaseDelegate<TARGET extends AbsTarget> {
  protected final String TAG;
  protected TARGET mTarget;
  protected AbsTaskWrapper mWrapper;

  public BaseDelegate(TARGET target, AbsTaskWrapper wrapper) {
    TAG = CommonUtil.getClassName(getClass());
    mTarget = target;
    mWrapper = wrapper;
  }

  protected AbsTaskWrapper getTaskWrapper() {
    return mWrapper;
  }

  /**
   * 使用对应等控制器，注意：
   * 1、对于不存在的任务（第一次下载），只能使用{@link ControllerType#START_CONTROLLER}
   * 2、对于已存在的任务，只能使用{@link ControllerType#NORMAL_CONTROLLER}
   *
   * @param clazz {@link ControllerType#START_CONTROLLER}、{@link ControllerType#NORMAL_CONTROLLER}
   */
  @CheckResult(suggest = Suggest.TASK_CONTROLLER)
  public <T extends FeatureController> T controller(@ControllerType Class<T> clazz) {
    return FeatureController.newInstance(clazz, getTaskWrapper());
  }
}
