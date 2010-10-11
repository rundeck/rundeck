/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* WFFirstWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:16:49 PM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.BaseExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionService;

import java.util.ArrayList;
import java.util.List;

/**
 * StepFirstWorkflowStrategy iterates over the workflow steps and dispatches each one to all
 * nodes matching the filter.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class StepFirstWorkflowStrategy extends BaseWorkflowStrategy {

    public StepFirstWorkflowStrategy(WorkflowExecutionItem item, ExecutionService executionService,
                                     ExecutionListener listener, Framework framework) {
        super(item,executionService, listener, framework);
    }

    public ExecutionResult executeWorkflow() {
        //TODO: intialize a data context used for conditionals
        boolean workflowsuccess = false;
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        final List<String> failedList = new ArrayList<String>();
        final List resultList = new ArrayList();
        try {
            listener.log(Constants.DEBUG_LEVEL, "NodeSet: " + item.getNodeSet());
            listener.log(Constants.DEBUG_LEVEL, "Workflow: " + workflow);
            listener.log(Constants.DEBUG_LEVEL, "data context: " + item.getDataContext());

            final List<IWorkflowCmdItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                listener.log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            workflowsuccess = executeWorkflowItemsForNodeSet( workflow, failedList, resultList,
                iWorkflowCmdItems, item.getNodeSet());
            if (!workflowsuccess) {
                throw new WorkflowAction.WorkflowFailureException("Some steps in the workflow failed: " + failedList);
            }
        } catch (Exception e) {
            exception = e;
        }
        if (workflowsuccess) {
            return BaseExecutionResult.createSuccess(resultList);
        } else {
            return BaseExecutionResult.createFailure(exception);
        }

    }

}
