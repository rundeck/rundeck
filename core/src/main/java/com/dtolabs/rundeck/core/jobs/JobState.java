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

package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.dispatcher.ExecutionState;

import java.util.Set;

/**
 * The state of a job
 */
public interface JobState {
    /**
     * @return true if the job is currently running
     */
    public boolean isRunning();

    /**
     * @return if running, return the execution ID(s) of the execution. otherwise return null.
     */
    public Set<String> getRunningExecutionIds();

    /**
     * @return the execution state of the last execution, or null if not execution has completed.
     */
    public ExecutionState getPreviousExecutionState();

    /**
     * @return the custom status string of the last execution, or null if not set
     */
    public String getPreviousExecutionStatusString();
}
