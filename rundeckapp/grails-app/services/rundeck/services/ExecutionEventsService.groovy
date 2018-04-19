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

package rundeck.services

import grails.gorm.transactions.Transactional
import rundeck.services.events.ExecutionCompleteEvent

@Transactional
class ExecutionEventsService {
    LogFileStorageService logFileStorageService

    /**
     * Prepares and submits logfile storage requests
     * @param event
     */
//    @Listener
    def executionComplete(ExecutionCompleteEvent e) {

        logFileStorageService.submitForStorage(e.execution)
    }
    /**
     * Checkpoint during job execution to allow partiallog storage
     * @param event
     */
//    @Listener
    def executionCheckpoint(ExecutionCompleteEvent e) {
        logFileStorageService.submitForPartialStorage(e.execution, e.context.fileSizeChange, e.context.timediff)
    }
}
