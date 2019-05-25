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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import com.arialyy.annotations.Upload;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.common.ftp.FtpInterceptHandler;
import com.arialyy.aria.core.common.ftp.IFtpUploadInterceptor;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.UploadTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.FileUtil;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.BuildConfig;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.common.ModifyUrlDialog;
import com.arialyy.simple.databinding.ActivityFtpUploadBinding;
import java.io.File;
import java.util.List;

/**
 * Created by lyy on 2017/7/28. Ftp 文件上传demo
 */
public class FtpUploadActivity extends BaseActivity<ActivityFtpUploadBinding> {
  private final int OPEN_FILE_MANAGER_CODE = 0xB1;
  private String mFilePath = "/mnt/sdcard/AriaPrj.rar";
  private String mUrl = "ftp://9.9.9.205:2121/aa/你好";

  @Override protected void init(Bundle savedInstanceState) {
    setTile("D_FTP 文件上传");
    super.init(savedInstanceState);
    Aria.upload(this).register();
    UploadEntity entity = Aria.upload(this).getUploadEntity(mFilePath);
    if (entity != null) {
      getBinding().setFileSize(CommonUtil.formatFileSize(entity.getFileSize()));
      getBinding().setProgress(entity.isComplete() ? 100
          : (int) (entity.getCurrentProgress() * 100 / entity.getFileSize()));
    }
    getBinding().setUrl(mUrl);
    getBinding().setFilePath(mFilePath);
    getBinding().setViewModel(this);
  }

  @Override protected int setLayoutId() {
    return R.layout.activity_ftp_upload;
  }

  public void chooseUrl() {
    ModifyUrlDialog dialog =
        new ModifyUrlDialog(this, getString(R.string.modify_url_dialog_title), mUrl);
    dialog.show(getSupportFragmentManager(), "ModifyUrlDialog");
  }

  public void chooseFilePath() {

    File parentFile = new File(mFilePath);
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    Uri uri;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      uri = FileProvider.getUriForFile(this,
          BuildConfig.APPLICATION_ID + ".provider",
          parentFile.getParentFile());
    } else {
      uri = Uri.fromFile(parentFile.getParentFile());
    }

    intent.setDataAndType(uri, "*/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivityForResult(intent, OPEN_FILE_MANAGER_CODE);
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        if (Aria.upload(this).load(mFilePath).isRunning()) {
          Aria.upload(this).loadFtp(mFilePath).stop();
          getBinding().setStateStr(getString(R.string.resume));
        } else {
          getBinding().setStateStr(getString(R.string.stop));
          Aria.upload(this).loadFtp(mFilePath).setUploadUrl(mUrl).setUploadInterceptor(
              new IFtpUploadInterceptor() {

                @Override
                public FtpInterceptHandler onIntercept(UploadEntity entity, List<String> fileList) {
                  FtpInterceptHandler.Builder builder = new FtpInterceptHandler.Builder();
                  //builder.coverServerFile();
                  builder.resetFileName("test.zip");
                  return builder.build();
                }
              })
              //.login("lao", "123456")
              .login("N0rI", "0qcK")
              .start();
        }
        break;
      case R.id.cancel:
        Aria.upload(this).loadFtp(mFilePath).cancel();
        break;
    }
  }

  @Upload.onWait void onWait(UploadTask task) {
    Log.d(TAG, task.getTaskName() + "_wait");
  }

  @Upload.onPre public void onPre(UploadTask task) {
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Upload.onTaskStart public void taskStart(UploadTask task) {
    Log.d(TAG, "开始上传，md5：" + FileUtil.getFileMD5(new File(task.getEntity().getFilePath())));
  }

  @Upload.onTaskResume public void taskResume(UploadTask task) {
    Log.d(TAG, "恢复上传");
  }

  @Upload.onTaskStop public void taskStop(UploadTask task) {
    getBinding().setSpeed("");
    Log.d(TAG, "停止上传");
  }

  @Upload.onTaskCancel public void taskCancel(UploadTask task) {
    getBinding().setSpeed("");
    getBinding().setFileSize("");
    getBinding().setProgress(0);
    Log.d(TAG, "删除任务");
  }

  @Upload.onTaskFail public void taskFail(UploadTask task) {
    Log.d(TAG, "上传失败");
  }

  @Upload.onTaskRunning public void taskRunning(UploadTask task) {
    Log.d(TAG, "PP = " + task.getPercent());
    getBinding().setProgress(task.getPercent());
    getBinding().setSpeed(task.getConvertSpeed());
  }

  @Upload.onTaskComplete public void taskComplete(UploadTask task) {
    getBinding().setProgress(100);
    getBinding().setSpeed("");
    T.showShort(this, "文件：" + task.getEntity().getFileName() + "，上传完成");
  }

  @Override protected void dataCallback(int result, Object data) {
    super.dataCallback(result, data);
    if (result == ModifyUrlDialog.MODIFY_URL_DIALOG_RESULT) {
      mUrl = String.valueOf(data);
      getBinding().setUrl(mUrl);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == OPEN_FILE_MANAGER_CODE && resultCode == RESULT_OK) {
      Uri uri = data.getData();
      //Toast.makeText(this, "文件路径：" + uri.getPath(), Toast.LENGTH_SHORT).show();
    }
  }
}
