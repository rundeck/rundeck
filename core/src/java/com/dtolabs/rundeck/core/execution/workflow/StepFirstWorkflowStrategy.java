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
* WFFirstWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:16:49 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;

import java.util.*;

/**
 * StepFirstWorkflowStrategy iterates over the workflow steps and dispatches each one to all nodes matching the filter.
 * This strategy is used either for an entire workflow and set of multiple nodes OR by the NodeFirstWorkflowStrategy as
 * the inner loop over a single node.
 * <p/>
 * The WorkflowExecutionResult will contain as the resultSet a map of Node name to
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class StepFirstWorkflowStrategy extends BaseWorkflowStrategy {

    public StepFirstWorkflowStrategy(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final ExecutionContext executionContext,
                                                   final WorkflowExecutionItem item) {
        boolean workflowsuccess = false;
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        final Map<Integer, Object> failedList = new HashMap<Integer, Object>();
        final List<DispatcherResult> resultList = new ArrayList<DispatcherResult>();
        try {
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                "NodeSet: " + executionContext.getNodeSet());
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Workflow: " + workflow);
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "data context: " + executionContext
                .getDataContext());

            final List<ExecutionItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            workflowsuccess = executeWorkflowItemsForNodeSet(executionContext, failedList, resultList,
                iWorkflowCmdItems, workflow.isKeepgoing());
            if (!workflowsuccess) {
                throw new WorkflowFailureException("Some steps in the workflow failed: " + failedList);
            }
        } catch (Exception e) {
            exception = e;
        }
        final boolean success = workflowsuccess;
        final Exception orig = exception;
        final HashMap<String, List<StatusResult>> results = convertResults(resultList);
        final Map<String, Collection<String>> failures = convertFailures(failedList);
        return new WorkflowExecutionResult(results, failures, success, orig);

    }

}
