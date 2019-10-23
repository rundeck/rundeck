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

import java.util.concurrent.CompletableFuture;

/**
 * Provides async result when loading remote storage file
 */
public interface AsyncExecutionFileLoaderService
        extends ExecutionFileLoaderService
{
    /**
     * Request loading of an execution file, possibly remotely, and resolve the future when the file is available or an
     * error occurs or other state result
     *
     * @param e        execution reference
     * @param filetype file type
     * @return loader state
     * @throws ExecutionNotFound if not found
     */
    CompletableFuture<ExecutionFileLoader> requestFileLoadAsync(ExecutionReference e, String filetype)
            throws ExecutionNotFound;

}
