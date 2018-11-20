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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.arialyy.aria.orm.annotation.Primary;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Created by laoyuyu on 2018/3/22.
 * 增加数据、更新数据
 */
class DelegateUpdate extends AbsDelegate {
  private DelegateUpdate() {
  }

  ///**
  // * 添加或更新关联数据
  // */
  //void saveRelationData(SQLiteDatabase db, AbsWrapper wrapper) {
  //  Class clazz = wrapper.getClass();
  //  List<Field> fields = CommonUtil.getAllFields(clazz);
  //  DbEntity one = null;
  //  Object many = null;
  //  try {
  //    for (Field field : fields) {
  //      if (SqlUtil.isOne(field)) {
  //        one = (DbEntity) field.get(wrapper);
  //      } else if (SqlUtil.isMany(field)) {
  //        many = field.get(wrapper);
  //      }
  //    }
  //    if (one == null) {
  //      ALog.w(TAG, "保存关联数据失败，@One注解的字段为null");
  //      return;
  //    }
  //    if (many == null) {
  //      ALog.w(TAG, "保存关联数据失败，@Many注解的字段为null");
  //      return;
  //    }
  //    List<Field> oneFields = CommonUtil.getAllFields(one.getClass());
  //    one.save();
  //    if (many.getClass() == List.class) {
  //      for (DbEntity sub : (List<DbEntity>) many) {
  //        sub.getClass().getA
  //        sub.save();
  //      }
  //    } else {
  //      if (DbEntity.class.isInstance(many)) {
  //        ((DbEntity) many).save();
  //      } else {
  //        ALog.w(TAG, "保存关联数据失败，@Many注解的字段不是DbEntity子类");
  //        return;
  //      }
  //    }
  //  } catch (IllegalAccessException e) {
  //    e.printStackTrace();
  //  }
  //}

  /**
   * 删除某条数据
   */
  synchronized <T extends DbEntity> void delData(SQLiteDatabase db, Class<T> clazz, String... expression) {
    db = checkDb(db);
    CheckUtil.checkSqlExpression(expression);

    String sql = "DELETE FROM " + CommonUtil.getClassName(clazz) + " WHERE " + expression[0] + " ";
    sql = sql.replace("?", "%s");
    Object[] params = new String[expression.length - 1];
    for (int i = 0, len = params.length; i < len; i++) {
      params[i] = String.format("'%s'", encodeStr(expression[i + 1]));
    }
    sql = String.format(sql, params);
    print(DEL_DATA, sql);
    db.execSQL(sql);
    close(db);
  }

  /**
   * 修改某行数据
   */
  synchronized void modifyData(SQLiteDatabase db, DbEntity dbEntity) {
    db = checkDb(db);
    Class<?> clazz = dbEntity.getClass();
    List<Field> fields = CommonUtil.getAllFields(clazz);
    if (fields != null && fields.size() > 0) {
      ContentValues values = new ContentValues();
      try {
        for (Field field : fields) {
          field.setAccessible(true);
          if (isIgnore(dbEntity, field)) {
            continue;
          }
          String value;
          Type type = field.getType();
          if (type == Map.class && checkMap(field)) {
            value = SqlUtil.map2Str((Map<String, String>) field.get(dbEntity));
          } else if (type == List.class && checkList(field)) {
            value = SqlUtil.list2Str(dbEntity, field);
          } else {
            Object obj = field.get(dbEntity);
            value = obj == null ? "" : obj.toString();
          }
          values.put(field.getName(), encodeStr(value));
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      if (values.size() > 0) {
        db.update(CommonUtil.getClassName(dbEntity), values, "rowid=?",
            new String[] { String.valueOf(dbEntity.rowID) });
      } else {
        ALog.d(TAG, "没有数据更新");
      }
    }
    close(db);
  }

  /**
   * 插入数据
   */
  synchronized void insertData(SQLiteDatabase db, DbEntity dbEntity) {
    db = checkDb(db);
    Class<?> clazz = dbEntity.getClass();
    List<Field> fields = CommonUtil.getAllFields(clazz);
    if (fields != null && fields.size() > 0) {
      ContentValues values = new ContentValues();
      try {
        for (Field field : fields) {
          field.setAccessible(true);
          if (isIgnore(dbEntity, field)) {
            continue;
          }
          String value = null;
          Type type = field.getType();
          if (type == Map.class && checkMap(field)) {
            value = SqlUtil.map2Str((Map<String, String>) field.get(dbEntity));
          } else if (type == List.class && checkList(field)) {
            value = SqlUtil.list2Str(dbEntity, field);
          } else {
            Object obj = field.get(dbEntity);
            if (obj != null) {
              value = field.get(dbEntity).toString();
            }
          }
          values.put(field.getName(), encodeStr(value));
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      dbEntity.rowID = db.insert(CommonUtil.getClassName(dbEntity), null, values);
    }
    close(db);
  }

  /**
   * {@code true}自动增长的主键和需要忽略的字段
   */
  private boolean isIgnore(Object obj, Field field) throws IllegalAccessException {
    if (SqlUtil.isIgnore(field)) {
      return true;
    }
    Object value = field.get(obj);
    if (value == null) {  // 忽略为空的字段
      return true;
    }
    if (value instanceof String) {
      if (TextUtils.isEmpty(String.valueOf(value))) {
        return true;
      }
    }
    if (value instanceof List) {
      if (((List) value).size() == 0) {
        return true;
      }
    }
    if (value instanceof Map) {
      if (((Map) value).size() == 0) {
        return true;
      }
    }

    if (SqlUtil.isPrimary(field)) {   //忽略自动增长的主键
      Primary p = field.getAnnotation(Primary.class);
      if (p.autoincrement()) {
        return true;
      }
    }

    return false;
  }
}
