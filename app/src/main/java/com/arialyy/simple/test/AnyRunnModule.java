package com.arialyy.simple.test;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.show.L;
import java.io.File;

/**
 * Created by laoyuyu on 2018/4/13.
 */

public class AnyRunnModule {
  String TAG = "AnyRunnModule";
  private Context mContext;
  private String mUrl;

  public AnyRunnModule(Context context) {
    Aria.download(this).register();
    mContext = context;
  }

  @Download.onWait void onWait(DownloadTask task) {
    Log.d(TAG, "wait ==> " + task.getDownloadEntity().getFileName());
  }

  @Download.onPre protected void onPre(DownloadTask task) {
    Log.d(TAG, "onPre");
  }

  @Download.onTaskStart void taskStart(DownloadTask task) {
    Log.d(TAG, "onStart");
  }

  @Download.onTaskRunning protected void running(DownloadTask task) {
    Log.d(TAG, "running；Percent = " + task.getPercent());
  }

  @Download.onTaskResume void taskResume(DownloadTask task) {
    Log.d(TAG, "resume");
  }

  @Download.onTaskStop void taskStop(DownloadTask task) {
    Log.d(TAG, "stop");
  }

  @Download.onTaskCancel void taskCancel(DownloadTask task) {
    Log.d(TAG, "cancel");
  }

  @Download.onTaskFail void taskFail(DownloadTask task) {
    Log.d(TAG, "fail");
  }

  @Download.onTaskComplete void taskComplete(DownloadTask task) {
    L.d(TAG, "path ==> " + task.getDownloadEntity().getDownloadPath());
    L.d(TAG, "md5Code ==> " + CommonUtil.getFileMD5(new File(task.getDownloadPath())));
  }

  void start(String url) {
    mUrl = url;
    String path = Environment.getExternalStorageDirectory().getPath() + "/mmm2.mp4";
    Aria.download(this)
        .load(url)
        .setFilePath(path)
        .resetState()
        .start();
  }

  void startFtp(String url) {
    mUrl = url;
    Aria.download(this)
        .loadFtp(url)
        .login("lao", "123456")
        .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/Download/")
        .asFtps()
        .setStorePath("/mnt/sdcard/Download/server.crt")
        .setAlias("www.laoyuyu.me")
        .setStorePass("123456")
        .start();
  }

  void stop(String url) {
    Aria.download(this).load(url).stop();
  }

  void cancel(String url) {
    Aria.download(this).load(url).cancel();
  }

  void unRegister() {
    Aria.download(this).unRegister();
  }
}
