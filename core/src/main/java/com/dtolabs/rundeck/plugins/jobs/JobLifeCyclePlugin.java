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

package com.dtolabs.rundeck.plugins.jobs;

import com.dtolabs.rundeck.core.execution.JobLifeCycleException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.logging.LoggingManager;

import java.util.Map;

/**
 * JobLifeCyclePlugin interface for a task to be executed during the job life cycle
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:22 AM
 */
public interface JobLifeCyclePlugin {

    /**
     * IT calls a service in order to know whether a job should run or not
     * @param item workflow data
     * @param executionContext notification configuration
     * @param workflowLogManager used to be able to log
     * @return true if successful
     */
    public boolean onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext, LoggingManager workflowLogManager)throws JobLifeCycleException;

}