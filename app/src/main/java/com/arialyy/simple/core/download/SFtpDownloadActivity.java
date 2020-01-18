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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.common.SFtpOption;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.FileUtil;
import com.arialyy.frame.util.show.L;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.common.DirChooseDialog;
import com.arialyy.simple.common.ModifyUrlDialog;
import com.arialyy.simple.databinding.ActivitySftpDownloadBinding;
import java.io.File;
import java.io.IOException;

/**
 * Created by lyy on 2017/7/25.
 * Ftp下载
 */
public class SFtpDownloadActivity extends BaseActivity<ActivitySftpDownloadBinding> {
  private String mUrl, mFilePath;
  private FtpDownloadModule mModule;
  private long mTaskId;
  private String user = "tester", passw = "password";
  private String prvKeyPath;
  private String pubKeyPath;

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    setTitle("FTP文件下载");
    Aria.download(this).register();
    prvKeyPath = getFilesDir().getPath() + "/id_rsa";
    pubKeyPath = getFilesDir().getPath() + "/id_rsa.pub";
    copyKey();
    mModule = ViewModelProviders.of(this).get(FtpDownloadModule.class);
    mModule.getSftpDownloadInfo(this).observe(this, new Observer<DownloadEntity>() {

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
    //try {
    //  getBinding().codeView.setSource(AppUtil.getHelpCode(this, "FtpDownload.java"));
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}
  }

  private void copyKey() {
    try {
      // 为了测试方便，每次重新覆盖证书文件
      File prvKey = new File(prvKeyPath);
      //FileUtil.deleteFile(prvKey);
      if (!prvKey.exists()) {
        FileUtil.createFileFormInputStream(getAssets().open("id_rsa"), prvKeyPath);
      }
      File pubKey = new File(pubKeyPath);
      //FileUtil.deleteFile(pubKey);
      if (!pubKey.exists()) {
        FileUtil.createFileFormInputStream(getAssets().open("id_rsa.pub"), pubKeyPath);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void onClick(View view) {

    switch (view.getId()) {
      case R.id.start:

        if (mTaskId == -1) {
          mTaskId = Aria.download(this).loadFtp(mUrl)
              .setFilePath(mFilePath, true)
              .sftpOption(getFtpOption())
              .create();
          getBinding().setStateStr(getString(R.string.stop));
          break;
        }
        if (Aria.download(this).load(mTaskId).isRunning()) {
          getBinding().setStateStr(getString(R.string.resume));
          Aria.download(this).loadFtp(mTaskId).stop();
        } else {
          Aria.download(this)
              .loadFtp(mTaskId)
              .sftpOption(getFtpOption())
              .resume();
          getBinding().setStateStr(getString(R.string.stop));
        }
        break;

      case R.id.cancel:
        Aria.download(this).loadFtp(mTaskId).cancel(true);
        getBinding().setStateStr(getString(R.string.start));
        mTaskId = -1;
        break;
    }
  }

  private SFtpOption getFtpOption() {
    SFtpOption option = new SFtpOption();
    option.login(user, passw); // 账号密码登录
    // 证书登录
    option.setPrvKey(prvKeyPath); // 设置私钥
    option.setPrvKeyPass("123456"); // 设置私钥密码（如果没有密码，可以不设置）
    option.setPubKey(pubKeyPath); // 设置公钥
    option.setKnowHostPath(getFilesDir().getPath() + "/know_hosts");

    //option.setServerIdentifier(FtpOption.FTPServerIdentifier.SYST_NT);
    //option.setConnectionMode(FtpConnectionMode.DATA_CONNECTION_MODE_ACTIVITY);
    return option;
  }

  public void chooseUrl() {
    ModifyUrlDialog dialog =
        new ModifyUrlDialog(this, getString(R.string.modify_url_dialog_title), mUrl);
    dialog.show(getSupportFragmentManager(), "ModifyUrlDialog");
  }

  public void chooseFilePath() {
    DirChooseDialog dirChooseDialog = new DirChooseDialog(this);
    dirChooseDialog.show(getSupportFragmentManager(), "DirChooseDialog");
  }

  @Download.onPre() protected void onPre(DownloadTask task) {
    L.d(TAG, "ftp pre");
  }

  @Download.onTaskPre() protected void onTaskPre(DownloadTask task) {
    L.d(TAG, "ftp task pre, fileSize = " + task.getConvertFileSize());
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Download.onTaskStart() void taskStart(DownloadTask task) {
    L.d(TAG, "ftp task create");
  }

  @Download.onTaskRunning() protected void running(DownloadTask task) {
    ALog.d(TAG, "running, p = " + task.getPercent() + ", speed = " + task.getConvertSpeed());
    getBinding().setProgress(task.getPercent());
    getBinding().setSpeed(task.getConvertSpeed());
  }

  @Download.onTaskResume() void taskResume(DownloadTask task) {
    L.d(TAG, "ftp task resume");
  }

  @Download.onTaskStop() void taskStop(DownloadTask task) {
    L.d(TAG, "ftp task stop");
    getBinding().setSpeed("");
    getBinding().setStateStr(getString(R.string.resume));
  }

  @Download.onTaskCancel() void taskCancel(DownloadTask task) {
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Download.onTaskFail() void taskFail(DownloadTask task) {
    L.d(TAG, "ftp task fail");
    getBinding().setSpeed("");
    getBinding().setStateStr(getString(R.string.resume));
  }

  @Download.onTaskComplete() void taskComplete(DownloadTask task) {
    getBinding().setSpeed("");
    getBinding().setProgress(100);
    getBinding().setStateStr(getString(R.string.re_start));
    Log.d(TAG, "md5 ==> " + CommonUtil.getFileMD5(new File(task.getFilePath())));
    T.showShort(this, "文件：" + task.getEntity().getFileName() + "，下载完成");
  }

  @Override protected int setLayoutId() {
    return R.layout.activity_sftp_download;
  }

  @Override protected void dataCallback(int result, Object data) {
    super.dataCallback(result, data);
    if (result == ModifyUrlDialog.MODIFY_URL_DIALOG_RESULT) {
      mModule.uploadUrl(this, String.valueOf(data));
    } else if (result == DirChooseDialog.DIR_CHOOSE_DIALOG_RESULT) {
      mModule.updateFilePath(this, String.valueOf(data));
    }
  }
}
