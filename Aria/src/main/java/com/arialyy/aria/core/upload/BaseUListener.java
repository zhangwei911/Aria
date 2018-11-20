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

import android.os.Handler;
import com.arialyy.aria.core.common.BaseListener;
import com.arialyy.aria.core.common.TaskRecord;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.IUploadListener;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CommonUtil;

/**
 * 下载监听类
 */
class BaseUListener extends BaseListener<UploadEntity, UploadTaskEntity, UploadTask>
    implements IUploadListener {

  BaseUListener(UploadTask task, Handler outHandler) {
    super(task, outHandler);
    isConvertSpeed = manager.getUploadConfig().isConvertSpeed();
    mUpdateInterval = manager.getUploadConfig().getUpdateInterval();
  }

  @Override
  protected void saveData(int state, long location) {
    mTaskEntity.setState(state);
    mEntity.setState(state);
    mEntity.setComplete(state == IEntity.STATE_COMPLETE);
    if (state == IEntity.STATE_CANCEL) {
      if (mEntity instanceof UploadEntity) {
        TaskRecord record =
            DbEntity.findFirst(TaskRecord.class, "TaskRecord.filePath=?", mTaskEntity.getKey());
        if (record != null) {
          CommonUtil.delTaskRecord(record, mTaskEntity.isRemoveFile(), mEntity);
        }
      }
      //else if (mEntity instanceof AbsGroupEntity) {
      //  CommonUtil.delGroupTaskRecord(mTaskEntity.isRemoveFile(), ((AbsGroupEntity) mEntity));
      //}
      return;
    } else if (state == IEntity.STATE_STOP) {
      mEntity.setStopTime(System.currentTimeMillis());
    } else if (mEntity.isComplete()) {
      mEntity.setCompleteTime(System.currentTimeMillis());
      mEntity.setCurrentProgress(mEntity.getFileSize());
    } else if (location > 0) {
      mEntity.setCurrentProgress(location);
    }
    mTaskEntity.update();
  }
}