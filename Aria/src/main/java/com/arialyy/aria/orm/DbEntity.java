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

import java.util.List;

/**
 * Created by lyy on 2015/11/2.
 * 所有数据库实体父类
 */
public abstract class DbEntity {
  private static final Object LOCK = new Object();
  protected long rowID = -1;

  protected DbEntity() {

  }

  ///**
  // * 保存关联数据
  // */
  //public static void saveRelationData(AbsWrapper wrapper) {
  //  DelegateWrapper.getInstance().saveRelationData(wrapper);
  //}

  /**
   * 查询关联数据
   *
   * @param expression 查询条件
   */
  public static <T extends AbsWrapper> List<T> findRelationData(Class<T> clazz,
      String... expression) {
    return DelegateWrapper.getInstance().findRelationData(clazz, expression);
  }

  /**
   * 检查某个字段的值是否存在
   *
   * @param expression 字段和值"downloadPath=?"
   * @return {@code true}该字段的对应的value已存在
   */
  public static boolean checkDataExist(Class clazz, String... expression) {
    return DelegateWrapper.getInstance().checkDataExist(clazz, expression);
  }

  /**
   * 清空表数据
   */
  public static <T extends DbEntity> void clean(Class<T> clazz) {
    DelegateWrapper.getInstance().clean(clazz);
  }

  /**
   * 直接执行sql语句
   */
  public static void exeSql(String sql) {
    DelegateWrapper.getInstance().exeSql(sql);
  }

  /**
   * 查询所有数据
   *
   * @return 没有数据返回null
   */
  public static <T extends DbEntity> List<T> findAllData(Class<T> clazz) {
    DelegateWrapper util = DelegateWrapper.getInstance();
    return util.findAllData(clazz);
  }

  /**
   * 查询第一条数据
   */
  public static <T extends DbEntity> T findFirst(Class<T> clazz) {
    List<T> list = findAllData(clazz);
    return (list == null || list.size() == 0) ? null : list.get(0);
  }

  /**
   * 查询一组数据
   * <code>
   * DownloadEntity.findFirst(DownloadEntity.class, "downloadUrl=?", downloadUrl);
   * </code>
   *
   * @return 没有数据返回null
   */
  public static <T extends DbEntity> List<T> findDatas(Class<T> clazz, String... expression) {
    DelegateWrapper util = DelegateWrapper.getInstance();
    return util.findData(clazz, expression);
  }

  /**
   * 模糊查询一组数据
   * <code>
   * DownloadEntity.findDatas(DownloadEntity.class, "downloadUrl like http://");
   * </code>
   *
   * @return 没有数据返回null
   */
  public static <T extends DbEntity> List<T> findDataByFuzzy(Class<T> clazz, String conditions) {
    DelegateWrapper util = DelegateWrapper.getInstance();
    return util.findDataByFuzzy(clazz, conditions);
  }

  /**
   * 查询一行数据
   * <code>
   * DownloadEntity.findFirst(DownloadEntity.class, "downloadUrl=?", downloadUrl);
   * </code>
   *
   * @return 没有数据返回null
   */
  public static <T extends DbEntity> T findFirst(Class<T> clazz, String... expression) {
    DelegateWrapper util = DelegateWrapper.getInstance();
    List<T> datas = util.findData(clazz, expression);
    return datas == null ? null : datas.size() > 0 ? datas.get(0) : null;
  }

  /**
   * 删除当前数据
   */
  public void deleteData() {
    deleteData(getClass(), "rowid=?", rowID + "");
  }

  /**
   * 根据条件删除数据
   * <code>
   * DownloadEntity.deleteData(DownloadEntity.class, "downloadUrl=?", downloadUrl);
   * </code>
   */
  public static <T extends DbEntity> void deleteData(Class<T> clazz, String... expression) {
    DelegateWrapper util = DelegateWrapper.getInstance();
    util.delData(clazz, expression);
  }

  /**
   * 修改数据
   */
  public void update() {
    DelegateWrapper.getInstance().modifyData(this);
  }

  /**
   * 保存自身，如果表中已经有数据，则更新数据，否则插入数据
   * 只有 target中checkEntity成功后才能保存，创建实体部分也不允许保存
   */
  public void save() {
    synchronized (LOCK) {
      if (thisIsExist()) {
        update();
      } else {
        insert();
      }
    }
  }

  /**
   * 查找数据在表中是否存在
   */
  private boolean thisIsExist() {
    DelegateWrapper util = DelegateWrapper.getInstance();
    return rowID != -1 && util.isExist(getClass(), rowID);
  }

  /**
   * 表是否存在
   * @return  {@code true} 存在
   */
  public static boolean tableExists(Class<DbEntity> clazz){
    return DelegateWrapper.getInstance().tableExists(clazz);
  }

  /**
   * 插入数据，只有 target中checkEntity成功后才能插入，创建实体部分也不允许操作
   */
  public void insert() {
    DelegateWrapper.getInstance().insertData(this);
  }
}