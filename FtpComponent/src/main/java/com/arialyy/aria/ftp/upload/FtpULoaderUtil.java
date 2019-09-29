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
package com.arialyy.aria.ftp.upload;

import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.listener.IUploadListener;
import com.arialyy.aria.core.loader.AbsLoader;
import com.arialyy.aria.core.loader.AbsNormalLoaderUtil;
import com.arialyy.aria.core.loader.NormalLoader;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.ftp.FtpTaskOption;

/**
 * @Author lyy
 * @Date 2019-09-19
 */
public class FtpULoaderUtil extends AbsNormalLoaderUtil {

  public FtpULoaderUtil(DTaskWrapper wrapper, IUploadListener uploadListener) {
    super(wrapper, uploadListener);
    wrapper.generateTaskOption(FtpTaskOption.class);
  }

  @Override protected AbsLoader createLoader() {
    NormalLoader loader = new NormalLoader(getListener(), getTaskWrapper());
    loader.setAdapter(new FtpULoaferAdapter(getTaskWrapper()));
    return null;
  }

  @Override protected Runnable createInfoThread() {
    return new FtpUFileInfoThread((UTaskWrapper) getTaskWrapper(), new OnFileInfoCallback() {
      @Override public void onComplete(String url, CompleteInfo info) {
        if (info.code == FtpUFileInfoThread.CODE_COMPLETE) {
          getListener().onComplete();
        } else {
          getLoader().start();
        }
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        fail(e, needRetry);
      }
    });
  }
}
