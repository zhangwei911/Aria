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

import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

/**
 * 获取sftp文件信息
 *
 * @author lyy
 */
public class AbsSFtpFileInfoThread<WP extends AbsTaskWrapper> implements Runnable {

  private WP mWrapper;
  private SFtpUtil mUtil;
  private OnFileInfoCallback mCallback;

  public AbsSFtpFileInfoThread(SFtpUtil util, WP wrapper, OnFileInfoCallback callback) {
    mWrapper = wrapper;
    mUtil = util;
    mCallback = callback;
  }

  @Override public void run() {
    startFlow();
  }

  private void startFlow() {

  }

  private ChannelSftp createChannel() {
    ChannelSftp sftp = null;
    try {
      sftp = (ChannelSftp) mUtil.getSession().openChannel(SFtpUtil.CMD_TYPE_SFTP);
      sftp.connect();
    } catch (JSchException e) {
      e.printStackTrace();
    }
    return sftp;
  }
}
