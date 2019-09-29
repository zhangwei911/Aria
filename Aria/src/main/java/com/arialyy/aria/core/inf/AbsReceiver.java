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

package com.arialyy.aria.core.inf;

import com.arialyy.aria.core.queue.DGroupTaskQueue;
import com.arialyy.aria.core.queue.DTaskQueue;
import com.arialyy.aria.core.queue.UTaskQueue;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by AriaL on 2017/6/27.
 * 接收器
 */
public abstract class AbsReceiver implements IReceiver {
  /**
   * 观察者对象map
   * key 由 {@link #getKey(IReceiver)}指定
   */
  public static final Map<String, Object> OBJ_MAP = new ConcurrentHashMap<>();
  /**
   * 观察者对象类的完整名称
   */
  public String targetName;
  /**
   * 当dialog、dialogFragment、popupwindow已经被用户使用了Dismiss事件或Cancel事件，需要手动移除receiver
   */
  public boolean needRmListener = false;

  /**
   * 创建观察者对象map的key，生成规则：
   * {@link #targetName}_{@code download}{@code upload}_{@link #hashCode()}
   *
   * @param receiver 当前接收器
   * @return 返回key
   */
  public static String getKey(IReceiver receiver) {
    return String.format("%s_%s_%s", receiver.getTargetName(), receiver.getType(),
        receiver.hashCode());
  }

  @Override public String getTargetName() {
    return targetName;
  }

  /**
   * 获取当前Receiver的key
   */
  @Override public String getKey() {
    return getKey(this);
  }

  /**
   * 移除观察者对象
   */
  private void removeObj() {
    for (Iterator<Map.Entry<String, Object>> iter = OBJ_MAP.entrySet().iterator();
        iter.hasNext(); ) {
      Map.Entry<String, Object> entry = iter.next();
      String key = entry.getKey();
      if (key.equals(getKey())) {
        iter.remove();
      }
    }
  }

  @Override public void destroy() {
    unRegisterListener();
    removeObj();
  }

  /**
   * 移除{@link DTaskQueue}、{@link DGroupTaskQueue}、{@link UTaskQueue}中注册的观察者
   */
  protected abstract void unRegisterListener();
}
