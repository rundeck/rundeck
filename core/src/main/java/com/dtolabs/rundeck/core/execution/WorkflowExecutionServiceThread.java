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
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleComponentHandler;
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus;
import com.dtolabs.rundeck.core.logging.LoggingManager;
import com.dtolabs.rundeck.core.logging.PluginLoggingManager;
import com.dtolabs.rundeck.plugins.jobs.JobEventResultImpl;

/**
 * WorkflowExecutionServiceThread is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionServiceThread
        extends ServiceThreadBase<WorkflowExecutionResult>
        implements WorkflowExecutionServiceContext
{
    WorkflowExecutionService weservice;
    WorkflowExecutionItem weitem;
    private StepExecutionContext context;
    private WorkflowExecutionResult result;
    private LoggingManager loggingManager;
    private ExecutionLifecycleComponentHandler executionLifecycleComponentHandler;

    public WorkflowExecutionServiceThread(
            WorkflowExecutionService eservice,
            WorkflowExecutionItem eitem,
            StepExecutionContext econtext,
            LoggingManager loggingManager,
            ExecutionLifecycleComponentHandler executionLifecycleComponentHandler
    )
    {
        this.weservice = eservice;
        this.weitem = eitem;
        this.context = econtext;
        this.loggingManager = loggingManager;
        this.executionLifecycleComponentHandler = executionLifecycleComponentHandler;
    }

    public void run() {
        if (null == this.weservice || null == this.weitem || null == context) {
            throw new IllegalStateException("project or execution detail not instantiated");
        }
        if (loggingManager != null) {
            PluginLoggingManager pluginLogging = null;
            try {
                pluginLogging = loggingManager.createPluginLogging(context, null);
            } catch (Throwable e) {
                e.printStackTrace(System.err);
                thrown = e;
                return;
            }
            resultObject = pluginLogging.runWith(this::runWorkflow);
        } else {
            resultObject = runWorkflow();
        }
    }

    public WorkflowExecutionResult runWorkflow() {
        try {
            StepExecutionContext executionContext = context;
            if (executionLifecycleComponentHandler != null) {
                //TODO: check success and stop execution
                StepExecutionContext newExecutionContext =
                        executionLifecycleComponentHandler.beforeJobStarts(context, weitem)
                                                       .map(ExecutionLifecycleStatus::getExecutionContext)
                                                       .orElse(null);
                executionContext = newExecutionContext != null? newExecutionContext: executionContext;
            }
            final WorkflowExecutor executorForItem = weservice.getExecutorForItem(weitem);
            setResult(executorForItem.executeWorkflow(executionContext, weitem));
            success = getResult().isSuccess();
            if (null != getResult().getException()) {
                thrown = getResult().getException();
            }

            if (null != executionLifecycleComponentHandler) {
                try {
                    executionLifecycleComponentHandler.afterJobEnds(executionContext, new JobEventResultImpl(getResult(), isAborted()));
                } catch (ExecutionLifecycleComponentException err) {
                    err.printStackTrace(System.err);
                }
            }
            return getResult();
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            thrown = e;
            return null;
        }
    }

    @Override
    public StepExecutionContext getContext() {
        return context;
    }

    @Override
    public WorkflowExecutionResult getResult() {
        return result;
    }

    public void setResult(final WorkflowExecutionResult result) {
        this.result = result;
    }
}
