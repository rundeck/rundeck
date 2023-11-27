/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.app.internal.workflow.BaseWorkflowExecutionListener;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.ThreadBoundLogOutputStream;

public class LogFlusher extends BaseWorkflowExecutionListener {
    private ThreadBoundLogOutputStream logOut;

    public LogFlusher() {

    }

    public LogFlusher(final ThreadBoundLogOutputStream logOut) {
        assert logOut != null;
        this.logOut = logOut;
    }

    @Override
    public void beginStepExecution(
            final StepExecutor executor,
            final StepExecutionContext context,
            final StepExecutionItem item
    )
    {
        if(logOut!=null){
            logOut.installManager();
        }
    }

    @Override
    public void finishStepExecution(
            final StepExecutor executor,
            final StatusResult result,
            final StepExecutionContext context,
            final StepExecutionItem item
    )
    {
        if(logOut!=null){
            logOut.flushBuffers();
        }
    }

    @Override
    public void beginExecuteNodeStep(final ExecutionContext context, final NodeStepExecutionItem item, final INodeEntry node) {
        if(logOut!=null){
            logOut.installManager();
        }
    }

    @Override
    public void finishExecuteNodeStep(
            final NodeStepResult result,
            final ExecutionContext context,
            final StepExecutionItem item,
            final INodeEntry node
    )
    {
        if(logOut!=null){
            logOut.flushBuffers();
        }
    }
}
