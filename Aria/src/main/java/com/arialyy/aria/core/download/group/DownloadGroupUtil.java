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
package com.arialyy.aria.core.download.group;

import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组下载工具
 */
public class DownloadGroupUtil extends AbsGroupUtil implements IUtil {

  public DownloadGroupUtil(IDownloadGroupListener listener, DGTaskWrapper taskWrapper) {
    super(listener, taskWrapper);
  }

  @Override int getTaskType() {
    return HTTP_GROUP;
  }

  @Override public void onCancel() {
    super.onCancel();
  }

  @Override protected void onStop() {
    super.onStop();
  }

  @Override protected void onStart() {
    super.onStart();
    if (mState.getCompleteNum() == mState.getSubSize()) {
      mListener.onComplete();
    } else {
      for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
        if (wrapper.getState() != IEntity.STATE_COMPLETE) {
          createSubLoader(wrapper);
        }
      }
    }
  }
}