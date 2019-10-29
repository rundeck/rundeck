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

import com.dtolabs.rundeck.core.execution.ExecutionReference;
import org.rundeck.app.spi.AppService;

import java.io.File;

/**
 * methods for local file paths
 */
public interface ExecutionFileManagerService
        extends AppService
{
    /**
     * @return local dir to store logs
     */
    File getLocalLogsDir();

    /**
     * @param execution execution
     * @param filetype  file type string
     * @return local file path for given file type for the execution
     */
    File getLocalExecutionFileForType(
            ExecutionReference execution,
            String filetype
    );

    /**
     * @param execution execution
     * @param filetype  file type string
     * @param partial   true for partial file
     * @return local file for given partial file type for the execution
     */
    File getLocalExecutionFileForType(
            ExecutionReference execution,
            String filetype,
            boolean partial
    );
}
