/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.logging;

/**
 * Indicates an error with a log storage request
 * @deprecated no longer used, see {@link ExecutionFileStorageException}
 */
public class LogFileStorageException extends Exception {
    public LogFileStorageException() {
    }

    public LogFileStorageException(String s) {
        super(s);
    }

    public LogFileStorageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LogFileStorageException(Throwable throwable) {
        super(throwable);
    }
}
