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

/**
 * Represents a file produced by an execution producer
 */
interface ExecutionFile {
    File getLocalFile()

    /**
     * @return policy for deleting this file
     */
    ExecutionFileDeletePolicy getFileDeletePolicy()
}

enum ExecutionFileDeletePolicy {
    /**
     * Always delete the produced file (e.g. temp file usage)
     */
    ALWAYS,
    /**
     * Never delete the produced file (required for other use)
     */
    NEVER,
    /**
     * Delete only when it can be retrieved again later
     */
    WHEN_RETRIEVABLE
}

class ExecutionFileUtil {
    public static deleteExecutionFilePerPolicy(ExecutionFile file, boolean canRetrieve) {
        if (file.fileDeletePolicy == ExecutionFileDeletePolicy.ALWAYS) {
            file.localFile.delete()
        } else if (file.fileDeletePolicy == ExecutionFileDeletePolicy.WHEN_RETRIEVABLE && canRetrieve) {
            //todo: cache/delete after timeout
            file.localFile.deleteOnExit()
        }
    }
}

class ProducedExecutionFile implements ExecutionFile {
    File localFile
    ExecutionFileDeletePolicy fileDeletePolicy
}