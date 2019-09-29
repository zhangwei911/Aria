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

import com.arialyy.aria.core.command.CancelCmd;
import com.arialyy.aria.core.command.NormalCmdFactory;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.core.command.CmdHelper;

/**
 * 启动控制器
 */
public final class NormalController extends FeatureController implements INormalFeature {
  private String TAG = "NormalController";

  public NormalController(AbsTaskWrapper wrapper) {
    super(wrapper);
  }

  /**
   * 停止任务
   */
  @Override
  public void stop() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_STOP,
              checkTaskType()));
    }
  }

  /**
   * 恢复任务
   */
  @Override
  public void resume() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_START,
              checkTaskType()));
    }
  }

  /**
   * 删除任务
   */
  @Override
  public void cancel() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_CANCEL,
              checkTaskType()));
    }
  }

  /**
   * 任务重试
   */
  @Override
  public void reTry() {
    if (checkConfig()) {
      int taskType = checkTaskType();
      EventMsgUtil.getDefault()
          .post(CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_STOP, taskType));
      EventMsgUtil.getDefault()
          .post(
              CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_START, taskType));
    }
  }

  /**
   * 删除任务
   *
   * @param removeFile {@code true} 不仅删除任务数据库记录，还会删除已经删除完成的文件
   * {@code false}如果任务已经完成，只删除任务数据库记录，
   */
  @Override
  public void cancel(boolean removeFile) {
    if (checkConfig()) {
      CancelCmd cancelCmd =
          (CancelCmd) CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_CANCEL,
              checkTaskType());
      cancelCmd.removeFile = removeFile;
      EventMsgUtil.getDefault().post(cancelCmd);
    }
  }

  /**
   * 重新下载
   */
  @Override
  public void reStart() {
    if (checkConfig()) {
      EventMsgUtil.getDefault()
          .post(CmdHelper.createNormalCmd(getTaskWrapper(), NormalCmdFactory.TASK_RESTART,
              checkTaskType()));
    }
  }

  @Override public void save() {
    if (!checkConfig()) {
      ALog.e(TAG, "保存修改失败");
    } else {
      ALog.i(TAG, "保存成功");
    }
  }
}
