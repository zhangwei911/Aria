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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.core.AbsPopupWindow;
import com.arialyy.simple.R;
import com.arialyy.simple.util.AppUtil;
import com.arialyy.simple.widget.HorizontalProgressBarWithNumber;

/**
 * Created by AriaL on 2017/1/2.
 */
public class DownloadPopupWindow extends AbsPopupWindow implements View.OnClickListener {
  private HorizontalProgressBarWithNumber mPb;
  private Button mStart;
  private Button mCancel;
  private TextView mSize;
  private TextView mSpeed;
  private DownloadEntity mEntity;

  private static final String DOWNLOAD_URL =
      "http://static.gaoshouyou.com/d/25/57/2e25bd9d4557ba31e9beebacfaf9e804.apk";

  public DownloadPopupWindow(Context context) {
    super(context, new ColorDrawable(Color.WHITE));
    initWidget();
  }

  @Override protected int setLayoutId() {
    return R.layout.dialog_download;
  }

  private void initWidget() {
    mPb = mView.findViewById(R.id.progressBar);
    mStart = mView.findViewById(R.id.start);
    mCancel = mView.findViewById(R.id.cancel);
    mSize = mView.findViewById(R.id.size);
    mSpeed = mView.findViewById(R.id.speed);

    mStart.setOnClickListener(this);
    mCancel.setOnClickListener(this);

    mEntity = Aria.download(this).getFirstDownloadEntity(DOWNLOAD_URL);
    if (mEntity != null) {
      mPb.setProgress(mEntity.getPercent());
      mSize.setText(CommonUtil.formatFileSize(mEntity.getFileSize()));
      if (mEntity.getState() == IEntity.STATE_RUNNING) {
        mStart.setText(getContext().getString(R.string.stop));
      } else {
        mStart.setText(getContext().getString(R.string.resume));
      }
    } else {
      mStart.setText(getContext().getString(R.string.start));
    }
    Aria.download(this).register();
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (!AppUtil.chekEntityValid(mEntity)) {
          Aria.download(this)
              .load(DOWNLOAD_URL)
              .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/消消乐.apk")
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
        }
        break;
    }
  }

  @Override protected void dataCallback(int result, Object obj) {

  }

  @Download.onTaskPre public void onTaskPre(DownloadTask task) {
    mSize.setText(CommonUtil.formatFileSize(task.getFileSize()));
  }

  @Download.onTaskStop public void onTaskStop(DownloadTask task) {
    mSpeed.setText("0.0kb/s");
    mStart.setText(getContext().getString(R.string.resume));
  }

  @Download.onTaskCancel public void onTaskCancel(DownloadTask task) {
    mPb.setProgress(0);
    mSpeed.setText("0.0kb/s");
  }

  @Download.onTaskRunning public void onTaskRunning(DownloadTask task) {
    long current = task.getCurrentProgress();
    long len = task.getFileSize();
    if (len == 0) {
      mPb.setProgress(0);
    } else {
      mPb.setProgress((int) ((current * 100) / len));
    }
    mSpeed.setText(task.getConvertSpeed());
  }
}
