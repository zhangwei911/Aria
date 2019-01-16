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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.util.ALog;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lyy on 2015/11/2.
 * sql帮助类
 */
final class SqlHelper extends SQLiteOpenHelper {
  private static final String TAG = "SqlHelper";
  static volatile SqlHelper INSTANCE = null;

  private DelegateCommon mDelegate;

  static SqlHelper init(Context context) {
    if (INSTANCE == null) {
      synchronized (SqlHelper.class) {
        DelegateCommon delegate = DelegateManager.getInstance().getDelegate(DelegateCommon.class);
        INSTANCE = new SqlHelper(context.getApplicationContext(), delegate);
        SQLiteDatabase db = INSTANCE.getWritableDatabase();
        db = delegate.checkDb(db);
        // SQLite在3.6.19版本中开始支持外键约束，
        // 而在Android中 2.1以前的版本使用的SQLite版本是3.5.9， 在2.2版本中使用的是3.6.22.
        // 但是为了兼容以前的程序，默认并没有启用该功能，如果要启用该功能
        // 需要使用如下语句：
        db.execSQL("PRAGMA foreign_keys=ON;");
        Set<String> tables = DBConfig.mapping.keySet();
        for (String tableName : tables) {
          Class clazz = null;
          clazz = DBConfig.mapping.get(tableName);

          if (!delegate.tableExists(db, clazz)) {
            delegate.createTable(db, clazz);
          }
        }
      }
    }
    return INSTANCE;
  }

  private SqlHelper(Context context, DelegateCommon delegate) {
    super(DBConfig.SAVE_IN_SDCARD ? new DatabaseContext(context) : context, DBConfig.DB_NAME, null,
        DBConfig.VERSION);
    mDelegate = delegate;
  }

  @Override public void onCreate(SQLiteDatabase db) {

  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < newVersion) {
      if (oldVersion < 31) {
        handle314AriaUpdate(db);
      } else {
        handleDbUpdate(db);
      }
    }
  }

  @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    //super.onDowngrade(db, oldVersion, newVersion);
    if (oldVersion > newVersion) {
      handleDbUpdate(db);
    }
  }

  /**
   * 处理数据库升级
   */
  private void handleDbUpdate(SQLiteDatabase db) {
    if (db == null) {
      ALog.e("SqlHelper", "db 为 null");
      return;
    } else if (!db.isOpen()) {
      ALog.e("SqlHelper", "db已关闭");
      return;
    }
    Set<String> tables = DBConfig.mapping.keySet();
    for (String tableName : tables) {
      Class clazz = DBConfig.mapping.get(tableName);
      if (mDelegate.tableExists(db, clazz)) {
        String countColumnSql = "SELECT rowid FROM " + tableName;
        Cursor cursor = db.rawQuery(countColumnSql, null);
        int dbColumnNum = cursor.getColumnCount();
        List<Field> fields = SqlUtil.getAllNotIgnoreField(clazz);
        int newEntityColumnNum = (fields == null || fields.isEmpty()) ? 0 : fields.size();
        if (dbColumnNum != newEntityColumnNum) {
          db = mDelegate.checkDb(db);
          //备份数据
          List<DbEntity> list =
              DelegateManager.getInstance().getDelegate(DelegateFind.class).findAllData(db, clazz);
          //修改表名为中介表名
          String alertSql = "ALTER TABLE " + tableName + " RENAME TO " + tableName + "_temp";
          //db.beginTransaction();
          db.execSQL(alertSql);

          //创建一个原本的表
          mDelegate.createTable(db, clazz);
          //传入原来的数据
          if (list != null && list.size() > 0) {
            DelegateUpdate update = DelegateManager.getInstance().getDelegate(DelegateUpdate.class);
            for (DbEntity entity : list) {
              update.insertData(db, entity);
            }
          }
          //删除中介表
          mDelegate.dropTable(db, tableName + "_temp");
        }
      }
    }
    //db.setTransactionSuccessful();
    //db.endTransaction();
    mDelegate.close(db);
  }

  /**
   * 处理3.4版本之前数据库迁移，主要是修改子表外键字段对应的值
   */
  private void handle314AriaUpdate(SQLiteDatabase db) {
    Set<String> tables = DBConfig.mapping.keySet();
    Map<String, List<DbEntity>> map = new HashMap<>();
    for (String tableName : tables) {
      Class clazz = DBConfig.mapping.get(tableName);

      String pColumn = SqlUtil.getPrimaryName(clazz);
      if (!TextUtils.isEmpty(pColumn)) {
        //删除所有主键为null的数据
        String nullSql =
            "DELETE FROM " + tableName + " WHERE " + pColumn + " = '' OR " + pColumn + " IS NULL";
        ALog.d(TAG, nullSql);
        db.execSQL(nullSql);

        //删除所有主键重复的数据
        String repeatSql = "DELETE FROM "
            + tableName
            + " WHERE "
            + pColumn
            + " in (SELECT "
            + pColumn
            + " FROM "
            + tableName
            + " GROUP BY " + pColumn + " having  count(" + pColumn + ") > 1)";

        ALog.d(TAG, repeatSql);
        db.execSQL(repeatSql);
      }

      //备份数据
      List<DbEntity> list =
          DelegateManager.getInstance().getDelegate(DelegateFind.class).findAllData(db, clazz);

      map.put(tableName, list);

      //修改表名为中介表名
      String alertSql = "ALTER TABLE " + tableName + " RENAME TO " + tableName + "_temp";
      db.execSQL(alertSql);

      //创建一个原本的表
      mDelegate.createTable(db, clazz);
      //插入数据，3.6版本后移除任务实体的保存
      //if (list != null && list.size() > 0) {
      //  DelegateUpdate update = DelegateManager.getInstance().getDelegate(DelegateUpdate.class);
      //  try {
      //    for (DbEntity entity : list) {
      //      if (entity instanceof DTaskWrapper && ((DTaskWrapper) entity).isGroupTask()) {
      //        if (TextUtils.isEmpty(((DTaskWrapper) entity).getKey())) {
      //          ALog.w(TAG, "DownloadTaskEntity的key为空，将忽略该条数据");
      //          continue;
      //        }
      //        if (TextUtils.isEmpty(((DTaskWrapper) entity).getUrl())) {
      //          List<DbEntity> temp = map.get("DownloadEntity");
      //          boolean isRefresh = false;
      //          for (DbEntity dbEntity : temp) {
      //            if (((DownloadEntity) dbEntity).getDownloadPath()
      //                .equals(((DTaskWrapper) entity).getKey())) {
      //              ((DTaskWrapper) entity).setUrl(((DownloadEntity) dbEntity).getUrl());
      //              isRefresh = true;
      //              break;
      //            }
      //          }
      //          if (isRefresh) {
      //            update.insertData(db, entity);
      //          }
      //        }
      //      } else {
      //        update.insertData(db, entity);
      //      }
      //    }
      //  } catch (Exception e) {
      //    ALog.e(TAG, ALog.getExceptionString(e));
      //  }
      //}

      //删除中介表
      mDelegate.dropTable(db, tableName + "_temp");

      mDelegate.close(db);
    }

    map.clear();
  }
}