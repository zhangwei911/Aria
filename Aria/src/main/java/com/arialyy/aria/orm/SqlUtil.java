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

import android.text.TextUtils;
import com.arialyy.aria.orm.annotation.Default;
import com.arialyy.aria.orm.annotation.Foreign;
import com.arialyy.aria.orm.annotation.Ignore;
import com.arialyy.aria.orm.annotation.Many;
import com.arialyy.aria.orm.annotation.NoNull;
import com.arialyy.aria.orm.annotation.One;
import com.arialyy.aria.orm.annotation.Primary;
import com.arialyy.aria.orm.annotation.Unique;
import com.arialyy.aria.orm.annotation.Wrapper;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lyy on 2017/7/24.
 * sql工具
 */
final class SqlUtil {

  /**
   * 获取主键字段名
   */
  static String getPrimaryName(Class clazz) {
    List<Field> fields = CommonUtil.getAllFields(clazz);
    String column;
    if (fields != null && !fields.isEmpty()) {

      for (Field field : fields) {
        field.setAccessible(true);
        if (isPrimary(field)) {
          column = field.getName();
          return column;
        }
      }
    }
    return null;
  }

  /**
   * 获取类中所有不被忽略的字段
   */
  static List<Field> getAllNotIgnoreField(Class clazz) {
    List<Field> fields = CommonUtil.getAllFields(clazz);
    List<Field> temp = new ArrayList<>();
    if (fields != null && fields.size() > 0) {
      for (Field f : fields) {
        f.setAccessible(true);
        if (!isIgnore(f)) {
          temp.add(f);
        }
      }
      return temp;
    } else {
      return null;
    }
  }

  /**
   * 列表数据转字符串
   *
   * @param field list反射字段
   */
  static String list2Str(DbEntity dbEntity, Field field) throws IllegalAccessException {
    List list = (List) field.get(dbEntity);
    if (list == null || list.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (Object aList : list) {
      sb.append(aList).append("$$");
    }
    return sb.toString();
  }

  /**
   * 字符串转列表
   *
   * @param str 数据库中的字段
   * @return 如果str为null，则返回null
   */
  static List str2List(String str, Field field) {
    if (TextUtils.isEmpty(str)) return null;
    String[] datas = str.split("\\$\\$");
    List list = new ArrayList();
    Class clazz = CommonUtil.getListParamType(field);
    if (clazz != null) {
      String type = clazz.getName();
      for (String data : datas) {
        list.add(checkData(type, data));
      }
    }

    return list;
  }

  /**
   * 字符串转Map，只支持
   * <pre>
   *   {@code Map<String, String>}
   * </pre>
   */
  static Map<String, String> str2Map(String str) {
    Map<String, String> map = new HashMap<>();
    if (TextUtils.isEmpty(str)) {
      return map;
    }
    boolean isDecode = false;
    if (str.endsWith("_&_decode_&_")) {
      isDecode = true;
      str = str.substring(0, str.length() - 12);
    }
    String[] element = str.split(",");
    for (String data : element) {
      String[] s = data.split("\\$");
      if (isDecode) {
        map.put(CommonUtil.decryptBASE64(s[0]), CommonUtil.decryptBASE64(s[1]));
      } else {
        map.put(s[0], s[1]);
      }
    }
    return map;
  }

  /**
   * Map转字符串，只支持
   * <pre>
   *   {@code Map<String, String>}
   * </pre>
   */
  static String map2Str(Map<String, String> map) {
    StringBuilder sb = new StringBuilder();
    Set<String> keys = map.keySet();
    for (String key : keys) {
      sb.append(CommonUtil.encryptBASE64(key))
          .append("$")
          .append(CommonUtil.encryptBASE64(map.get(key)))
          .append(",");
    }
    String str = sb.toString();
    str = TextUtils.isEmpty(str) ? str : str.substring(0, str.length() - 1);
    //3.3.10版本之前没有decode，需要加标志
    if (map.size() != 0) {
      str += "_&_decode_&_";
    }
    return str;
  }

  /**
   * shadow$_klass_、shadow$_monitor_、{@link Ignore}、rowID、{@link Field#isSynthetic()}、{@link
   * Modifier#isFinal(int)}、{@link Modifier#isStatic(int)}将被忽略
   *
   * @return true 忽略该字段
   */
  static boolean isIgnore(Field field) {
    // field.isSynthetic(), 使用as热启动App时，AS会自动给你的class添加change字段
    Ignore ignore = field.getAnnotation(Ignore.class);
    int modifiers = field.getModifiers();
    String fieldName = field.getName();
    return (ignore != null && ignore.value()) || fieldName.equals("rowID") || fieldName.equals(
        "shadow$_klass_") || fieldName.equals("shadow$_monitor_") || field.isSynthetic() || Modifier
        .isStatic(modifiers) || Modifier.isFinal(modifiers);
  }

  /**
   * 判断是否是Wrapper注解
   *
   * @return {@code true} 是
   */
  static boolean isWrapper(Class clazz) {
    Wrapper w = (Wrapper) clazz.getAnnotation(Wrapper.class);
    return w != null;
  }

  /**
   * 判断是否一对多注解
   */
  static boolean isMany(Field field) {
    Many oneToMany = field.getAnnotation(Many.class);
    return oneToMany != null;
  }

  /**
   * 判断是否是一对一注解
   */
  static boolean isOne(Field field) {
    One oneToOne = field.getAnnotation(One.class);
    return oneToOne != null;
  }

  /**
   * 判断是否是主键约束
   *
   * @return {@code true}主键约束
   */
  static boolean isPrimary(Field field) {
    Primary pk = field.getAnnotation(Primary.class);
    return pk != null;
  }

  /**
   * 判断是否是外键约束
   *
   * @return {@code true}外键约束
   */
  static boolean isForeign(Field field) {
    Foreign fk = field.getAnnotation(Foreign.class);
    return fk != null;
  }

  /**
   * 判断是否是非空约束
   *
   * @return {@code true}为非空约束
   */
  static boolean isNoNull(Field field) {
    NoNull nn = field.getAnnotation(NoNull.class);
    return nn != null;
  }

  /**
   * 判断是否是default
   *
   * @return {@code true}为default
   */
  static boolean isDefault(Field field) {
    Default nn = field.getAnnotation(Default.class);
    return nn != null;
  }

  /**
   * 判断是否是Unique
   *
   * @return {@code true}为Unique
   */
  static boolean isUnique(Field field) {
    Unique nn = field.getAnnotation(Unique.class);
    return nn != null;
  }

  private static Object checkData(String type, String data) {
    if (type.equalsIgnoreCase("java.lang.String")) {
      return data;
    } else if (type.equalsIgnoreCase("int") || type.equals("java.lang.Integer")) {
      return Integer.parseInt(data);
    } else if (type.equalsIgnoreCase("double") || type.equals("java.lang.Double")) {
      return Double.parseDouble(data);
    } else if (type.equalsIgnoreCase("float") || type.equals("java.lang.Float")) {
      return Float.parseFloat(data);
    }
    return null;
  }
}
