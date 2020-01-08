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

package com.arialyy.simple.core.upload;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import com.arialyy.annotations.Upload;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.common.HttpOption;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.task.UploadTask;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.frame.util.FileUtil;
import com.arialyy.frame.util.show.L;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.databinding.ActivityUploadBinding;
import com.arialyy.simple.util.AppUtil;
import com.arialyy.simple.widget.HorizontalProgressBarWithNumber;
import java.io.File;

/**
 * Created by lyy on 2017/2/9.
 */
public class HttpUploadActivity extends BaseActivity<ActivityUploadBinding> {
  private static final String TAG = "HttpUploadActivity";
  HorizontalProgressBarWithNumber mPb;

  private final String FILE_PATH = "/mnt/sdcard/QQMusic-import-1.2.1.zip";
  //private final String FILE_PATH =
  //    Environment.getExternalStorageDirectory().getPath() + "/Download/QQMusic-import-1.2.1.zip";
  private UploadEntity mEntity;

  @Override protected int setLayoutId() {
    return R.layout.activity_upload;
  }

  @Override protected void init(Bundle savedInstanceState) {
    setTile("D_HTTP 上传");
    super.init(savedInstanceState);
    Aria.upload(this).getTaskList();

    mEntity = Aria.upload(this).getFirstUploadEntity(FILE_PATH);

    Aria.upload(this).register();
    getBinding().upload.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        upload();
      }
    });
    getBinding().stop.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        stop();
      }
    });
    getBinding().remove.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        remove();
      }
    });
    mPb = getBinding().pb;
  }

  void upload() {
    HttpOption option = new HttpOption();
    option.setRequestType(RequestEnum.POST);
    option.setParam("params", "bbbbbbbb");
    option.setAttachment("file");
    Aria.upload(HttpUploadActivity.this).load(FILE_PATH)
        //.setUploadUrl("http://lib-test.xzxyun.com:8042/Api/upload?data={\"type\":\"1\",\"fileType\":\".apk\"}")
        .setUploadUrl("http://9.9.9.28:5000/upload/")
        .ignoreFilePathOccupy()
        //.setTempUrl("http://192.168.1.6:8080/upload/sign_file/").setAttachment("file")
        //.addHeader("iplanetdirectorypro", "11a09102fb934ad0bc206f9c611d7933")
        .option(option)
        .create();
  }

  void stop() {
    if (AppUtil.chekEntityValid(mEntity)) {
      Aria.upload(this).load(mEntity.getId()).stop();
    }
  }

  void remove() {
    if (AppUtil.chekEntityValid(mEntity)) {
      Aria.upload(this).load(mEntity.getId()).cancel();
    }
  }

  @Upload.onPre public void onPre(UploadTask task) {
  }

  @Upload.onTaskStart public void taskStart(UploadTask task) {
    L.d(TAG, "upload create，md5：" + FileUtil.getFileMD5(new File(task.getEntity().getFilePath())));
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Upload.onTaskStop public void taskStop(UploadTask task) {
    L.d(TAG, "upload stop");
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Upload.onTaskCancel public void taskCancel(UploadTask task) {
    L.d(TAG, "upload cancel");
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Upload.onTaskRunning public void taskRunning(UploadTask task) {
    getBinding().setSpeed(task.getConvertSpeed());
    getBinding().setProgress(task.getPercent());
    L.d(TAG, "running, P = " + task.getPercent());
  }

  @Upload.onTaskComplete public void taskComplete(UploadTask task) {
    L.d(TAG, "上传完成");
    L.d(TAG, "上传成功服务端返回数据（如果有的话）：" + task.getEntity().getResponseStr());
    getBinding().setSpeed("");
    getBinding().setProgress(100);
  }
}
