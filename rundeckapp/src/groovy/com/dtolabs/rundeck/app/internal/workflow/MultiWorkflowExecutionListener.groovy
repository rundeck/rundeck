package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionListenerOverride
import com.dtolabs.rundeck.core.execution.FailedNodesListener
import com.dtolabs.rundeck.core.execution.StatusResult
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult

/**
 * Calls listener methods on a list of sub listeners, and uses a reverse ordering for finish* methods.
 */
class MultiWorkflowExecutionListener implements WorkflowExecutionListener,ExecutionListener,ExecutionListenerOverride{
    List<WorkflowExecutionListener> listenerList;
    List<WorkflowExecutionListener> reversedListenerList;
    ExecutionListener delegate;

    private MultiWorkflowExecutionListener(ExecutionListener delegate,List<WorkflowExecutionListener> listenerList) {
        this.delegate = delegate;
        this.listenerList = listenerList;
        this.reversedListenerList=listenerList.reverse()
    }

    /**
     *
     * @param delegate
     * @param listenerList specified in outer-first ordering
     * @return
     */
    public static MultiWorkflowExecutionListener create(ExecutionListener delegate, List<WorkflowExecutionListener> listenerList) {
        return new MultiWorkflowExecutionListener(delegate,listenerList);
    }

    @Override
    void beginWorkflowExecution(StepExecutionContext executionContext, WorkflowExecutionItem item) {
        listenerList*.beginWorkflowExecution(executionContext,item)
    }

    @Override
    void finishWorkflowExecution(WorkflowExecutionResult result, StepExecutionContext executionContext, WorkflowExecutionItem item) {
        reversedListenerList*.finishWorkflowExecution(result,executionContext,item)
    }

    @Override
    void beginWorkflowItem(int step, StepExecutionItem item) {
        listenerList*.beginWorkflowItem(step, item)
    }

    @Override
    void beginWorkflowItemErrorHandler(int step, StepExecutionItem item) {
        listenerList*.beginWorkflowItemErrorHandler(step, item)
    }

    @Override
    void finishWorkflowItem(int step, StepExecutionItem item, StepExecutionResult result) {
        reversedListenerList*.finishWorkflowItem(step, item, result)
    }

    @Override
    void finishWorkflowItemErrorHandler(int step, StepExecutionItem item, StepExecutionResult result) {
        reversedListenerList*.finishWorkflowItemErrorHandler(step, item, result)
    }

    @Override
    void beginStepExecution(StepExecutor executor,StepExecutionContext context, StepExecutionItem item) {
        listenerList*.beginStepExecution(executor, context, item)
    }

    @Override
    void finishStepExecution(StepExecutor executor,StatusResult result, StepExecutionContext context, StepExecutionItem item) {
        reversedListenerList*.finishStepExecution(executor,result, context, item)
    }

    @Override
    void beginExecuteNodeStep(ExecutionContext context, NodeStepExecutionItem item, INodeEntry node) {
        listenerList*.beginExecuteNodeStep(context, item, node)
    }

    @Override
    void finishExecuteNodeStep(NodeStepResult result, ExecutionContext context, StepExecutionItem item, INodeEntry node) {
        reversedListenerList*.finishExecuteNodeStep(result, context, item, node)
    }

    @Override
    boolean isTerse() {
        return delegate.isTerse()
    }

    @Override
    String getLogFormat() {
        return delegate.getLogFormat()
    }

    @Override
    void log(int level, String message) {
        delegate.log(level,message)
    }

    @Override
    void event(String eventType, String message, Map eventMeta) {
        delegate.event(eventType,message,eventMeta)
    }

    FailedNodesListener overriddenFailedNodesListener
    @Override
    FailedNodesListener getFailedNodesListener() {
        return overriddenFailedNodesListener?:delegate.getFailedNodesListener()
    }

    @Override
    void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {
        delegate.beginNodeExecution(context,command,node)
    }

    @Override
    void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command, INodeEntry node) {
        delegate.finishNodeExecution(result, context, command, node)
    }

    @Override
    void beginNodeDispatch(ExecutionContext context, StepExecutionItem item) {
        delegate.beginNodeDispatch(context,item)
    }

    @Override
    void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
        delegate.beginNodeDispatch(context, item)
    }

    @Override
    void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item) {
        delegate.finishNodeDispatch(result,context,item)
    }

    @Override
    void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {
        delegate.finishNodeDispatch(result,context,item)
    }

    @Override
    void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {
        delegate.beginFileCopyFileStream(context,input,node)
    }

    @Override
    void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {
        delegate.beginFileCopyFile(context,input,node)
    }

    @Override
    void beginFileCopyScriptContent(ExecutionContext context, String input, INodeEntry node) {
        delegate.beginFileCopyScriptContent(context, input, node)
    }

    @Override
    void finishFileCopy(String result, ExecutionContext context, INodeEntry node) {
        delegate.finishFileCopy(result, context, node)
    }

    @Override
    ExecutionListenerOverride createOverride() {
        return new MultiWorkflowExecutionListener(delegate,listenerList)
    }

    @Override
    void setFailedNodesListener(FailedNodesListener listener) {
        overriddenFailedNodesListener=listener
    }
}
