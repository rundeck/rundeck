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
* BaseWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:19:17 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.SelectorUtils;
import com.dtolabs.rundeck.core.dispatcher.BaseDataContext;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContext;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BaseWorkflowStrategy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public abstract class BaseWorkflowExecutor implements WorkflowExecutor {
    protected static final String DATA_CONTEXT_PREFIX = "data context: ";
    protected static final String OPTION_KEY = "option";
    protected static final String SECURE_OPTION_KEY = "secureOption";
    protected static final String SECURE_OPTION_VALUE = "****";

    private final Framework framework;

    public BaseWorkflowExecutor(final Framework framework) {
        this.framework = framework;
    }

    /**
     * @param status       success/failure
     * @param statusString status string
     * @param behavior     control behavior
     *
     * @return result with the given input
     */
    protected static WorkflowStatusResult workflowResult(boolean status, String statusString, ControlBehavior behavior) {
        return new BaseWorkflowStatusResult(status, statusString, behavior);
    }

    /**
     * Failure result
     */
    protected static final WorkflowStatusResult WorkflowResultFailed = new BaseWorkflowStatusResult(
            false,
            null,
            ControlBehavior.Continue
    );

    static boolean isInnerLoop(final WorkflowExecutionItem item) {
        return item.getWorkflow() instanceof StepFirstWrapper;
    }

    protected Framework getFramework() {
        return framework;
    }

    static class BaseWorkflowStatusResult implements WorkflowStatusResult {
        private boolean status;
        private String statusString;
        private ControlBehavior controlBehavior;

        public BaseWorkflowStatusResult(boolean status, String statusString, ControlBehavior controlBehavior) {
            this.status = status;
            this.statusString = statusString;
            this.controlBehavior = controlBehavior;
        }

        public BaseWorkflowStatusResult(WorkflowStatusResult result) {
            this.status = result.isSuccess();
            this.statusString = result.getStatusString();
            this.controlBehavior = result.getControlBehavior();
        }

        public boolean isSuccess() {
            return status;
        }

        public String getStatusString() {
            return statusString;
        }

        public ControlBehavior getControlBehavior() {
            return controlBehavior;
        }
    }

    protected static class BaseWorkflowExecutionResult extends BaseWorkflowStatusResult implements WorkflowExecutionResult {
        private final List<StepExecutionResult> results;
        private final Map<String, Collection<StepExecutionResult>> failures;
        private final Map<Integer, StepExecutionResult> stepFailures;
        private final Exception orig;

        public BaseWorkflowExecutionResult(
                List<StepExecutionResult> results,
                Map<String, Collection<StepExecutionResult>> failures,
                final Map<Integer, StepExecutionResult> stepFailures,
                Exception orig,
                final WorkflowStatusResult status
        )
        {
            super(status);
            this.results = results;
            this.failures = failures;
            this.stepFailures = stepFailures;
            this.orig = orig;
        }

        public List<StepExecutionResult> getResultSet() {
            return results;
        }

        public Map<String, Collection<StepExecutionResult>> getNodeFailures() {
            return failures;
        }


        public Exception getException() {
            return orig;
        }

        @Override
        public String toString() {
            return "[Workflow result: "
//                   + (null != getResultSet() && getResultSet().size() > 0 ? "results: " + getResultSet() : "")
                   +
                   (null != getStepFailures() && getStepFailures().size() > 0
                    ? ", step failures: " + getStepFailures()
                    : "")
                   +
                   (null != getNodeFailures() && getNodeFailures().size() > 0 ? ", Node failures: "
                                                                                + getNodeFailures() : "")
                   +
                   (null != getException() ? ", exception: " + getException() : "")
                   + (null != getControlBehavior() ? ", flow control: " + getControlBehavior() : "")
                   +
                   (null != getStatusString()
                    ? ", status: " + getStatusString()
                    : ", status: " + (isSuccess() ? "succeeded" : "failed"))
                   +
                   "]";
        }

        public Map<Integer, StepExecutionResult> getStepFailures() {
            return stepFailures;
        }
    }

    public final WorkflowExecutionResult executeWorkflow(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {

        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        if (null != wlistener && !BaseWorkflowExecutor.isInnerLoop(item)) {
            wlistener.beginWorkflowExecution(executionContext, item);
        }
        WorkflowExecutionResult result = null;
        try {
            result = executeWorkflowImpl(executionContext, item);
        } finally {
            if (null != wlistener && !BaseWorkflowExecutor.isInnerLoop(item)) {
                wlistener.finishWorkflowExecution(result, executionContext, item);
            }
        }
        return result;
    }

    protected WorkflowExecutionListener getWorkflowListener(final ExecutionContext executionContext) {
        WorkflowExecutionListener wlistener = null;
        final ExecutionListener elistener = executionContext.getExecutionListener();
        if (null != elistener && elistener instanceof WorkflowExecutionListener) {
            wlistener = (WorkflowExecutionListener) elistener;
        }
        return wlistener;
    }

    public abstract WorkflowExecutionResult executeWorkflowImpl(
            StepExecutionContext executionContext,
            WorkflowExecutionItem item
    );

    /**
     * Execute a workflow item, returns true if the item succeeds.  This method will throw an exception if the workflow
     * item fails and the Workflow is has keepgoing==false.
     *
     * @param executionContext context
     * @param failedMap        List to add any messages if the item fails
     * @param c                index of the WF item
     * @param cmd              WF item descriptor
     *
     * @return true if the execution succeeds, false otherwise
     */
    protected StepExecutionResult executeWFItem(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final int c,
            final StepExecutionItem cmd
    )
    {

        if (null != executionContext.getExecutionListener()) {
            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL,
                    c + ": Workflow step executing: " + cmd
            );
        }
        StepExecutionResult result;
        try {
            result = framework.getExecutionService().executeStep(
                    ExecutionContextImpl.builder(executionContext).stepNumber(c).build(),
                    cmd
            );
            if (!result.isSuccess()) {
                failedMap.put(c, result);
            }
        } catch (StepException e) {
            result = StepExecutionResultImpl.wrapStepException(e);
            failedMap.put(c, result);
        }
        if (null != executionContext.getExecutionListener()) {
            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL,
                    c + ": Workflow step finished, result: " + result
            );
        }
        return result;
    }


    /**
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value
     *
     * @param executionContext  context
     * @param failedMap         failures
     * @param resultList        results
     * @param iWorkflowCmdItems list of steps
     * @param keepgoing         true to keepgoing on step failure
     *
     * @return true if successful
     *
     * @deprecated
     */
    protected WorkflowStatusResult executeWorkflowItemsForNodeSet(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final List<StepExecutionResult> resultList,
            final List<StepExecutionItem> iWorkflowCmdItems,
            final boolean keepgoing
    )
    {
        return executeWorkflowItemsForNodeSet(executionContext, failedMap, resultList, iWorkflowCmdItems, keepgoing,
                                              executionContext.getStepNumber()
        );
    }

    /**
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value
     *
     * @param executionContext  context
     * @param failedMap         failures
     * @param resultList        results
     * @param iWorkflowCmdItems list of steps
     * @param keepgoing         true to keepgoing on step failure
     * @param beginStepIndex    beginning step index
     *
     * @return true if successful
     *
     * @deprecated
     */
    protected WorkflowStatusResult executeWorkflowItemsForNodeSet(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final List<StepExecutionResult> resultList,
            final List<StepExecutionItem> iWorkflowCmdItems,
            final boolean keepgoing,
            final int beginStepIndex
    )
    {

        boolean workflowsuccess = true;
        String statusString = null;
        ControlBehavior controlBehavior = null;
        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        int c = beginStepIndex;
        for (final StepExecutionItem cmd : iWorkflowCmdItems) {
            StepResultCapture stepResultCapture = executeWorkflowStep(
                    executionContext,
                    failedMap,
                    resultList,
                    keepgoing,
                    wlistener,
                    c,
                    cmd
            );
            statusString = stepResultCapture.getStatusString();
            controlBehavior = stepResultCapture.getControlBehavior();
            if (!stepResultCapture.isSuccess()) {
                workflowsuccess = false;
            }
            if (stepResultCapture.getControlBehavior() == ControlBehavior.Halt ||
                !stepResultCapture.isSuccess() && !keepgoing) {
                break;
            }
            c++;
        }
        return workflowResult(
                workflowsuccess,
                statusString,
                null != controlBehavior ? controlBehavior : ControlBehavior.Continue
        );
    }

    /**
     * @param stepContext    the context
     * @param stepController a flow control object
     *
     * @return new context using the flow controller
     */
    private StepExecutionContext withFlowControl(
            final StepExecutionContext stepContext,
            final FlowController stepController
    )
    {
        return new ExecutionContextImpl.Builder(stepContext).flowControl(stepController).build();
    }

    private StepExecutionContext withDataOutput(
            final StepExecutionContext stepContext,
            final DataOutput dataOutput
    )
    {
        return new ExecutionContextImpl.Builder(stepContext).outputContext(dataOutput).build();
    }

    /**
     * @param builder
     * @param stepController a flow control object
     *
     * @return new context using the flow controller
     */
    private ExecutionContextImpl.Builder withOverride(
            final FlowController stepController,
            final OutputContext dataOutput,
            final ExecutionContextImpl.Builder builder
    )
    {
        return builder
                .flowControl(stepController)
                .outputContext(dataOutput);
    }

    /**
     * Add step result failure information to the data context
     *
     * @param stepResult         result
     *
     * @return new context
     */
    protected void addStepFailureContextData(
            StepExecutionResult stepResult,
            ExecutionContextImpl.Builder builder
    )
    {
        HashMap<String, String>
                resultData = new HashMap<>();
        if (null != stepResult.getFailureData()) {
            //convert values to string
            for (final Map.Entry<String, Object> entry : stepResult.getFailureData().entrySet()) {
                resultData.put(entry.getKey(), entry.getValue().toString());
            }
        }
        FailureReason reason = stepResult.getFailureReason();
        if (null == reason) {
            reason = StepFailureReason.Unknown;
        }
        resultData.put("reason", reason.toString());
        String message = stepResult.getFailureMessage();
        if (null == message) {
            message = "No message";
        }
        resultData.put("message", message);
        //add to data context

        builder.setContext("result", resultData);
    }

    /**
     * Add any node-specific step failure information to the node-specific data contexts
     *
     * @param dispatcherStepResult result
     *
     * @param builder
     * @return new context
     */
    protected void addNodeStepFailureContextData(
            final StepExecutionResult dispatcherStepResult,
            final ExecutionContextImpl.Builder builder
    )
    {
        final Map<String, ? extends NodeStepResult> resultMap;
        if (NodeDispatchStepExecutor.isWrappedDispatcherResult(dispatcherStepResult)) {
            DispatcherResult dispatcherResult = NodeDispatchStepExecutor.extractDispatcherResult(dispatcherStepResult);
            resultMap = dispatcherResult.getResults();
        } else if (NodeDispatchStepExecutor.isWrappedDispatcherException(dispatcherStepResult)) {
            DispatcherException exception
                    = NodeDispatchStepExecutor.extractDispatcherException(dispatcherStepResult);
            HashMap<String, NodeStepResult> nodeResults = new HashMap<>();
            NodeStepException nodeStepException = exception.getNodeStepException();
            if (null != nodeStepException && null != exception.getNode()) {
                NodeStepResult nodeExecutorResult = nodeStepResultFromNodeStepException(
                        exception.getNode(),
                        nodeStepException
                );
                nodeResults.put(
                        nodeStepException.getNodeName(),
                        nodeExecutorResult
                );
            }
            resultMap = nodeResults;
        } else {
            return;
        }
        if (null != resultMap) {
            for (final Map.Entry<String, ? extends NodeStepResult> dentry : resultMap.entrySet()) {
                String nodename = dentry.getKey();
                NodeStepResult stepResult = dentry.getValue();
                HashMap<String, String> resultData = new HashMap<>();
                if (null != stepResult.getFailureData()) {
                    //convert values to string
                    for (final Map.Entry<String, Object> entry : stepResult.getFailureData().entrySet()) {
                        resultData.put(entry.getKey(), entry.getValue().toString());
                    }
                }
                FailureReason reason = stepResult.getFailureReason();
                if (null == reason) {
                    reason = StepFailureReason.Unknown;
                }
                resultData.put("reason", reason.toString());
                String message = stepResult.getFailureMessage();
                if (null == message) {
                    message = "No message";
                }
                resultData.put("message", message);
                //add to data context
//                builder.mergeContext("result", resultData);
                builder.nodeDataContext(nodename, new BaseDataContext("result", resultData));
            }
        }
    }

    protected ExecutionContextImpl.Builder replaceFailedNodesListenerInContext(
            final ExecutionContextImpl.Builder builder,
            final FailedNodesListener captureFailedNodesListener,
            final ExecutionListener executionListener
    )
    {
        ExecutionListenerOverride listen = null;
        if (null != executionListener) {
            listen = executionListener.createOverride();
        }
        if (null != listen) {
            listen.setFailedNodesListener(captureFailedNodesListener);
        }

        return builder.executionListener(listen);
    }

    /**
     * Convert map of step execution results keyed by step number, to a collection of step execution results
     * keyed by node name
     *
     * @param failedMap failures
     *
     * @return converted
     */
    protected Map<String, Collection<StepExecutionResult>> convertFailures(
            final Map<Integer, StepExecutionResult> failedMap
    )
    {

        final Map<String, Collection<StepExecutionResult>> failures
                = new HashMap<>();
        for (final Map.Entry<Integer, StepExecutionResult> entry : failedMap.entrySet()) {
            final StepExecutionResult o = entry.getValue();

            if (NodeDispatchStepExecutor.isWrappedDispatcherResult(o)) {
                //indicates dispatcher returned node results
                final DispatcherResult dispatcherResult = NodeDispatchStepExecutor.extractDispatcherResult(o);

                for (final String s : dispatcherResult.getResults().keySet()) {
                    final NodeStepResult interpreterResult = dispatcherResult.getResults().get(s);
                    if (!failures.containsKey(s)) {
                        failures.put(s, new ArrayList<>());
                    }
                    failures.get(s).add(interpreterResult);
                }
            } else if (NodeDispatchStepExecutor.isWrappedDispatcherException(o)) {
                DispatcherException e = NodeDispatchStepExecutor.extractDispatcherException(o);
                final INodeEntry node = e.getNode();
                if (null != node) {
                    //dispatch failed for a specific node
                    final String key = node.getNodename();
                    if (!failures.containsKey(key)) {
                        failures.put(key, new ArrayList<>());
                    }
                    NodeStepException nodeStepException = e.getNodeStepException();
                    if (null != nodeStepException) {
                        failures.get(key).add(
                                nodeStepResultFromNodeStepException(node, nodeStepException)
                        );
                    }
                }
            }
        }
        return failures;
    }

    /**
     * @param node              node
     * @param nodeStepException exception
     *
     * @return a failure result with components from an exception
     */
    static protected NodeStepResult nodeStepResultFromNodeStepException(
            final INodeEntry node,
            final NodeStepException nodeStepException
    )
    {
        return new NodeStepResultImpl(
                nodeStepException.getCause(),
                nodeStepException.getFailureReason(),
                nodeStepException.getMessage(),
                node
        );
    }

    /**
     * Creates a copy of the given data context with the secure option values obfuscated.
     * This does not modify the original data context.
     *
     * "secureOption" map values will always be obfuscated. "option" entries that are also in "secureOption"
     * will have their values obfuscated. All other maps within the data context will be added
     * directly to the copy.
     *
     * @param dataContext data
     *
     * @return printable data
     */
    protected Map<String, Map<String, String>> createPrintableDataContext(Map<String, Map<String, String>>
                                                                                  dataContext) {
        return createPrintableDataContext(OPTION_KEY, SECURE_OPTION_KEY, SECURE_OPTION_VALUE, dataContext);
    }

    /**
     * Creates a copy of the given data context with the secure option values obfuscated.
     * This does not modify the original data context.
     *
     * "secureOption" map values will always be obfuscated. "option" entries that are also in "secureOption"
     * will have their values obfuscated. All other maps within the data context will be added
     * directly to the copy.
     *
     * @param optionKey         key
     * @param secureOptionKey   secure key
     * @param secureOptionValue secure value
     * @param dataContext       data
     *
     * @return printable data
     */
    protected Map<String, Map<String, String>> createPrintableDataContext(
            String optionKey,
            String secureOptionKey,
            String secureOptionValue,
            Map<String, Map<String, String>> dataContext
    )
    {
        Map<String, Map<String, String>> printableContext = new HashMap<>();
        if (dataContext != null) {
            printableContext.putAll(dataContext);
            Set<String> secureValues = new HashSet<>();
            if (dataContext.containsKey(secureOptionKey)) {
                Map<String, String> secureOptions = new HashMap<>();
                secureOptions.putAll(dataContext.get(secureOptionKey));
                secureValues.addAll(secureOptions.values());
                for (Map.Entry<String, String> entry : secureOptions.entrySet()) {
                    entry.setValue(secureOptionValue);
                }
                printableContext.put(secureOptionKey, secureOptions);
            }

            if (dataContext.containsKey(optionKey)) {
                Map<String, String> options = new HashMap<>();
                options.putAll(dataContext.get(optionKey));
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    if (secureValues.contains(entry.getValue())) {
                        entry.setValue(secureOptionValue);
                    }
                }
                printableContext.put(optionKey, options);
            }
        }
        return printableContext;
    }

    /**
     * Execute a step and handle flow control and error handler.
     *
     * @param executionContext context
     * @param failedMap        map for placing failure results
     * @param resultList       list of step results
     * @param keepgoing        true if the workflow should keepgoing on error
     * @param wlistener        listener
     * @param c                step number
     * @param cmd              step
     *
     * @return result and flow control
     */
    public StepResultCapture executeWorkflowStep(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> failedMap,
            final List<StepExecutionResult> resultList,
            final boolean keepgoing,
            final WorkflowExecutionListener wlistener,
            final int c,
            final StepExecutionItem cmd
    )
    {
        if (null != wlistener) {
            wlistener.beginWorkflowItem(c, cmd);
        }
        WorkflowStatusResultImpl result = new WorkflowStatusResultImpl(null, null, false);

        boolean hasHandler = cmd instanceof HasFailureHandler;

        //wrap node failed listener (if any) and capture status results
        NodeRecorder stepCaptureFailedNodesListener = new NodeRecorder();

        //create the new context for workflow execution
        ExecutionContextImpl.Builder wfRunContext = new ExecutionContextImpl.Builder(executionContext);

        replaceFailedNodesListenerInContext(
                wfRunContext,
                stepCaptureFailedNodesListener,
                executionContext.getExecutionListener()
        );

        final FlowController stepController = new FlowController();

        withOverride(
                stepController,
                outputContext,
                wfRunContext
        );
        final DataOutput outputContext = new DataOutput(ContextView.step(c));

        Map<String, NodeStepResult> nodeFailures;

        //execute the step item, and store the results
        StepExecutionResult stepResult = null;
        Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<>();
        stepResult = executeWFItem(wfRunContext.build(), stepFailedMap, c, cmd);
        result.setSuccess(stepResult.isSuccess());
        nodeFailures = stepCaptureFailedNodesListener.getFailedNodes();

        //collect node data results
        WFSharedContext combinedResultData = new WFSharedContext();
        combineResultData(c, outputContext, combinedResultData, stepResult);

        System.err.println("Finished step, result data context: " + combinedResultData);

        reportNodesMatched(executionContext, stepCaptureFailedNodesListener);

        if (stepController.isControlled()) {

            //TODO: halt execution without running the error-handler?
            result = WorkflowStatusResultImpl.with(stepController);
            executionContext.getExecutionListener().log(3, result.toString());
        }
        try {
            if (!result.isSuccess() && hasHandler) {
                final HasFailureHandler handles = (HasFailureHandler) cmd;
                final StepExecutionItem handler = handles.getFailureHandler();
                if (null != handler) {
                    //if there is a failure, and a failureHandler item, execute the failure handler
                    //set keepgoing=false, and store the results
                    //will throw an exception on failure because keepgoing=false

                    NodeRecorder handlerCaptureFailedNodesListener = new NodeRecorder();

                    ExecutionContextImpl.Builder wfHandlerContext = new ExecutionContextImpl.Builder(executionContext);
                    replaceFailedNodesListenerInContext(
                            wfHandlerContext,
                            handlerCaptureFailedNodesListener,
                            executionContext.getExecutionListener()
                    );

                    //if multi-node, determine set of nodes to run handler on: (failed node list only)
                    if (stepCaptureFailedNodesListener.getMatchedNodes().size() > 1) {
                        HashSet<String> failedNodeList = new HashSet<>(
                                stepCaptureFailedNodesListener.getFailedNodes().keySet());

                        wfHandlerContext.nodeSelector(SelectorUtils.nodeList(failedNodeList));

                    }

                    //add step failure data to data context
                    addStepFailureContextData(stepResult, wfHandlerContext);

                    //extract node-specific failure and set as node-context data
                    addNodeStepFailureContextData(stepResult, wfHandlerContext);

                    //add in data context results produced by the step
                    wfHandlerContext.mergeSharedContext(outputContext.getSharedContext());


                    if (null != wlistener) {
                        wlistener.beginWorkflowItemErrorHandler(c, cmd);
                    }

                    //allow flow control
                    final FlowController handlerController = new FlowController();
                    withOverride(
                            handlerController,
                            outputContext,
                            wfHandlerContext
                    );

                    Map<Integer, StepExecutionResult> handlerFailedMap = new HashMap<>();
                    StepExecutionResult handlerResult = executeWFItem(
                            wfHandlerContext.build(),
                            handlerFailedMap,
                            c,
                            handler
                    );
                    boolean handlerSuccess = handlerResult.isSuccess();

                    if (null != wlistener) {
                        wlistener.finishWorkflowItemErrorHandler(c, cmd, handlerResult);
                    }

                    //combine handler result data
                    combineResultData(c, outputContext, combinedResultData, handlerResult);

                    if (handlerController.isControlled() &&
                        handlerController.getControlBehavior() == ControlBehavior.Halt) {
                        //handler called Halt()
                        result = WorkflowStatusResultImpl.with(handlerController);
                        executionContext.getExecutionListener().log(3, result.toString());
                    } else {

                        //handle success conditions:
                        //1. if keepgoing=true, then status from handler overrides original step
                        //2. keepgoing=false, then status is the same as the original step, unless
                        //   the keepgoingOnSuccess is set to true and the handler succeeded
                        boolean useHandlerResults = keepgoing;
                        if (!keepgoing && handlerSuccess && handler instanceof HandlerExecutionItem) {
                            useHandlerResults = ((HandlerExecutionItem) handler).isKeepgoingOnSuccess();
                        }
                        if (useHandlerResults) {
                            result.setSuccess(handlerSuccess);
                            stepResult = handlerResult;
                            stepFailedMap = handlerFailedMap;
                            nodeFailures = handlerCaptureFailedNodesListener.getFailedNodes();
                        }
                    }
                }
            }
        } catch (RuntimeException t) {
//            t.printStackTrace(System.err);
            stepResult = new StepExecutionResultImpl(t, StepFailureReason.Unknown, t.getMessage());
            throw t;
        } finally {
            if (null != wlistener) {
                wlistener.finishWorkflowItem(c, cmd, stepResult);
            }
        }
        resultList.add(stepResult);
        failedMap.putAll(stepFailedMap);

        //report node failures based on results of step and handler run.
        if (null != executionContext.getExecutionListener() && null != executionContext.getExecutionListener()
                                                                                       .getFailedNodesListener()) {
            if (nodeFailures.size() > 0) {
                executionContext.getExecutionListener().getFailedNodesListener().nodesFailed(
                        nodeFailures);
            } else if (result.isSuccess()) {
                executionContext.getExecutionListener().getFailedNodesListener().nodesSucceeded();
            }

        }

        return new StepResultCapture(stepResult, result, combinedResultData);
    }

    public void combineResultData(
            final int c,
            final DataOutput outputContext,
            final WFSharedContext combinedResultData,
            final StepExecutionResult handlerResult
    )
    {
        combinedResultData.merge(outputContext.getSharedContext());
        if (NodeDispatchStepExecutor.isWrappedDispatcherResult(handlerResult)) {
            combineNodeResultData(c, handlerResult, combinedResultData);
        }
    }

    public void reportNodesMatched(
            final StepExecutionContext executionContext,
            final NodeRecorder stepCaptureFailedNodesListener
    )
    {
        if (null != executionContext.getExecutionListener() &&
            null != executionContext.getExecutionListener().getFailedNodesListener()) {
            executionContext.getExecutionListener().getFailedNodesListener().matchedNodes(
                    stepCaptureFailedNodesListener.getMatchedNodes());

        }
    }

    public void combineNodeResultData(
            final int c,
            final StepExecutionResult stepResult,
            final WFSharedContext combinedResultData
    )
    {
        HashMap<ContextView, DataContext> map = new HashMap<>();
        DispatcherResult dispatcherResult = NodeDispatchStepExecutor.extractDispatcherResult(stepResult);
        Map<String, ? extends NodeStepResult> results = dispatcherResult.getResults();
        System.out.println("Finished step, node results: " + results);
        WFSharedContext noderesults = new WFSharedContext();
        for (String node : results.keySet()) {
            NodeStepResult nodeStepResult = results.get(node);
            if (nodeStepResult instanceof HasSharedContext) {
                WFSharedContext dataContext = ((HasSharedContext) nodeStepResult).getSharedContext();
                noderesults.merge(dataContext);
                //XXX: also including node data in nodestep data??
//                map.put(ContextView.nodeStep(c, node), dataContext);
            }
        }
        combinedResultData.merge(noderesults);
    }

    public static class StepResultCapture implements WorkflowStatusResult {

        private StepExecutionResult stepResult;
        private WorkflowStatusResult statusResult;
        private WFSharedContext resultData;

        public StepResultCapture(
                final StepExecutionResult stepResult,
                final WorkflowStatusResult statusResult,
                final WFSharedContext resultData
        )
        {
            this.stepResult = stepResult;
            this.statusResult = statusResult;
            this.resultData = resultData;
        }

        public String getStatusString() {
            return statusResult.getStatusString();
        }

        @Override
        public boolean isSuccess() {
            return statusResult.isSuccess();
        }

        public ControlBehavior getControlBehavior() {
            return statusResult.getControlBehavior();
        }


        public StepExecutionResult getStepResult() {
            return stepResult;
        }

        public WFSharedContext getResultData() {
            return resultData;
        }

        @Override
        public String toString() {
            return "StepResultCapture{" +
                   "stepResult=" + stepResult +
                   ", stepSuccess=" + isSuccess() +
                   ", statusString='" + getStatusString() + '\'' +
                   ", controlBehavior=" + getStepResult() +
                   ", resultData=" + resultData +
                   '}';
        }
    }

    /**
     * Wrapper of IWorkflow that always returns STEP_FIRST for strategy
     */
    static class StepFirstWrapper implements IWorkflow {
        private IWorkflow workflow;

        StepFirstWrapper(IWorkflow workflow) {
            this.workflow = workflow;
        }

        public List<StepExecutionItem> getCommands() {
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

        @Override
        public Map<String, Object> getPluginConfig() {
            return workflow.getPluginConfig();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof StepFirstWrapper)) {
                return false;
            }

            StepFirstWrapper that = (StepFirstWrapper) o;

            if (workflow != null ? !workflow.equals(that.workflow) : that.workflow != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return workflow != null ? workflow.hashCode() : 0;
        }
    }
}
