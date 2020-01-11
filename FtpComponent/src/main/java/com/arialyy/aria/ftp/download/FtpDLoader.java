///*
// * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.arialyy.aria.ftp.download;
//
//import com.arialyy.aria.core.TaskRecord;
//import com.arialyy.aria.core.common.SubThreadConfig;
//import com.arialyy.aria.core.listener.IEventListener;
//import com.arialyy.aria.core.loader.NormalTTBuilder;
//import com.arialyy.aria.core.loader.IRecordHandler;
//import com.arialyy.aria.core.loader.IThreadTaskBuilder;
//import com.arialyy.aria.core.loader.NormalLoader;
//import com.arialyy.aria.core.wrapper.AbsTaskWrapper;
//import com.arialyy.aria.util.ALog;
//import com.arialyy.aria.util.FileUtil;
//import java.io.File;
//import java.lang.reflect.InvocationHandler;
//import java.lang.reflect.Method;
//import java.lang.reflect.Proxy;
//
//final class FtpDLoader extends NormalLoader {
//  FtpDLoader(AbsTaskWrapper wrapper, IEventListener listener) {
//    super(wrapper, listener);
//  }
//
//  @Override public void addComponent(IThreadTaskBuilder builder) {
//    mTTBuilder = (IThreadTaskBuilder) Proxy.newProxyInstance(getClass().getClassLoader(),
//        NormalTTBuilder.class.getInterfaces(), new InvocationHandler() {
//          NormalTTBuilder target = new NormalTTBuilder(mTaskWrapper);
//          @Override public Object invoke(Object proxy, Method method, Object[] args)
//              throws Exception {
//            if (method.getDeclaringClass() == Object.class) {
//              return method.invoke(this, args);
//            }
//
//            //if (method.isDefault()) {
//            //  Constructor<MethodHandles.Lookup>
//            //      constructor =
//            //      MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
//            //  constructor.setAccessible(true);
//            //  return constructor.newInstance(AbsNormalTTBuilder.class, -1 /* trusted */)
//            //      .unreflectSpecial(method, AbsNormalTTBuilder.class)
//            //      .bindTo(proxy)
//            //      .invokeWithArguments(args);
//            //}
//
//            String methodName = method.getName();
//            switch (methodName) {
//
//              case "handleNewTask":
//                return handleNewTask((TaskRecord) args[0], (int) args[1]);
//              case "getAdapter":
//                return new FtpDThreadTaskAdapter((SubThreadConfig) args[0]);
//            }
//
//            return method.invoke(target, args);
//          }
//        });
//  }
//
//  /**
//   * 处理新任务
//   */
//  private boolean handleNewTask(TaskRecord record, int totalThreadNum) {
//    File temp = new File(getEntity().getFilePath());
//    if (!record.isBlock) {
//      if (temp.exists()) {
//        FileUtil.deleteFile(temp);
//      }
//      //CommonUtil.createFile(mTempFile.getPath());
//    } else {
//      for (int i = 0; i < totalThreadNum; i++) {
//        File blockFile =
//            new File(String.format(IRecordHandler.SUB_PATH, temp.getPath(), i));
//        if (blockFile.exists()) {
//          ALog.d(TAG, String.format("分块【%s】已经存在，将删除该分块", i));
//          FileUtil.deleteFile(blockFile);
//        }
//      }
//    }
//    return true;
//  }
//}
