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

import com.dtolabs.rundeck.core.execution.ExecutionNotFound;
import com.dtolabs.rundeck.core.execution.ExecutionReference;
import org.rundeck.app.spi.AppService;

/**
 * Provides execution file loading
 */
public interface ExecutionFileLoaderService
        extends AppService, ExecutionFileManagerService
{
    /**
     * Request loading of an execution file
     * @param e execution reference
     * @param filetype file type
     * @param performLoad if true, perform any remote loading asynchronously
     * @return loader state
     * @throws ExecutionNotFound if not found
     */
    ExecutionFileLoader requestFileLoad(ExecutionReference e, String filetype, boolean performLoad) throws
                                                                                                       ExecutionNotFound;

}
