/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.execution.logstorage;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * State of file loader
 */
public interface ExecutionFileLoader {
    /**
     * @return state of loaded file
     */
    ExecutionFileState getState();

    /**
     * @return error code
     */
    String getErrorCode();

    /**
     * @return data for error code
     */
    List<String> getErrorData();

    /**
     * @return local file if loaded
     */
    File getFile();

    /**
     * @return time in milliseconds the requestor should wait before requesting again
     */
    long getRetryBackoff();

    /**
     * @return the data as a stream
     */
    default InputStream getStream() { return null; }
}
