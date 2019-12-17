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
package com.arialyy.aria.orm;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.net.URLEncoder;

/**
 * Created by laoyuyu on 2018/3/22.
 */
abstract class AbsDelegate {
  static final String TAG = "AbsDelegate";



  /**
   * 检查list参数是否合法，list只能是{@code List<String>}
   *
   * @return {@code true} 合法
   */
  boolean checkList(Field list) {
    Class t = CommonUtil.getListParamType(list);
    if (t != null && t == String.class) {
      return true;
    } else {
      ALog.d(TAG, "map参数错误，支持List<String>的参数字段");
      return false;
    }
  }

  /**
   * 检查map参数是否合法，map只能是{@code Map<String, String>}
   *
   * @return {@code true} 合法
   */
  boolean checkMap(Field map) {
    Class[] ts = CommonUtil.getMapParamType(map);
    if (ts != null
        && ts[0] != null
        && ts[1] != null
        && ts[0] == String.class
        && ts[1] == String.class) {
      return true;
    } else {
      ALog.d(TAG, "map参数错误，支持Map<String,String>的参数字段");
      return false;
    }
  }

  void closeCursor(Cursor cursor) {
    synchronized (AbsDelegate.class) {
      if (cursor != null && !cursor.isClosed()) {
        try {
          cursor.close();
        } catch (android.database.SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * 检查数据库是否关闭，已经关闭的话，打开数据库
   *
   * @return 返回数据库
   */
  SQLiteDatabase checkDb(SQLiteDatabase db) {
    if (db == null || !db.isOpen()) {
      db = SqlHelper.getInstance().getDb();
    }
    return db;
  }
}
