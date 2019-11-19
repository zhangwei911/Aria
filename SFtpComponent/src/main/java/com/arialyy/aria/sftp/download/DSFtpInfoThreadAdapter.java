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
package com.arialyy.aria.sftp.download;

import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.sftp.BaseInfoThreadAdapter;
import java.util.Vector;

final class DSFtpInfoThreadAdapter extends BaseInfoThreadAdapter<DTaskWrapper> {

  DSFtpInfoThreadAdapter(DTaskWrapper taskWrapper) {
    super(taskWrapper);
  }

  @Override protected boolean handlerFile(Vector vector) {
    return false;
  }
}
