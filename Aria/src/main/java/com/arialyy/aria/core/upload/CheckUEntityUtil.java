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
package com.arialyy.aria.core.upload;

import android.text.TextUtils;
import com.arialyy.aria.core.inf.ICheckEntityUtil;
import com.arialyy.aria.core.inf.ITaskWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import java.io.File;

public class CheckUEntityUtil implements ICheckEntityUtil {
  private final String TAG = "CheckUEntityUtil";
  private UTaskWrapper mWrapper;
  private UploadEntity mEntity;

  public static CheckUEntityUtil newInstance(UTaskWrapper wrapper) {
    return new CheckUEntityUtil(wrapper);
  }

  private CheckUEntityUtil(UTaskWrapper wrapper) {
    mWrapper = wrapper;
    mEntity = mWrapper.getEntity();
  }

  @Override
  public boolean checkEntity() {
    if (mWrapper.getErrorEvent() != null) {
      ALog.e(TAG, mWrapper.getErrorEvent().errorMsg);
      return false;
    }

    boolean b = checkFtps() && checkUrl() && checkFilePath();
    if (b) {
      mEntity.save();
    }
    return b;
  }

  private boolean checkFilePath() {
    String filePath = mEntity.getFilePath();
    if (TextUtils.isEmpty(filePath)) {
      ALog.e(TAG, "上传失败，文件路径为null");
      return false;
    } else if (!filePath.startsWith("/")) {
      ALog.e(TAG, "上传失败，文件路径【" + filePath + "】不合法");
      return false;
    }

    File file = new File(mEntity.getFilePath());
    if (!file.exists()) {
      ALog.e(TAG, "上传失败，文件【" + filePath + "】不存在");
      return false;
    }
    if (file.isDirectory()) {
      ALog.e(TAG, "上传失败，文件【" + filePath + "】不能是文件夹");
      return false;
    }
    return true;
  }

  private boolean checkUrl() {

    final String url = mWrapper.getTempUrl();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "上传失败，url为null");
      return false;
    } else if (!CheckUtil.checkUrlNotThrow(url)) {
      ALog.e(TAG, "上传失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "上传失败，url【" + url + "】不合法");
      return false;
    }
    mEntity.setUrl(url);
    return true;
  }

  private boolean checkFtps() {
    //if (mWrapper.getRequestType() == ITaskWrapper.U_FTP && mWrapper.asFtp()
    //    .getUrlEntity().isFtps) {
    //  String ftpUrl = mEntity.getUrl();
    //  if (!ftpUrl.startsWith("ftps") && !ftpUrl.startsWith("sftp")) {
    //    ALog.e(TAG, String.format("地址【%s】错误，ftps地址开头必须是：ftps或sftp", ftpUrl));
    //    return false;
    //  }
    //}
    return true;
  }
}
