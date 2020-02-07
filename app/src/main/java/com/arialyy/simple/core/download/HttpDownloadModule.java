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
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.frame.base.BaseViewModule;
import com.arialyy.simple.util.AppUtil;

import java.io.File;

public class HttpDownloadModule extends BaseViewModule {
    private final String HTTP_URL_KEY = "HTTP_URL_KEY";
    private final String HTTP_PATH_KEY = "HTTP_PATH_KEY";

    private final String defUrl =
            "http://hzdown.muzhiwan.com/2017/05/08/nl.noio.kingdom_59104935e56f0.apk";
    //"https://ss1.baidu.com/-4o3dSag_xI4khGko9WTAnF6hhy/image/h%3D300/sign=a9e671b9a551f3dedcb2bf64a4eff0ec/4610b912c8fcc3cef70d70409845d688d53f20f7.jpg";
    //"http://9.9.9.205:5000/download/Cyberduck-6.9.4.30164.zip";
    //"http://202.98.201.103:7000/vrs/TPK/ZTC440402001Z.tpk";
    private final String defFilePath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + "/update.zip";

    private MutableLiveData<DownloadEntity> liveData = new MutableLiveData<>();
    private DownloadEntity singDownloadInfo;

    /**
     * 单任务下载的信息
     */
    LiveData<DownloadEntity> getHttpDownloadInfo(Context context) {
        //String url = AppUtil.getConfigValue(context, HTTP_URL_KEY, defUrl);
        //String url =
        //    "http://sdkdown.muzhiwan.com/openfile/2019/05/21/com.netease.tom.mzw_5ce3ef8754d05.apk";
        //String url = "http://image.totwoo.com/totwoo-TOTWOO-v3.5.6.apk";
        //String url = "http://fdfs.speedata.cn:9989/group1/M00/00/05/rBGFrl3fdAKAVJwfMtSa9R18wLU139.zip";
        //String url = "http://9.9.9.28:8088/files/update.zip";
        //String url = "https://ss1.baidu.com/-4o3dSag_xI4khGko9WTAnF6hhy/image/h%3D300/sign=a9e671b9a551f3dedcb2bf64a4eff0ec/4610b912c8fcc3cef70d70409845d688d53f20f7.jpg";
        //String url = "https://imtt.dd.qq.com/16891/apk/70BFFDB05AB8686F2A4CF3E07588A377.apk?fsname=com.tencent.tmgp.speedmobile_1.16.0.33877_1160033877.apk&csr=1bbd";
        //String url = "https://ss1.baidu.com/-4o3dSag_xI4khGko9WTAnF6hhy/image/h%3D300/sign=a9e671b9a551f3dedcb2bf64a4eff0ec/4610b912c8fcc3cef70d70409845d688d53f20f7.jpg";
        //String filePath = AppUtil.getConfigValue(context, HTTP_PATH_KEY, defFilePath);
        String url = "https://y.qq.com/download/import/QQMusic-import-1.2.1.zip";
        String filePath = "/mnt/sdcard/update.zip";
        //String url = "https://dhfspace.360drm.com/1_12809_1543904946_VID_20180808_212829.vep?e=1578554567&token=gUBmfZgZS5wy4wdQIDZG8UVxlNCyVSjvksIb13K5:WYSZRgmLbH1_9hjgqOAGmqR27JM=";
        //String filePath = "/mnt/sdcard/sssss.zip";

        singDownloadInfo = Aria.download(context).getFirstDownloadEntity(url);
        if (singDownloadInfo == null) {
            singDownloadInfo = new DownloadEntity();
            singDownloadInfo.setUrl(url);
            File file = new File(defFilePath);
            singDownloadInfo.setFilePath(filePath);
            singDownloadInfo.setFileName(file.getName());
        } else {
            AppUtil.setConfigValue(context, HTTP_PATH_KEY, singDownloadInfo.getFilePath());
            AppUtil.setConfigValue(context, HTTP_URL_KEY, singDownloadInfo.getUrl());
        }
        liveData.postValue(singDownloadInfo);

        return liveData;
    }

    /**
     * 更新文件保存路径
     *
     * @param filePath 文件保存路径
     */
    void updateFilePath(Context context, String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            ALog.e(TAG, "文件保存路径为空");
            return;
        }
        File temp = new File(filePath);
        AppUtil.setConfigValue(context, HTTP_PATH_KEY, filePath);
        singDownloadInfo.setFileName(temp.getName());
        singDownloadInfo.setFilePath(filePath);
        liveData.postValue(singDownloadInfo);
    }

    /**
     * 更新url
     */
    void uploadUrl(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            ALog.e(TAG, "下载地址为空");
            return;
        }
        AppUtil.setConfigValue(context, HTTP_URL_KEY, url);
        singDownloadInfo.setUrl(url);
        liveData.postValue(singDownloadInfo);
    }
}
