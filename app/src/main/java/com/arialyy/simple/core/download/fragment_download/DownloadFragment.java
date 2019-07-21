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

package com.arialyy.simple.core.download.fragment_download;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.core.AbsFragment;
import com.arialyy.simple.R;
import com.arialyy.simple.databinding.FragmentDownloadBinding;
import com.arialyy.simple.util.AppUtil;

/**
 * Created by lyy on 2017/1/4.
 */
public class DownloadFragment extends AbsFragment<FragmentDownloadBinding>
    implements View.OnClickListener {
  Button mStart;
  Button mCancel;
  private DownloadEntity mEntity;

  private static final String DOWNLOAD_URL =
      "https://res5.d.cn/2137e42d610b3488d9420c6421529386eee5bdbfd9be1fafe0a05d6dabaec8c156ddbd00581055bbaeac03904fb63310e80010680235d16bd4c040b50096a0c20dd1c4b0854529a1.apk";

  @Override protected void init(Bundle savedInstanceState) {
    mStart = mRootView.findViewById(R.id.start);
    mCancel = mRootView.findViewById(R.id.cancel);
    mStart.setOnClickListener(this);
    mCancel.setOnClickListener(this);

    mEntity = Aria.download(this).getFirstDownloadEntity(DOWNLOAD_URL);
    if (mEntity != null) {
      getBinding().setFileSize(CommonUtil.formatFileSize(mEntity.getFileSize()));
      int state = mEntity.getState();
      getBinding().setProgress(mEntity.getPercent());
      if (mEntity.getState() == IEntity.STATE_RUNNING) {
        getBinding().setStateStr(getString(R.string.stop));
      } else {
        getBinding().setStateStr(getString(R.string.resume));
      }
    } else {
      getBinding().setStateStr(getString(R.string.start));
    }
    Aria.download(this).register();
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (!AppUtil.chekEntityValid(mEntity)) {
          Aria.download(this)
              .load(DOWNLOAD_URL)
              .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/王者军团.apk")
              .start();
          getBinding().setStateStr(getString(R.string.stop));
          break;
        }
        if (Aria.download(this).load(mEntity.getId()).isRunning()) {
          Aria.download(this).load(mEntity.getId()).stop();
          getBinding().setStateStr(getString(R.string.resume));
        } else {
          Aria.download(this).load(mEntity.getId()).resume();
          getBinding().setStateStr(getString(R.string.stop));
        }
        break;

      case R.id.cancel:
        if (AppUtil.chekEntityValid(mEntity)) {
          Aria.download(this).load(mEntity.getId()).cancel();
        }
        break;
    }
  }

  @Download.onTaskPre public void onTaskPre(DownloadTask task) {
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Download.onTaskStop public void onTaskStop(DownloadTask task) {
    getBinding().setSpeed("");
    getBinding().setStateStr(getString(R.string.resume));
  }

  @Download.onTaskCancel public void onTaskCancel(DownloadTask task) {
    getBinding().setProgress(0);
    getBinding().setSpeed("");
    getBinding().setStateStr(getString(R.string.cancel));
  }

  @Download.onTaskRunning public void onTaskRunning(DownloadTask task) {
    long len = task.getFileSize();
    if (len == 0) {
      getBinding().setProgress(0);
    } else {
      getBinding().setProgress(task.getPercent());
    }
    getBinding().setSpeed(task.getConvertSpeed());
  }

  @Override protected void onDelayLoad() {

  }

  @Override protected int setLayoutId() {
    return R.layout.fragment_download;
  }

  @Override protected void dataCallback(int result, Object obj) {

  }
}
