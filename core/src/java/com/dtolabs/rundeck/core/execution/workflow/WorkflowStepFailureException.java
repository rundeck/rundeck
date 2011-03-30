/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* WorkflowStepFailureException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/24/11 9:45 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.ExecutionResult;

import java.util.*;

/**
* WorkflowStepFailureException is ...
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
public class WorkflowStepFailureException extends Exception {
    final private ExecutionResult executionResult;
    final private int workflowStep;

    public WorkflowStepFailureException(final String s, final ExecutionResult executionResult, final int workflowStep) {
        super(s);
        this.executionResult = executionResult;
        this.workflowStep = workflowStep;
    }

    public WorkflowStepFailureException(final String s, final Throwable throwable, final int workflowStep) {
        super(s, throwable);
        this.executionResult = null;
        this.workflowStep = workflowStep;
    }

    public ExecutionResult getExecutionResult() {
        return executionResult;
    }

    public int getWorkflowStep() {
        return workflowStep;
    }
}
