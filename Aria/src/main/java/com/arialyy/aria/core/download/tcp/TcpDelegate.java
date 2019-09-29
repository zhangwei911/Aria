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
package com.arialyy.aria.core.download.tcp;

import android.text.TextUtils;
import androidx.annotation.CheckResult;
import com.arialyy.aria.core.common.BaseDelegate;
import com.arialyy.aria.core.inf.Suggest;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import java.nio.charset.Charset;

/**
 * @Author aria
 * @Date 2019-09-06
 */
public class TcpDelegate<TARGET extends AbsTarget> extends BaseDelegate<TARGET> {

  private TcpTaskConfig mTcpConfig;

  public TcpDelegate(TARGET target, AbsTaskWrapper wrapper) {
    super(target, wrapper);

    mTcpConfig = (TcpTaskConfig) wrapper.getTaskOption();
  }

  /**
   * 上传给tcp服务的初始数据，一般是文件名、文件路径等信息
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public TcpDelegate<TARGET> setParam(String params) {
    if (TextUtils.isEmpty(params)) {
      ALog.w(TAG, "tcp传输的数据不能为空");
      return this;
    }
    mTcpConfig.setParams(params);
    return this;
  }

  /**
   * 设置心跳包传输的数据
   *
   * @param heartbeatInfo 心跳包数据
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public TcpDelegate<TARGET> setHeartbeatInfo(String heartbeatInfo) {
    if (TextUtils.isEmpty(heartbeatInfo)) {
      ALog.w(TAG, "心跳包传输的数据不能为空");
      return this;
    }
    mTcpConfig.setHeartbeat(heartbeatInfo);
    return this;
  }

  /**
   * 心跳间隔，默认1s
   *
   * @param interval 单位毫秒
   */
  @CheckResult(suggest = Suggest.TO_CONTROLLER)
  public TcpDelegate<TARGET> setHeartbeatInterval(long interval) {
    if (interval <= 0) {
      ALog.w(TAG, "心跳间隔不能小于1毫秒");
      return this;
    }
    mTcpConfig.setHeartbeatInterval(interval);
    return this;
  }

  /**
   * 数据传输编码，默认"utf-8"
   */
  public TcpDelegate<TARGET> setCharset(String charset) {
    if (!Charset.isSupported(charset)) {
      ALog.w(TAG, "不支持的编码");
      return this;
    }

    mTcpConfig.setCharset(charset);
    return this;
  }
}
