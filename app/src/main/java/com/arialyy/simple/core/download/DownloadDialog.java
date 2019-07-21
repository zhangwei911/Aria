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

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.core.AbsDialog;
import com.arialyy.simple.R;
import com.arialyy.simple.util.AppUtil;
import com.arialyy.simple.widget.HorizontalProgressBarWithNumber;

/**
 * Created by AriaL on 2017/1/2.
 */
public class DownloadDialog extends AbsDialog implements View.OnClickListener {
  private HorizontalProgressBarWithNumber mPb;
  private Button mStart;
  private Button mCancel;
  private TextView mSize;
  private TextView mSpeed;
  private DownloadEntity mEntity;

  private static final String DOWNLOAD_URL =
      "http://static.gaoshouyou.com/d/4b/d7/e04b308d9cd7f0ad4cac18d1a514544c.apk";

  public DownloadDialog(Context context) {
    super(context);
    init();
  }

  @Override protected int setLayoutId() {
    return R.layout.dialog_download;
  }

  private void init() {
    Aria.download(this).register();
    mPb = findViewById(R.id.progressBar);
    mStart = findViewById(R.id.start);
    mCancel = findViewById(R.id.cancel);
    mSize = findViewById(R.id.size);
    mSpeed = findViewById(R.id.speed);
    mEntity = Aria.download(this).getFirstDownloadEntity(DOWNLOAD_URL);
    if (mEntity != null) {
      mSize.setText(CommonUtil.formatFileSize(mEntity.getFileSize()));
      int p = (int) (mEntity.getCurrentProgress() * 100 / mEntity.getFileSize());
      mPb.setProgress(p);
      int state = mEntity.getState();
      setBtState(state != DownloadEntity.STATE_RUNNING);
    } else {
      setBtState(true);
    }
    mStart.setOnClickListener(this);
    mCancel.setOnClickListener(this);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (!AppUtil.chekEntityValid(mEntity)) {
          Aria.download(this)
              .load(DOWNLOAD_URL)
              .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/飞机大战.apk")
              .start();
          mStart.setText(getContext().getString(R.string.stop));
          break;
        }
        if (Aria.download(this).load(mEntity.getId()).isRunning()) {
          Aria.download(this).load(mEntity.getId()).stop();
          mStart.setText(getContext().getString(R.string.resume));
        } else {
          Aria.download(this).load(mEntity.getId()).resume();
          mStart.setText(getContext().getString(R.string.stop));
        }
        break;

      case R.id.cancel:
        if (AppUtil.chekEntityValid(mEntity)) {
          Aria.download(this).load(mEntity.getId()).cancel();
          mStart.setText(getContext().getString(R.string.start));
        }
        break;
    }
  }

  @Download.onTaskPre public void onTaskPre(DownloadTask task) {
    mSize.setText(CommonUtil.formatFileSize(task.getFileSize()));
    setBtState(false);
  }

  @Download.onTaskStop public void onTaskStop(DownloadTask task) {
    setBtState(true);
    mSpeed.setText(task.getConvertSpeed());
  }

  @Download.onTaskCancel public void onTaskCancel(DownloadTask task) {
    setBtState(true);
    mPb.setProgress(0);
    mSpeed.setText(task.getConvertSpeed());
  }

  @Download.onTaskRunning public void onTaskRunning(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      mPb.setProgress(task.getPercent());
      mSpeed.setText(task.getConvertSpeed());
    }
  }

  @Override protected void dataCallback(int result, Object obj) {

  }

  private void setBtState(boolean startEnable) {
    mStart.setEnabled(startEnable);
    mCancel.setEnabled(!startEnable);
  }
}
