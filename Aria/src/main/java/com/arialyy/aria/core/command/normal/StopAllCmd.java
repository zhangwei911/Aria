package com.arialyy.aria.core.command.normal;

import com.arialyy.aria.core.inf.AbsTaskEntity;

/**
 * Created by AriaL on 2017/6/13.
 * 停止所有任务的命令，并清空所有等待队列
 */
final class StopAllCmd<T extends AbsTaskEntity> extends AbsNormalCmd<T> {
  StopAllCmd(T entity, int taskType) {
    super(entity, taskType);
  }

  @Override public void executeCmd() {
    stopAll();
  }
}
