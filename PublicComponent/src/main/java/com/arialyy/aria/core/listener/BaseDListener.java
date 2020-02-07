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
package com.arialyy.aria.core.listener;

import android.os.Handler;

import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.loader.IRecordHandler;
import com.arialyy.aria.core.inf.TaskSchedulerType;
import com.arialyy.aria.core.task.AbsTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.RecordUtil;

/**
 * 下载监听类
 */
public class BaseDListener extends BaseListener<DownloadEntity, DTaskWrapper, AbsTask<DTaskWrapper>>
        implements IDLoadListener {

    public BaseDListener(AbsTask<DTaskWrapper> task, Handler outHandler) {
        super(task, outHandler);
    }

    @Override
    public void onPostPre(long fileSize) {
        mEntity.setFileSize(fileSize);
        mEntity.setConvertFileSize(CommonUtil.formatFileSize(fileSize));
        saveData(IEntity.STATE_POST_PRE, -1);
        sendInState2Target(ISchedulers.POST_PRE);
    }

    @Override
    public void supportBreakpoint(boolean support) {
        if (!support) {
            sendInState2Target(ISchedulers.NO_SUPPORT_BREAK_POINT);
        }
    }

    @Override
    protected void handleCancel() {
        int sType = getTask().getSchedulerType();
        if (sType == TaskSchedulerType.TYPE_CANCEL_AND_NOT_NOTIFY) {
            mEntity.setComplete(false);
            mEntity.setState(IEntity.STATE_WAIT);
            RecordUtil.delTaskRecord(mEntity.getFilePath(), IRecordHandler.TYPE_DOWNLOAD,
                    mTaskWrapper.isRemoveFile(), false);
        } else {
            RecordUtil.delTaskRecord(mEntity.getFilePath(), IRecordHandler.TYPE_DOWNLOAD,
                    mTaskWrapper.isRemoveFile(), true);
        }
    }
}