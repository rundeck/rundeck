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

package com.dtolabs.rundeck.core.execution.workflow.state;

/**
 * $INTERFACE is ... User: greg Date: 10/15/13 Time: 2:52 PM
 */
public enum ExecutionState {
    /**
     * Waiting to start running
     */
    WAITING,
    /**
     * Currently running
     */
    RUNNING,
    /**
     * Running error handler
     */
    RUNNING_HANDLER,
    /**
     * Finished running successfully
     */
    SUCCEEDED,
    /**
     * Finished with a failure
     */
    FAILED,
    /**
     * Execution was aborted
     */
    ABORTED,
    /**
     * Partial success for some nodes
     */
    NODE_PARTIAL_SUCCEEDED,
    /**
     * Mixed states among nodes
     */
    NODE_MIXED,
    /**
     * After waiting the execution did not start
     */
    NOT_STARTED,
    ;

    public boolean isCompletedState() {
        return this == ExecutionState.ABORTED || this == ExecutionState.SUCCEEDED || this == ExecutionState.FAILED
                || this == ExecutionState.NODE_MIXED || this == ExecutionState.NODE_PARTIAL_SUCCEEDED
                || this == NOT_STARTED
                ;
    }
}
