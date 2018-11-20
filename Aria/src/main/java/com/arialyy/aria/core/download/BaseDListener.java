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
package com.arialyy.aria.core.download;

import android.os.Handler;
import com.arialyy.aria.core.common.BaseListener;
import com.arialyy.aria.core.common.TaskRecord;
import com.arialyy.aria.core.inf.IDownloadListener;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CommonUtil;

/**
 * 下载监听类
 */
class BaseDListener extends BaseListener<DownloadEntity, DownloadTaskEntity, DownloadTask>
    implements IDownloadListener {
  private static final String TAG = "BaseDListener";

  BaseDListener(DownloadTask task, Handler outHandler) {
    super(task, outHandler);
    isConvertSpeed = manager.getDownloadConfig().isConvertSpeed();
    mUpdateInterval = manager.getDownloadConfig().getUpdateInterval();
  }

  @Override public void onPostPre(long fileSize) {
    mEntity.setFileSize(fileSize);
    mEntity.setConvertFileSize(CommonUtil.formatFileSize(fileSize));
    saveData(IEntity.STATE_POST_PRE, -1);
    sendInState2Target(ISchedulers.POST_PRE);
  }

  @Override public void supportBreakpoint(boolean support) {

  }

  @Override
  protected void saveData(int state, long location) {
    mTaskEntity.setState(state);
    mEntity.setState(state);
    mEntity.setComplete(state == IEntity.STATE_COMPLETE);
    if (state == IEntity.STATE_CANCEL) {
      if (mEntity instanceof DownloadEntity) {
        TaskRecord record =
            DbEntity.findFirst(TaskRecord.class, "TaskRecord.filePath=?", mTaskEntity.getKey());
        if (record != null) {
          CommonUtil.delTaskRecord(record, mTaskEntity.isRemoveFile(), mEntity);
        } else {
          mEntity.deleteData();
        }
      }
      return;
    } else if (state == IEntity.STATE_STOP) {
      mEntity.setStopTime(System.currentTimeMillis());
    } else if (mEntity.isComplete()) {
      mEntity.setCompleteTime(System.currentTimeMillis());
      mEntity.setCurrentProgress(mEntity.getFileSize());
    }
    if (location > 0) {
      mEntity.setCurrentProgress(location);
    }
    mTaskEntity.update();
  }
}