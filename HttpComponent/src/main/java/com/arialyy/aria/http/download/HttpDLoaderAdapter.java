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
package com.arialyy.aria.http.download;

import com.arialyy.aria.core.TaskRecord;
import com.arialyy.aria.core.common.RecordHandler;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IRecordHandler;
import com.arialyy.aria.core.task.AbsNormalLoaderAdapter;
import com.arialyy.aria.core.task.IThreadTask;
import com.arialyy.aria.core.task.ThreadTask;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.core.wrapper.ITaskWrapper;
import com.arialyy.aria.http.HttpRecordAdapter;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.BufferedRandomAccessFile;
import com.arialyy.aria.util.FileUtil;
import java.io.File;
import java.io.IOException;

/**
 * @Author lyy
 * @Date 2019-09-21
 */
final class HttpDLoaderAdapter extends AbsNormalLoaderAdapter {
  HttpDLoaderAdapter(ITaskWrapper wrapper) {
    super(wrapper);
  }

  @Override public boolean handleNewTask(TaskRecord record, int totalThreadNum) {
    if (!record.isBlock) {
      if (getTempFile().exists()) {
        FileUtil.deleteFile(getTempFile());
      }
    } else {
      for (int i = 0; i < totalThreadNum; i++) {
        File blockFile =
            new File(String.format(IRecordHandler.SUB_PATH, getTempFile().getPath(), i));
        if (blockFile.exists()) {
          ALog.d(TAG, String.format("分块【%s】已经存在，将删除该分块", i));
          FileUtil.deleteFile(blockFile);
        }
      }
    }
    BufferedRandomAccessFile file = null;
    try {
      if (totalThreadNum > 1 && !record.isBlock) {
        file = new BufferedRandomAccessFile(new File(getTempFile().getPath()), "rwd", 8192);
        //设置文件长度
        file.setLength(getEntity().getFileSize());
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      ALog.e(TAG, String.format("下载失败，filePath: %s, url: %s", getEntity().getFilePath(),
          getEntity().getUrl()));
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return false;
  }

  @Override public IThreadTask createThreadTask(SubThreadConfig config) {
    ThreadTask task = new ThreadTask(config);
    HttpDThreadTaskAdapter adapter = new HttpDThreadTaskAdapter(config);
    task.setAdapter(adapter);
    return task;
  }

  @Override public IRecordHandler recordHandler(AbsTaskWrapper wrapper) {
    RecordHandler recordHandler = new RecordHandler(wrapper);
    HttpRecordAdapter adapter = new HttpRecordAdapter(wrapper);
    recordHandler.setAdapter(adapter);
    return recordHandler;
  }

  private DownloadEntity getEntity() {
    return (DownloadEntity) getWrapper().getEntity();
  }
}
