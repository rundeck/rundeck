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
* WorkflowExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 1:44 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;

/**
 * WorkflowExecutionListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface WorkflowExecutionListener extends ExecutionListener {

    public void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item);

    public void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext,
                                        WorkflowExecutionItem item);
    public void beginWorkflowItem(int step, StepExecutionItem node);
    public void finishWorkflowItem(int step, StepExecutionItem node);
}
