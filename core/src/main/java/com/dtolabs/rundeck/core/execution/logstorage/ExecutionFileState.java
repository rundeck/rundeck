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

import com.dtolabs.rundeck.core.logging.LogFileState;

/**
 * State of execution file
 */
public enum ExecutionFileState {
    NOT_FOUND,
    /**
     * Present locally
     */
    AVAILABLE,
    /**
     * Partial data present locally
     */
    AVAILABLE_PARTIAL,
    /**
     * Waiting for output
     */
    WAITING,
    /**
     * Present on remote storage
     */
    AVAILABLE_REMOTE,
    /**
     * Partial data on remote storage
     */
    AVAILABLE_REMOTE_PARTIAL,
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
    ERROR;

    /**
     *
     * @return AVAILABLE or AVAILABLE_PARTIAL
     */
    public boolean isAvailableOrPartial() {
        return this == AVAILABLE || this == AVAILABLE_PARTIAL;
    }

    /**
     * Return an {@link ExecutionFileState} given a local and remote {@link LogFileState}
     *
     * @param local
     * @param remote
     */
    public static ExecutionFileState forFileStates(LogFileState local, LogFileState remote) {
        return forFileStates(local, remote, null);
    }

    /**
     * Return an {@link ExecutionFileState} given a local and remote {@link LogFileState}
     *
     * @param local
     * @param remote
     * @param notFoundState a state to return if both states are NOT_FOUND
     */
    public static ExecutionFileState forFileStates(
            LogFileState local,
            LogFileState remote,
            ExecutionFileState notFoundState
    )
    {
        switch (local) {
            case AVAILABLE:
                return AVAILABLE;
            case AVAILABLE_PARTIAL:
                return AVAILABLE_PARTIAL;
            case PENDING:
                return PENDING_LOCAL;
            case NOT_FOUND:
                switch (remote) {
                    case ERROR:
                        return ERROR;
                    case AVAILABLE:
                        return AVAILABLE_REMOTE;
                    case AVAILABLE_PARTIAL:
                        return AVAILABLE_REMOTE_PARTIAL;
                    case PENDING:
                        return PENDING_REMOTE;
                    case NOT_FOUND:
                        if (null != notFoundState) {
                            return notFoundState;
                        }

                }
        }
        return NOT_FOUND;
    }

}
