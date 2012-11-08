/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* NodeDispatchedWorkflowExecutionItemImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/5/12 11:55 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;


/**
 * NodeDispatchedWorkflowExecutionItemImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatchedWorkflowExecutionItemImpl extends WorkflowExecutionItemImpl implements
                                                                                       NodeStepExecutionItem {
    public NodeDispatchedWorkflowExecutionItemImpl(final IWorkflow workflow) {
        super(workflow);
    }

    public NodeDispatchedWorkflowExecutionItemImpl(WorkflowExecutionItem item) {
        super(item);
    }

    @Override
    public String getNodeStepType() {
        return NodeDispatchStepExecutor.STEP_EXECUTION_TYPE;
    }
}
