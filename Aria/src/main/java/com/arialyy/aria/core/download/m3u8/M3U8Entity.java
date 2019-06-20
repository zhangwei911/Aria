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
import com.arialyy.aria.orm.DbEntity;

/**
 * M3U8实体信息
 */
public class M3U8Entity extends DbEntity implements Parcelable {

  /**
   * 文件保存路径
   */
  private String filePath;

  /**
   * 当前peer的位置
   */
  private int peerIndex;

  /**
   * peer总数
   */
  private int peerNum;

  /**
   * 是否是直播，true 直播
   */
  private boolean isLive;

  public boolean isLive() {
    return isLive;
  }

  public void setLive(boolean live) {
    isLive = live;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public int getPeerIndex() {
    return peerIndex;
  }

  public void setPeerIndex(int peerIndex) {
    this.peerIndex = peerIndex;
  }

  public int getPeerNum() {
    return peerNum;
  }

  public void setPeerNum(int peerNum) {
    this.peerNum = peerNum;
  }

  public M3U8Entity() {
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.filePath);
    dest.writeInt(this.peerIndex);
    dest.writeInt(this.peerNum);
    dest.writeByte(this.isLive ? (byte) 1 : (byte) 0);
  }

  protected M3U8Entity(Parcel in) {
    this.filePath = in.readString();
    this.peerIndex = in.readInt();
    this.peerNum = in.readInt();
    this.isLive = in.readByte() != 0;
  }

  public static final Creator<M3U8Entity> CREATOR = new Creator<M3U8Entity>() {
    @Override public M3U8Entity createFromParcel(Parcel source) {
      return new M3U8Entity(source);
    }

    @Override public M3U8Entity[] newArray(int size) {
      return new M3U8Entity[size];
    }
  };
}
