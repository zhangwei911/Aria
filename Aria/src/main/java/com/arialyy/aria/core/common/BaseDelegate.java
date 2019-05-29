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

import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.ITargetHandler;
import com.arialyy.aria.util.CommonUtil;

public abstract class BaseDelegate<TARGET extends AbsTarget> implements ITargetHandler {
  protected TARGET mTarget;
  protected final String TAG;

  public BaseDelegate(TARGET target) {
    mTarget = target;
    TAG = CommonUtil.getClassName(getClass());
  }

  @Override public void add() {
    mTarget.add();
  }

  @Override public void start() {
    mTarget.start();
  }

  @Override public void stop() {
    mTarget.stop();
  }

  @Override public void resume() {
    mTarget.resume();
  }

  @Override public void cancel() {
    mTarget.cancel();
  }

  @Override public void save() {
    mTarget.save();
  }

  @Override public void cancel(boolean removeFile) {
    mTarget.cancel(removeFile);
  }

  @Override public void reTry() {
    mTarget.reTry();
  }

  @Override public void reStart() {
    mTarget.reStart();
  }
}
