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
package com.arialyy.aria.core.inf;

import com.arialyy.aria.core.upload.UploadEntity;
import java.util.List;

/**
 * FTP文件上传拦截器，如果远端已有同名文件，可使用该拦截器控制覆盖文件或修改该文件上传到服务器端端文件名
 */
public interface IFtpUploadInterceptor {

  /**
   * 如果ftp服务器端已经有同名文件，控制是否覆盖远端的同名文件；
   * 如果你不希望覆盖远端文件，可以使用{@link #resetFileName(UploadEntity, List)}
   * 如果使用者同时实现{@link #resetFileName(UploadEntity, List)}和{@link #coverServerFile(UploadEntity,
   * List)}，将优先使用{@link #resetFileName(UploadEntity, List)}
   *
   * @param entity 上传信息实体
   * @param fileList ftp服务器端remotePath下的文件列表
   * @return {@code true} 如果ftp服务器端已经有同名文件，覆盖服务器端的同名文件
   */
  boolean coverServerFile(UploadEntity entity, List<String> fileList);

  /**
   * 如果ftp服务器端已经有同名文件，修改该文件上传到远端的文件名，该操作不会修改本地文件名
   * 如果你希望覆盖远端的同名文件，可以使用{@link #coverServerFile(UploadEntity, List)}
   * 如果使用者同时实现{@link #resetFileName(UploadEntity, List)}和{@link #coverServerFile(UploadEntity,
   * List)}，将优先使用{@link #resetFileName(UploadEntity, List)}
   *
   * @param entity 上传信息实体
   * @param fileList ftp服务器端remotePath下的文件列表
   * @return 该文件上传到远端的新的文件名
   */
  String resetFileName(UploadEntity entity, List<String> fileList);
}
