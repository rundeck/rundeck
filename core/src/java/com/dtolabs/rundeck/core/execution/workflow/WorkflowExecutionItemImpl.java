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
* WorkflowExecutionItemImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 10:44:41 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.ExecutionItem;

import java.util.List;

/**
 * WorkflowExecutionItemImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowExecutionItemImpl implements WorkflowExecutionItem {
    final private IWorkflow workflow;

    public String getType() {
        if (NODE_FIRST.equals(getWorkflow().getStrategy())) {
            return COMMAND_TYPE_NODE_FIRST;
        } else {
            return COMMAND_TYPE_STEP_FIRST;
        }
    }

    public WorkflowExecutionItemImpl(final IWorkflow workflow) {
        this.workflow = workflow;
    }

    public WorkflowExecutionItemImpl(WorkflowExecutionItem item) {
        this(item.getWorkflow());
    }

    /**
     * Create workflowExecutionItem suitable for inner loop of node-first strategy
     */
    public static WorkflowExecutionItem createInnerLoopItem(WorkflowExecutionItem item) {
        final WorkflowExecutionItemImpl workflowExecutionItem = new WorkflowExecutionItemImpl(new stepFirstWrapper(
            item.getWorkflow()));
        return workflowExecutionItem;
    }

    public IWorkflow getWorkflow() {
        return workflow;
    }

    private static class stepFirstWrapper implements IWorkflow {
        private IWorkflow workflow;

        private stepFirstWrapper(IWorkflow workflow) {
            this.workflow = workflow;
        }

        public List<ExecutionItem> getCommands() {
            return workflow.getCommands();
        }

        public int getThreadcount() {
            return workflow.getThreadcount();
        }

        public boolean isKeepgoing() {
            return workflow.isKeepgoing();
        }

        public String getStrategy() {
            return WorkflowExecutionItem.STEP_FIRST;
        }
    }

}
