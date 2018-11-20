package com.arialyy.aria.core.inf;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
    TaskSchedulerType.TYPE_DEFAULT,
    TaskSchedulerType.TYPE_STOP_NOT_NEXT,
    TaskSchedulerType.TYPE_STOP_AND_WAIT
})
@Retention(RetentionPolicy.SOURCE) public @interface TaskSchedulerType {
  int TYPE_DEFAULT = 1;
  /**
   * 停止当前任务并且不自动启动下一任务
   */
  int TYPE_STOP_NOT_NEXT = 2;
  /**
   * 停止任务并让当前任务处于等待状态
   */
  int TYPE_STOP_AND_WAIT = 3;
}