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

import com.arialyy.aria.core.common.AbsNormalTarget;
import java.util.List;

/**
 * @Author aria
 * @Date 2019-09-05
 */
public class GroupTargetFactory {

  public static volatile GroupTargetFactory INSTANCE;

  private GroupTargetFactory() {

  }

  public static GroupTargetFactory getInstance() {

    if (INSTANCE == null) {
      synchronized (GroupTargetFactory.class) {
        if (INSTANCE == null) {
          INSTANCE = new GroupTargetFactory();
        }
      }
    }

    return INSTANCE;
  }

  public <T extends AbsNormalTarget> T generateNormalTarget(Class<T> clazz, long taskId,
      String targetName) {
    T target = null;
    if (clazz == GroupNormalTarget.class) {
      target = (T) new GroupNormalTarget(taskId, targetName);
    } else if (clazz == FtpDirNormalTarget.class) {
      target = (T) new FtpDirNormalTarget(taskId, targetName);
    }

    return target;
  }

  public FtpDirBuilderTarget generateDirBuilderTarget(String url, String targetName) {
    return new FtpDirBuilderTarget(url, targetName);
  }

  public GroupBuilderTarget generateGroupBuilderTarget(List<String> urls, String targetName) {
    return new GroupBuilderTarget(urls, targetName);
  }
}
