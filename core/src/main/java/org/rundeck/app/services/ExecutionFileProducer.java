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

package org.rundeck.app.services;

import com.dtolabs.rundeck.core.execution.ExecutionReference;

/**
 * Produces files that need to be stored for an execution
 */
public interface ExecutionFileProducer {
    /**
     * @return the filetype string
     */
    String getExecutionFileType();

    /**
     * @return true if the file will be generated, false if it was previously generated
     */
    boolean isExecutionFileGenerated();

    /**
     * @return true if the file can be written at checkpoints
     */
    boolean isCheckpointable();

    /**
     * @param e execution
     * @return the file to store
     */
    ExecutionFile produceStorageFileForExecution(ExecutionReference e);

    ExecutionFile produceStorageCheckpointForExecution(ExecutionReference e);
}
