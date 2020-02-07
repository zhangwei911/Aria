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
package com.arialyy.aria.exception;

/**
 * Created by lyy on 2017/1/18.
 * Aria 文件异常
 */
public class FileNotFoundException extends BaseException {
    private static final String ARIA_FILE_EXCEPTION = "Aria File not found Exception:";

    public FileNotFoundException(String tag, String detailMessage) {
        super(tag, String.format("%s%s", ARIA_FILE_EXCEPTION, detailMessage));
    }

    public FileNotFoundException(String tag, String message, Exception e) {
        super(tag, message, e);
    }
}
