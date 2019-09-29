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
package com.arialyy.aria.core.task;

import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.loader.ILoaderAdapter;
import com.arialyy.aria.core.wrapper.ITaskWrapper;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * 但文件任务适配器
 *
 * @Author lyy
 * @Date 2019-09-19
 */
public abstract class AbsNormalLoaderAdapter implements ILoaderAdapter {
  protected String TAG = CommonUtil.getClassName(getClass());

  private ITaskWrapper mWrapper;
  private File mTempFile;

  public AbsNormalLoaderAdapter(ITaskWrapper wrapper) {
    mWrapper = wrapper;
    mTempFile = new File(((AbsNormalEntity) wrapper.getEntity()).getFilePath());
  }

  public ITaskWrapper getWrapper() {
    return mWrapper;
  }

  public File getTempFile() {
    return mTempFile;
  }
}
