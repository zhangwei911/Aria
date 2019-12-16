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

package com.arialyy.simple.core.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.common.HttpOption;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.listener.ISchedulers;
import com.arialyy.aria.core.processor.IHttpFileLenAdapter;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.FileUtil;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.common.ModifyPathDialog;
import com.arialyy.simple.common.ModifyUrlDialog;
import com.arialyy.simple.databinding.ActivitySingleBinding;
import com.arialyy.simple.util.AppUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SingleTaskActivity extends BaseActivity<ActivitySingleBinding> {

  private String mUrl;
  private String mFilePath;
  private HttpDownloadModule mModule;
  private long mTaskId = -1;

  BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(ISchedulers.ARIA_TASK_INFO_ACTION)) {
        ALog.d(TAG, "state = " + intent.getIntExtra(ISchedulers.TASK_STATE, -1));
        ALog.d(TAG, "type = " + intent.getIntExtra(ISchedulers.TASK_TYPE, -1));
        ALog.d(TAG, "speed = " + intent.getLongExtra(ISchedulers.TASK_SPEED, -1));
        ALog.d(TAG, "percent = " + intent.getIntExtra(ISchedulers.TASK_PERCENT, -1));
        ALog.d(TAG, "entity = " + intent.getParcelableExtra(ISchedulers.TASK_ENTITY).toString());
      }
    }
  };

  @Override
  protected void onResume() {
    super.onResume();
    //registerReceiver(receiver, new IntentFilter(ISchedulers.ARIA_TASK_INFO_ACTION));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    //unregisterReceiver(receiver);
    Aria.download(this).unRegister();
  }

  @Override
  protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    setTitle("单任务下载");
    Aria.download(this).register();
    mModule = ViewModelProviders.of(this).get(HttpDownloadModule.class);
    mModule.getHttpDownloadInfo(this).observe(this, new Observer<DownloadEntity>() {

      @Override public void onChanged(@Nullable DownloadEntity entity) {
        if (entity == null) {
          return;
        }
        mTaskId = entity.getId();
        if (entity.getState() == IEntity.STATE_STOP) {
          getBinding().setStateStr(getString(R.string.resume));
        } else if (entity.getState() == IEntity.STATE_RUNNING) {
          getBinding().setStateStr(getString(R.string.stop));
        }

        if (entity.getFileSize() != 0) {
          getBinding().setFileSize(CommonUtil.formatFileSize(entity.getFileSize()));
          getBinding().setProgress(entity.isComplete() ? 100
              : (int) (entity.getCurrentProgress() * 100 / entity.getFileSize()));
        }
        getBinding().setUrl(entity.getUrl());
        getBinding().setFilePath(entity.getFilePath());
        mUrl = entity.getUrl();
        mFilePath = entity.getFilePath();
      }
    });
    getBinding().setViewModel(this);
  }

  public void chooseUrl() {
    ModifyUrlDialog dialog =
        new ModifyUrlDialog(this, getString(R.string.modify_url_dialog_title), mUrl);
    dialog.show(getSupportFragmentManager(), "ModifyUrlDialog");
  }

  public void chooseFilePath() {
    ModifyPathDialog dialog =
        new ModifyPathDialog(this, getString(R.string.modify_file_path), mFilePath);
    dialog.show(getSupportFragmentManager(), "ModifyPathDialog");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_single_task_activity, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    int speed = -1;
    String msg = "";
    switch (item.getItemId()) {
      case R.id.help:
        msg = "一些小知识点：\n"
            + "1、你可以在注解中增加链接，用于指定被注解的方法只能被特定的下载任务回调，以防止progress乱跳\n"
            + "2、当遇到网络慢的情况时，你可以先使用onPre()更新UI界面，待连接成功时，再在onTaskPre()获取完整的task数据，然后给UI界面设置正确的数据\n"
            + "3、你可以在界面初始化时通过Aria.download(this).load(URL).getPercent()等方法快速获取相关任务的一些数据";
        showMsgDialog("tip", msg);
        break;
      case R.id.speed_0:
        speed = 0;
        break;
      case R.id.speed_128:
        speed = 128;
        break;
      case R.id.speed_256:
        speed = 256;
        break;
      case R.id.speed_512:
        speed = 512;
        break;
      case R.id.speed_1m:
        speed = 1024;
        break;
    }
    if (speed > -1) {
      msg = item.getTitle().toString();
      Aria.download(this).setMaxSpeed(speed);
      T.showShort(this, msg);
    }
    return true;
  }

  @Download.onWait
  void onWait(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      Log.d(TAG, "wait ==> " + task.getDownloadEntity().getFileName());
    }
  }

  @Download.onPre
  protected void onPre(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      getBinding().setStateStr(getString(R.string.stop));
    }
  }

  @Download.onTaskStart
  void taskStart(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      getBinding().setFileSize(task.getConvertFileSize());
      ALog.d(TAG, "isComplete = " + task.isComplete() + ", state = " + task.getState());
    }
  }

  @Download.onTaskRunning
  protected void running(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      ALog.d(TAG, "isRunning");
      //Log.d(TAG, task.getKey());
      long len = task.getFileSize();
      if (len == 0) {
        getBinding().setProgress(0);
      } else {
        getBinding().setProgress(task.getPercent());
      }
      getBinding().setSpeed(task.getConvertSpeed());
    }
  }

  @Download.onTaskResume
  void taskResume(DownloadTask task) {
    ALog.d(TAG, "resume");
    if (task.getKey().equals(mUrl)) {
      getBinding().setStateStr(getString(R.string.stop));
    }
  }

  @Download.onTaskStop
  void taskStop(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      getBinding().setStateStr(getString(R.string.resume));
      getBinding().setSpeed("");
    }
  }

  @Download.onTaskCancel
  void taskCancel(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      mTaskId = -1;
      getBinding().setProgress(0);
      getBinding().setStateStr(getString(R.string.start));
      getBinding().setSpeed("");
      Log.d(TAG, "cancel");
    }
  }

  @Download.onTaskFail
  void taskFail(DownloadTask task, Exception e) {
    Toast.makeText(SingleTaskActivity.this, getString(R.string.download_fail), Toast.LENGTH_SHORT)
        .show();
    if (task != null && task.getKey().equals(mUrl)) {
      getBinding().setStateStr(getString(R.string.start));
    }
  }

  @Download.onTaskComplete
  void taskComplete(DownloadTask task) {
    if (task.getKey().equals(mUrl)) {
      getBinding().setProgress(100);
      Toast.makeText(SingleTaskActivity.this, getString(R.string.download_success),
          Toast.LENGTH_SHORT).show();
      getBinding().setStateStr(getString(R.string.re_start));
      getBinding().setSpeed("");
      ALog.d(TAG, "md5: " + CommonUtil.getFileMD5(new File(task.getFilePath())));
    }
  }

  @Override
  protected int setLayoutId() {
    return R.layout.activity_single;
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (mTaskId == -1) {
          startD();
          break;
        }
        if (Aria.download(this).load(mTaskId).isRunning()) {
          Aria.download(this)
              .load(mTaskId)
              .stop();
        } else {
          mTaskId = Aria.download(this).load(mTaskId)
              //.updateUrl(mUrl)
              .reStart();
        }
        break;
      case R.id.cancel:
        Aria.download(this).load(mTaskId).cancel(true);
        break;
    }
  }

  private void startD() {
    HttpOption option = new HttpOption();
    option.addHeader("1", "@")
        .useServerFileName(true)
        .setFileLenAdapter(new FileLenAdapter());
    //option.setRequestType(RequestEnum.POST);
    mTaskId = Aria.download(SingleTaskActivity.this)
        .load(mUrl)
        .setFilePath(mFilePath, true)
        .option(option)
        .ignoreCheckPermissions()
        .create();
  }

  @Override
  protected void onStop() {
    super.onStop();
    //Aria.download(this).unRegister();
  }

  @Override public boolean dispatchTouchEvent(MotionEvent ev) {
    return super.dispatchTouchEvent(ev);
  }

  @Override protected void dataCallback(int result, Object data) {
    super.dataCallback(result, data);
    if (result == ModifyUrlDialog.MODIFY_URL_DIALOG_RESULT) {
      mModule.uploadUrl(this, String.valueOf(data));
    } else if (result == ModifyPathDialog.MODIFY_PATH_RESULT) {
      mModule.updateFilePath(this, String.valueOf(data));
    }
  }

  static class FileLenAdapter implements IHttpFileLenAdapter {
    @Override public long handleFileLen(Map<String, List<String>> headers) {

      List<String> sLength = headers.get("Content-Length");
      if (sLength == null || sLength.isEmpty()) {
        return -1;
      }
      String temp = sLength.get(0);

      return Long.parseLong(temp);
    }
  }
}