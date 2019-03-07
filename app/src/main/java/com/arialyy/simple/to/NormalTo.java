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
package com.arialyy.simple.to;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 简单列表对象
 */
public class NormalTo implements Parcelable {
  public int icon;
  public String title;

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.icon);
    dest.writeString(this.title);
  }

  public NormalTo() {
  }

  protected NormalTo(Parcel in) {
    this.icon = in.readInt();
    this.title = in.readString();
  }

  public static final Parcelable.Creator<NormalTo> CREATOR = new Parcelable.Creator<NormalTo>() {
    @Override public NormalTo createFromParcel(Parcel source) {
      return new NormalTo(source);
    }

    @Override public NormalTo[] newArray(int size) {
      return new NormalTo[size];
    }
  };
}
