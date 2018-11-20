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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

/**
 * Created by lyy on 2017/9/6.
 * 各类注解统计技术类
 */
@Deprecated final class CountFiler {
  private Filer mFiler;
  private ParamObtainUtil mPbUtil;

  CountFiler(Filer filer, ParamObtainUtil pbUtil) {
    mFiler = filer;
    mPbUtil = pbUtil;
  }

  /**
   * 每一种注解对应的统计类
   */
  void createCountFile() throws IOException {
    Set<String> keys = mPbUtil.getListenerClass().keySet();
    if (keys.size() == 0) return;
    TypeSpec.Builder builder = TypeSpec.classBuilder(
        ProxyConstance.PROXY_COUNTER_NAME + UUID.randomUUID().toString().replace("-", ""))
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    FieldSpec mappingField = FieldSpec.builder(
        ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
            ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(String.class))),
        ProxyConstance.PROXY_COUNTER_MAP)
        .addModifiers(Modifier.PRIVATE)
        .initializer("new $T()", HashMap.class)
        .build();
    builder.addField(mappingField);

    //增加构造函数
    CodeBlock.Builder cb = CodeBlock.builder();
    cb.add("Set<String> set = null;\n");
    for (String key : keys) {
      addTypeData(key, mPbUtil.getListenerClass().get(key), cb);
    }
    MethodSpec structure =
        MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addCode(cb.build()).build();
    builder.addMethod(structure);

    builder.addMethod(
        createMethod(ProxyConstance.COUNT_METHOD_DOWNLOAD, ProxyConstance.COUNT_DOWNLOAD));
    builder.addMethod(
        createMethod(ProxyConstance.COUNT_METHOD_UPLOAD, ProxyConstance.COUNT_UPLOAD));
    builder.addMethod(createMethod(ProxyConstance.COUNT_METHOD_DOWNLOAD_GROUP,
        ProxyConstance.COUNT_DOWNLOAD_GROUP));
    builder.addMethod(createMethod(ProxyConstance.COUNT_METHOD_DOWNLOAD_GROUP_SUB,
        ProxyConstance.COUNT_DOWNLOAD_GROUP_SUB));

    JavaFile jf = JavaFile.builder(ProxyConstance.PROXY_COUNTER_PACKAGE, builder.build()).build();
    createFile(jf);
  }

  /**
   * 创建不同任务类型的代理类集合
   *
   * @param key {@link ParamObtainUtil#addListenerMapping(String, String)}
   */
  private MethodSpec createMethod(String methodName, String key) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);
    ParameterizedTypeName returnName =
        ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(String.class));
    builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .returns(returnName)
        .addCode("return " + ProxyConstance.PROXY_COUNTER_MAP + ".get(\"" + key + "\");\n");
    return builder.build();
  }

  /**
   * 添加每一种注解对应类
   *
   * @param type {@link ParamObtainUtil#addListenerMapping(String, String)}
   */
  private void addTypeData(String type, Set<String> clsNames, CodeBlock.Builder cb) {
    if (clsNames == null || clsNames.isEmpty()) return;
    StringBuilder sb = new StringBuilder();
    sb.append("set = new $T();\n");
    for (String clsName : clsNames) {
      sb.append("set.add(\"").append(clsName).append("\");\n");
    }
    sb.append("typeMapping.put(\"").append(type).append("\", ").append("set);\n");
    cb.add(sb.toString(), ClassName.get(HashSet.class));
  }

  private void createFile(JavaFile jf) throws IOException {
    //jf.writeTo(System.out);
    if (ProxyConstance.DEBUG) {
      // 如果需要在控制台打印生成的文件，则去掉下面的注释
      jf.writeTo(System.out);
    } else {
      jf.writeTo(mFiler);
    }
  }
}
