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
package com.arialyy.aria.sftp;

import com.arialyy.aria.core.AriaConfig;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.AbsNormalEntity;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.ftp.FtpTaskOption;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.util.Vector;

/**
 * https://cloud.tencent.com/developer/article/1354612
 *
 * @author lyy
 */
public class SFtpInfoThread<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Runnable {

  private final String TAG = CommonUtil.getClassName(getClass());
  protected ENTITY mEntity;
  protected TASK_WRAPPER mTaskWrapper;
  protected FtpTaskOption mTaskOption;
  private int mConnectTimeOut;
  protected OnFileInfoCallback mCallback;
  protected long mSize = 0;
  protected String charSet = "UTF-8";
  private boolean isUpload = false;
  private SFtpUtil mSFtpUtil;
  private BaseInfoThreadAdapter mAdapter;

  public SFtpInfoThread(SFtpUtil ftpUtil, TASK_WRAPPER taskWrapper,
      OnFileInfoCallback callback) {
    mSFtpUtil = ftpUtil;
    mTaskWrapper = taskWrapper;
    mEntity = taskWrapper.getEntity();
    mTaskOption = (FtpTaskOption) taskWrapper.getTaskOption();
    mConnectTimeOut = AriaConfig.getInstance().getDConfig().getConnectTimeOut();
    mCallback = callback;
    if (mEntity instanceof UploadEntity) {
      isUpload = true;
    }
  }

  public void setAdapter(BaseInfoThreadAdapter adapter) {
    mAdapter = adapter;
  }

  @Override public void run() {
    if (mAdapter == null) {
      ALog.e(TAG, "adapter为空");
      return;
    }
    try {
      ChannelSftp channelSftp =
          (ChannelSftp) mSFtpUtil.getSession().openChannel(SFtpUtil.CMD_TYPE_SFTP);
      Vector files = channelSftp.ls(getUrlEntity().remotePath);

      if (files.isEmpty()) {
        ALog.e(TAG, String.format("路径【%s】没有文件", getUrlEntity().remotePath));
        mCallback.onFail(mEntity, null, false);
        return;
      }

      if (!mAdapter.handlerFile(files)) {
        ALog.e(TAG, "文件处理失败");
        mCallback.onFail(mEntity, null, false);
        return;
      }
    } catch (JSchException ex) {
      ex.printStackTrace();
    } catch (SftpException e) {
      e.printStackTrace();
    }
  }

  private FtpUrlEntity getUrlEntity() {
    return mTaskOption.getUrlEntity();
  }
}
