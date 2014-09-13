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

/**
 * WorkflowExecutionItemImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowExecutionItemImpl implements WorkflowExecutionItem {
    final private IWorkflow workflow;

    public WorkflowExecutionItemImpl(final IWorkflow workflow) {
        this.workflow = workflow;
    }

    public WorkflowExecutionItemImpl(WorkflowExecutionItem item) {
        this(item.getWorkflow());
    }

    public IWorkflow getWorkflow() {
        return workflow;
    }

    public String getType() {
        if (NODE_FIRST.equals(getWorkflow().getStrategy())) {
            return COMMAND_TYPE_NODE_FIRST;
        } else if (STEP_FIRST.equals(getWorkflow().getStrategy())) {
            return COMMAND_TYPE_STEP_FIRST;
        } else if (PARALLEL.equals(getWorkflow().getStrategy())) {
            return COMMAND_TYPE_PARALLEL;
        }

        throw new IllegalArgumentException("Invalid workflow strategy: [" + getWorkflow().getStrategy() + "]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowExecutionItemImpl)) {
            return false;
        }

        WorkflowExecutionItemImpl that = (WorkflowExecutionItemImpl) o;

        if (workflow != null ? !workflow.equals(that.workflow) : that.workflow != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return workflow != null ? workflow.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "WorkflowExecutionItemImpl{" +
               "workflow=" + workflow +
               '}';
    }
}
