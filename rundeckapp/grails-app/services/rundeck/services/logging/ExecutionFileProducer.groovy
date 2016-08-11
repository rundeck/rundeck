/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services.logging

import rundeck.Execution

/**
 * Produces files that need to be stored for an execution
 */
interface ExecutionFileProducer {
    /**
     * @return the filetype string
     */
    String getExecutionFileType()

    /**
     *
     * @return true if the file will be generated, false if it was previously generated
     */
    boolean isExecutionFileGenerated()

    /**
     * @param e execution
     * @return the file to store
     */
    ExecutionFile produceStorageFileForExecution(Execution e)
}