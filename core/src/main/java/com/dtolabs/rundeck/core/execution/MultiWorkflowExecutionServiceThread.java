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

/*
 * WorkflowExecutionServiceThread.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 3/29/11 12:00 PM
 *
 */
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.logging.LoggingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementation of {@link WorkflowExecutionServiceThread} which implements the chained execution of
 * multiple workflows after the execution of a primary execution workflow.
 *
 */
public class MultiWorkflowExecutionServiceThread extends WorkflowExecutionServiceThread {

    /**
     * List of secondary workflows to execute.
     */
    List<WorkflowExecutionItem> cleanupWorkflowExecutionItems;

    public MultiWorkflowExecutionServiceThread(
            WorkflowExecutionService eservice,
            WorkflowExecutionItem primaryWorkflowItem,
            List<WorkflowExecutionItem> cleanupWorkflowExecutionItems,
            StepExecutionContext econtext,
            LoggingManager loggingManager) {

        super(eservice, primaryWorkflowItem, econtext, loggingManager);

        this.cleanupWorkflowExecutionItems = cleanupWorkflowExecutionItems;

    }


    @Override
    public void run() {

        if (null == this.weservice || null == this.weitem || null == context) {
            throw new IllegalStateException("project or execution detail not instantiated");
        }

        try {

            // Execute primary workflow
            StaticWorkflowExecutionResult executionResult = runWorkflow(weitem);

            setResult(executionResult.workflowResult);
            success = executionResult.success;
            thrown = executionResult.thrown;
            resultObject = executionResult.workflowResult;

            if (cleanupWorkflowExecutionItems != null && !cleanupWorkflowExecutionItems.isEmpty()) {
                context.getExecutionLogger().log(2, "==> Executing Cleanup Workflows");

                List<StaticWorkflowExecutionResult> cleanupResults = new ArrayList<>();

                for (WorkflowExecutionItem nextWorkflow : cleanupWorkflowExecutionItems) {

                    try {

                        // TODO apply filters and check if must execute this workflow

                        StaticWorkflowExecutionResult nextResult = runWorkflow(nextWorkflow);
                        context.getExecutionLogger().log(2, "Secondary Workflow Executed: " + nextResult.toString());
                        cleanupResults.add(nextResult);

                    } catch (Exception e) {

                        context.getExecutionLogger().log(0, "Error executing secondary workflow: " +
                                e.getMessage());
                        e.printStackTrace(System.err);
                        cleanupResults.add(new StaticWorkflowExecutionResult()
                                .setSuccess(false)
                                .setThrown(e));
                    }

                    context.getExecutionLogger().log(2,
                            "==> Executed " + cleanupResults.size() + " cleanup workflows.");


                    // TODO resolve Execution state transition.
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            thrown = e;
            return;
        }
    }
}
