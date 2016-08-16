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

import com.dtolabs.rundeck.core.logging.LogFileState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/23/13
 * Time: 1:39 PM
 */
public enum ExecutionLogState {
    /**
     * Not found at all
     */
    NOT_FOUND,
    /**
     * Present locally
     */
    AVAILABLE,
    /**
     * Waiting for output
     */
    WAITING,
    /**
     * Present on remote storage
     */
    AVAILABLE_REMOTE,
    /**
     * Pending presence on remote storage
     */
    PENDING_REMOTE,
    /**
     * Pending presence on local storage (being copied)
     */
    PENDING_LOCAL,
    /**
     * Error determining state
     */
    ERROR
    /**
     * Return an {@link ExecutionLogState} given a local and remote {@link LogFileState}
     * @param local
     * @param remote
     * @return
     */
    public static ExecutionLogState forFileStates(LogFileState local, LogFileState remote) {
        return forFileStates(local, remote, null)
    }
    /**
     * Return an {@link ExecutionLogState} given a local and remote {@link LogFileState}
     * @param local
     * @param remote
     * @param notFoundState a state to return if both states are NOT_FOUND
     * @return
     */
    public static ExecutionLogState forFileStates(LogFileState local, LogFileState remote, ExecutionLogState notFoundState) {
        switch (local) {
            case LogFileState.AVAILABLE:
                return AVAILABLE
            case LogFileState.PENDING:
                return PENDING_LOCAL
            case LogFileState.NOT_FOUND:
                switch (remote) {
                    case LogFileState.ERROR:
                        return ERROR
                    case LogFileState.AVAILABLE:
                        return AVAILABLE_REMOTE
                    case LogFileState.PENDING:
                        return PENDING_REMOTE
                    case LogFileState.NOT_FOUND:
                        if(null!=notFoundState){
                            return notFoundState
                        }
                }
        }
        return NOT_FOUND
    }
}
