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
package com.arialyy.compiler;

import com.arialyy.annotations.Download;
import com.arialyy.annotations.DownloadGroup;
import com.arialyy.annotations.Upload;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Created by lyy on 2017/9/6.
 * 构建代理文件的参数获取工具
 */
class ParamObtainUtil {
  private Map<String, ProxyClassParam> mMethodParams = new HashMap<>();
  private Map<String, Set<String>> mListenerClass = new HashMap<>();
  private Elements mElementUtil;

  ParamObtainUtil(Elements elements) {
    mElementUtil = elements;
  }

  /**
   * 获取需要创建的代理类参数
   */
  Map<String, Set<String>> getListenerClass() {
    return mListenerClass;
  }

  /**
   * 获取搜索到的代理方法参数
   */
  Map<String, ProxyClassParam> getMethodParams() {
    return mMethodParams;
  }

  /**
   * 查找并保存扫描到的方法
   */
  void saveMethod(TaskEnum taskEnum, RoundEnvironment roundEnv,
      Class<? extends Annotation> annotationClazz, int annotationType) {
    for (Element element : roundEnv.getElementsAnnotatedWith(annotationClazz)) {
      ElementKind kind = element.getKind();
      if (kind == ElementKind.METHOD) {
        ExecutableElement method = (ExecutableElement) element;
        TypeElement classElement = (TypeElement) method.getEnclosingElement();
        PackageElement packageElement = mElementUtil.getPackageOf(classElement);

        String methodName = method.getSimpleName().toString();
        String className = method.getEnclosingElement().toString(); //全类名\
        String key = className + taskEnum.proxySuffix;
        ProxyClassParam proxyEntity = mMethodParams.get(key);
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.methodName = methodName;
        methodInfo.params = (List<VariableElement>) method.getParameters();
        checkMethod(taskEnum, method, annotationClazz, methodInfo.params);

        if (proxyEntity == null) {
          proxyEntity = new ProxyClassParam();
          proxyEntity.taskEnums = new HashSet<>();
          proxyEntity.packageName = packageElement.getQualifiedName().toString();
          proxyEntity.className = classElement.getSimpleName().toString();
          proxyEntity.proxyClassName = proxyEntity.className + taskEnum.proxySuffix;
          proxyEntity.mainTaskEnum = taskEnum;
          if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB || taskEnum == TaskEnum.DOWNLOAD_GROUP) {
            proxyEntity.subTaskEnum = TaskEnum.DOWNLOAD_ENTITY;
          }
          mMethodParams.put(key, proxyEntity);
        }
        proxyEntity.taskEnums.add(taskEnum);
        if (proxyEntity.methods.get(taskEnum) == null) {
          proxyEntity.methods.put(taskEnum, new HashMap<Class<? extends Annotation>, MethodInfo>());
        }

        proxyEntity.methods.get(taskEnum).put(annotationClazz, methodInfo);
        proxyEntity.keyMappings.put(methodName, getValues(taskEnum, method, annotationType));
      }
    }
  }

  /**
   * 获取注解的内容
   */
  private Set<String> getValues(TaskEnum taskEnum, ExecutableElement method, int annotationType) {
    String clsName = method.getEnclosingElement().toString();
    String[] keys = null;
    switch (taskEnum) {
      case DOWNLOAD:
        keys = ValuesUtil.getDownloadValues(method, annotationType);
        addListenerMapping(clsName, ProxyConstance.COUNT_DOWNLOAD);
        break;
      case UPLOAD:
        keys = ValuesUtil.getUploadValues(method, annotationType);
        addListenerMapping(clsName, ProxyConstance.COUNT_UPLOAD);
        break;
      case DOWNLOAD_GROUP:
        keys = ValuesUtil.getDownloadGroupValues(method, annotationType);
        addListenerMapping(clsName, ProxyConstance.COUNT_DOWNLOAD_GROUP);
        break;
      case DOWNLOAD_GROUP_SUB:
        keys = ValuesUtil.getDownloadGroupSubValues(method, annotationType);
        addListenerMapping(clsName, ProxyConstance.COUNT_DOWNLOAD_GROUP_SUB);
        break;
    }
    return keys == null ? null : convertSet(keys);
  }

  /**
   * 添加方法映射
   *
   * @param clsName 注解事件的类
   * @param key {@link ProxyConstance#COUNT_DOWNLOAD}、{@link ProxyConstance#COUNT_UPLOAD}、{@link
   * ProxyConstance#COUNT_DOWNLOAD_GROUP}、{@link ProxyConstance#COUNT_DOWNLOAD_GROUP_SUB}
   */
  void addListenerMapping(String clsName, String key) {
    Set<String> cls = mListenerClass.get(key);
    if (cls == null) {
      cls = new HashSet<>();
      mListenerClass.put(key, cls);
    }
    cls.add(clsName);
  }

  /**
   * 检查和下载相关的方法，如果被注解的方法为private或参数不合法，则抛异常
   */
  private void checkMethod(TaskEnum taskEnum, ExecutableElement method,
      Class<? extends Annotation> annotationClazz, List<VariableElement> params) {
    String methodName = method.getSimpleName().toString();
    String className = method.getEnclosingElement().toString();
    Set<Modifier> modifiers = method.getModifiers();
    if (modifiers.contains(Modifier.PRIVATE)) {
      throw new IllegalAccessError(String.format("%s.%s, 不能为private方法", className, methodName));
    }
    if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB) {
      if (isFailAnnotation(annotationClazz)) {
        if (params.size() != 3 && params.size() != 2) {
          throw new IllegalArgumentException(
              String.format("%s.%s参数错误, 参数只能是两个或三个，第一个参数是：%s，第二个参数是：%s，第三个参数（可选）是：%s", className,
                  methodName,
                  getCheckParams(taskEnum), getCheckSubParams(taskEnum),
                  Exception.class.getSimpleName()));
        }
      } else {
        if (params.size() != 2) {
          throw new IllegalArgumentException(
              String.format("%s.%s参数错误, 参数只能是两个，第一个参数是：%s，第二个参数是：%s", className, methodName,
                  getCheckParams(taskEnum), getCheckSubParams(taskEnum)));
        }
      }
    } else {
      if (isFailAnnotation(annotationClazz)) {
        if (params.size() != 1 && params.size() != 2) {
          throw new IllegalArgumentException(
              String.format("%s.%s参数错误, 参数只能有一个或两个，第一个参数是：%s，第二个参（可选）数是：%s", className, methodName,
                  getCheckParams(taskEnum), Exception.class.getSimpleName()));
        }
      } else {
        if (params.size() != 1) {
          throw new IllegalArgumentException(
              String.format("%s.%s参数错误, 参数只能有一个，且参数必须是：%s", className, methodName,
                  getCheckParams(taskEnum)));
        }
      }
    }
    if (!params.get(0).asType().toString().equals(getCheckParams(taskEnum))) {
      throw new IllegalArgumentException(
          String.format("%s.%s参数【%s】类型错误，参数必须是：%s", className, methodName,
              params.get(0).getSimpleName(), getCheckParams(taskEnum)));
    }
    if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB) {
      if (!params.get(1).asType().toString().equals(getCheckSubParams(taskEnum))) {
        throw new IllegalArgumentException(
            String.format("%s.%s参数【%s】类型错误，参数必须是：%s", className, methodName,
                params.get(0).getSimpleName(), getCheckSubParams(taskEnum)));
      }
    }
  }

  /**
   * 判断是否是任务失败的回调注解
   *
   * @return ｛@code true｝是任务失败的回调注解
   */
  private boolean isFailAnnotation(Class<? extends Annotation> annotationClazz) {
    return annotationClazz == Download.onTaskFail.class
        || annotationClazz == DownloadGroup.onTaskFail.class
        || annotationClazz == DownloadGroup.onSubTaskFail.class
        || annotationClazz == Upload.onTaskFail.class;
  }

  /**
   * 字符串数组转set
   *
   * @param keys 注解中查到的key
   */
  private Set<String> convertSet(final String[] keys) {
    if (keys == null || keys.length == 0) {
      return null;
    }
    if (keys[0].isEmpty()) return null;
    Set<String> set = new HashSet<>();
    Collections.addAll(set, keys);
    return set;
  }

  private String getCheckParams(TaskEnum taskEnum) {
    return taskEnum.pkg + "." + taskEnum.className;
  }

  /**
   * 检查任务组子任务第二个参数
   */
  private String getCheckSubParams(TaskEnum taskEnum) {
    if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB) {
      return TaskEnum.DOWNLOAD_ENTITY.pkg + "." + TaskEnum.DOWNLOAD_ENTITY.className;
    }
    return "";
  }
}
