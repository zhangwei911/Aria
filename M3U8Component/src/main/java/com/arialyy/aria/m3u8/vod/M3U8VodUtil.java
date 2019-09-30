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
package com.arialyy.aria.m3u8.vod;

import android.text.TextUtils;
import com.arialyy.aria.core.common.AbsEntity;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.OnFileInfoCallback;
import com.arialyy.aria.core.listener.IEventListener;
import com.arialyy.aria.core.loader.AbsLoader;
import com.arialyy.aria.core.loader.AbsNormalLoaderUtil;
import com.arialyy.aria.core.processor.IVodTsUrlConverter;
import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
import com.arialyy.aria.exception.BaseException;
import com.arialyy.aria.exception.M3U8Exception;
import com.arialyy.aria.http.HttpTaskOption;
import com.arialyy.aria.m3u8.M3U8InfoThread;
import com.arialyy.aria.m3u8.M3U8Listener;
import com.arialyy.aria.m3u8.M3U8TaskOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * M3U8点播文件下载工具
 * 工作流程：
 * 1、创建一个和文件同父路径并且同名隐藏文件夹
 * 2、将所有m3u8的ts文件下载到该文件夹中
 * 3、完成所有分片下载后，合并ts文件
 * 4、删除该隐藏文件夹
 */
public class M3U8VodUtil extends AbsNormalLoaderUtil {

  private List<String> mUrls = new ArrayList<>();
  private M3U8TaskOption mM3U8Option;

  public M3U8VodUtil(AbsTaskWrapper wrapper, IEventListener listener) {
    super(wrapper, listener);
  }

  @Override public DTaskWrapper getTaskWrapper() {
    return (DTaskWrapper) super.getTaskWrapper();
  }

  @Override protected AbsLoader createLoader() {
    getTaskWrapper().generateM3u8Option(M3U8TaskOption.class);
    getTaskWrapper().generateTaskOption(HttpTaskOption.class);
    mM3U8Option = (M3U8TaskOption) getTaskWrapper().getM3u8Option();
    return new M3U8VodLoader((M3U8Listener) getListener(), getTaskWrapper());
  }

  @Override protected Runnable createInfoThread() {
    return new M3U8InfoThread(getTaskWrapper(), new OnFileInfoCallback() {
      @Override public void onComplete(String key, CompleteInfo info) {
        IVodTsUrlConverter converter = mM3U8Option.getVodUrlConverter();
        if (converter != null) {
          if (TextUtils.isEmpty(mM3U8Option.getBandWidthUrl())) {
            mUrls.addAll(
                converter.convert(getTaskWrapper().getEntity().getUrl(), (List<String>) info.obj));
          } else {
            mUrls.addAll(
                converter.convert(mM3U8Option.getBandWidthUrl(), (List<String>) info.obj));
          }
        } else {
          mUrls.addAll((Collection<? extends String>) info.obj);
        }
        if (mUrls.isEmpty()) {
          fail(new M3U8Exception(TAG, "获取地址失败"), false);
          return;
        } else if (!mUrls.get(0).startsWith("http")) {
          fail(new M3U8Exception(TAG, "地址错误，请使用IM3U8UrlExtInfHandler处理你的url信息"), false);
          return;
        }
        mM3U8Option.setUrls(mUrls);
        if (isStop()) {
          getListener().onStop(getTaskWrapper().getEntity().getCurrentProgress());
        } else if (isCancel()) {
          getListener().onCancel();
        } else {
          getLoader().start();
        }
      }

      @Override public void onFail(AbsEntity entity, BaseException e, boolean needRetry) {
        fail(e, needRetry);
      }
    });
  }
}
