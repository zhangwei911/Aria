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
package com.arialyy.aria.core.download.m3u8;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * m3u8加密密钥信息
 */
public class M3U8KeyInfo implements Parcelable {

  /**
   * 加密key保存地址
   */
  public String keyPath;

  /**
   * 加密key的下载地址
   */
  public String keyUrl;

  /**
   * 加密算法
   */
  public String method;

  /**
   * key的iv值
   */
  public String iv;

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.keyPath);
    dest.writeString(this.keyUrl);
    dest.writeString(this.method);
    dest.writeString(this.iv);
  }

  public M3U8KeyInfo() {
  }

  protected M3U8KeyInfo(Parcel in) {
    this.keyPath = in.readString();
    this.keyUrl = in.readString();
    this.method = in.readString();
    this.iv = in.readString();
  }

  public static final Parcelable.Creator<M3U8KeyInfo> CREATOR =
      new Parcelable.Creator<M3U8KeyInfo>() {
        @Override public M3U8KeyInfo createFromParcel(Parcel source) {
          return new M3U8KeyInfo(source);
        }

        @Override public M3U8KeyInfo[] newArray(int size) {
          return new M3U8KeyInfo[size];
        }
      };
}
