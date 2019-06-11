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

import java.util.Optional;

/**
 * WorkflowExecutionServiceThread implements main thread control for the execution of a single Workflow.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionServiceThread extends ServiceThreadBase<WorkflowExecutionResult> {
    protected final WorkflowExecutionService weservice;
    protected final WorkflowExecutionItem weitem;
    protected final StepExecutionContext context;
    protected final LoggingManager loggingManager;
    protected WorkflowExecutionResult result;

    public WorkflowExecutionServiceThread(
            WorkflowExecutionService eservice,
            WorkflowExecutionItem eitem,
            StepExecutionContext econtext,
            LoggingManager loggingManager
    ) {
        this.weservice = eservice;
        this.weitem = eitem;
        this.context = econtext;
        this.loggingManager = loggingManager;
    }

    public StepExecutionContext getContext() {
        return context;
    }

    public WorkflowExecutionResult getResult() {
        return result;
    }

    public void setResult(final WorkflowExecutionResult result) {
        this.result = result;
    }


    public void run() {
        if (null == this.weservice || null == this.weitem || null == context) {
            throw new IllegalStateException("project or execution detail not instantiated");
        }

        try {

            StaticWorkflowExecutionResult executionResult = runWorkflow(weitem);

            setResult(executionResult.workflowResult);
            success = executionResult.success;
            thrown = executionResult.thrown;
            resultObject = executionResult.workflowResult;

        } catch (Throwable e) {
            e.printStackTrace(System.err);
            thrown = e;
            return;
        }

    }


    /**
     * Run the specified {@link WorkflowExecutionItem} using the current context and service.
     * @param workflowExecutionItem
     * @return
     */
    protected StaticWorkflowExecutionResult runWorkflow(final WorkflowExecutionItem workflowExecutionItem) {

        StaticWorkflowExecutionResult executionResult;
        if (loggingManager != null) {
            executionResult = loggingManager.createPluginLogging(context, null)
                    .runWith(() -> runWorkflowStatic(weservice, workflowExecutionItem, context));
        } else {
            executionResult = runWorkflowStatic(weservice, workflowExecutionItem, context);
        }
        return executionResult;
    }

    /**
     * Runs the specified WorkflowExecutionItem using the service and context provided as parameters.
     * @param weservice WorkflowExecutionService to resolve the WorkflowExecutor
     * @param weitem {@link WorkflowExecutionItem} to execute.
     * @param context Execution context.
     * @return Object with the execution results.
     */
    protected static StaticWorkflowExecutionResult runWorkflowStatic(
            WorkflowExecutionService weservice,
            WorkflowExecutionItem weitem,
            StepExecutionContext context
    ) {

        try {
            final WorkflowExecutor executorForItem = weservice.getExecutorForItem(weitem);
            WorkflowExecutionResult wresult = executorForItem.executeWorkflow(context, weitem);

            return new StaticWorkflowExecutionResult()
                    .setWorkflowResult(wresult)
                    .setSuccess(wresult.isSuccess())
                    .setThrown(Optional.ofNullable(wresult.getException())
                            .orElse(null));

        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return new StaticWorkflowExecutionResult()
                    .setThrown(e);
        }
    }


    /**
     * Workflow Execution results container.
     */
    protected static class StaticWorkflowExecutionResult {
        WorkflowExecutionResult workflowResult;
        boolean success;
        Throwable thrown;

        public StaticWorkflowExecutionResult setWorkflowResult(WorkflowExecutionResult workflowResult) {
            this.workflowResult = workflowResult;
            return this;
        }

        public StaticWorkflowExecutionResult setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public StaticWorkflowExecutionResult setThrown(Throwable thrown) {
            this.thrown = thrown;
            return this;
        }

        @Override
        public String toString() {
            return "StaticWorkflowExecutionResult{" +
                    "workflowResult=" + workflowResult +
                    ", success=" + success +
                    ", thrown=" + thrown +
                    '}';
        }
    }

}
