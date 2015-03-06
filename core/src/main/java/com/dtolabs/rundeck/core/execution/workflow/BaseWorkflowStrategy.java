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
public abstract class BaseWorkflowStrategy implements WorkflowStrategy {
    final Framework framework;

    public BaseWorkflowStrategy(final Framework framework) {
        this.framework = framework;
    }

    /**
     * @param status success/failure
     * @param statusString status string
     * @param behavior control behavior
     * @return result with the given input
     */
    static WorkflowStatusResult workflowResult(boolean status, String statusString, ControlBehavior behavior) {
        return new BaseWorkflowStatusResult(status, statusString, behavior);
    }

    /**
     * Failure result
     */
    static final WorkflowStatusResult WorkflowResultFailed = new BaseWorkflowStatusResult(
            false,
            null,
            ControlBehavior.Continue
    );

    static class BaseWorkflowStatusResult implements WorkflowStatusResult {
        private boolean status;
        private String statusString;
        private ControlBehavior controlBehavior;

        public BaseWorkflowStatusResult(boolean status, String statusString,ControlBehavior controlBehavior) {
            this.status = status;
            this.statusString = statusString;
            this.controlBehavior=controlBehavior;
        }

        public BaseWorkflowStatusResult(WorkflowStatusResult result) {
            this.status = result.isSuccess();
            this.statusString = result.getStatusString();
            this.controlBehavior=result.getControlBehavior();
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
    static class BaseWorkflowExecutionResult extends BaseWorkflowStatusResult implements WorkflowExecutionResult {
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

    public final WorkflowExecutionResult executeWorkflow(final StepExecutionContext executionContext,
                                                         final WorkflowExecutionItem item) {

        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        if (null != wlistener && !StepFirstWorkflowStrategy.isInnerLoop(item)) {
            wlistener.beginWorkflowExecution(executionContext, item);
        }
        WorkflowExecutionResult result = null;
        try {
            result = executeWorkflowImpl(executionContext, item);
        } finally {
            if (null != wlistener && !StepFirstWorkflowStrategy.isInnerLoop(item)) {
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

    public abstract WorkflowExecutionResult executeWorkflowImpl(StepExecutionContext executionContext,
                                                                WorkflowExecutionItem item);

    /**
     * Execute a workflow item, returns true if the item succeeds.  This method will throw an exception if the workflow
     * item fails and the Workflow is has keepgoing==false.
     *
     * @param executionContext  context
     * @param failedMap  List to add any messages if the item fails
     * @param c          index of the WF item
     * @param cmd        WF item descriptor
     * @return true if the execution succeeds, false otherwise
     *
     */
    protected StepExecutionResult executeWFItem(final StepExecutionContext executionContext,
                                                final Map<Integer, StepExecutionResult> failedMap,
                                                final int c,
                                                final StepExecutionItem cmd) {

        if (null != executionContext.getExecutionListener()) {
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                        c + ": Workflow step executing: " + cmd);
        }
        StepExecutionResult result;
        try {
            result = framework.getExecutionService().executeStep(
                ExecutionContextImpl.builder(executionContext).stepNumber(c).build(),
                cmd);
            if (!result.isSuccess()) {
                failedMap.put(c, result);
            }
        } catch (StepException e) {
            result = StepExecutionResultImpl.wrapStepException(e);
            failedMap.put(c, result);
        }
        if (null != executionContext.getExecutionListener()) {
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                        c + ": Workflow step finished, result: " + result);
        }
        return result;
    }


    /**
     *
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value
     * @param executionContext context
     * @param failedMap failures
     * @param resultList results
     * @param iWorkflowCmdItems list of steps
     * @param keepgoing true to keepgoing on step failure
     * @return true if successful
     */
    protected WorkflowStatusResult executeWorkflowItemsForNodeSet(final StepExecutionContext executionContext,
                                                     final Map<Integer, StepExecutionResult> failedMap,
                                                     final List<StepExecutionResult> resultList,
                                                     final List<StepExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing)  {
        return executeWorkflowItemsForNodeSet(executionContext, failedMap, resultList, iWorkflowCmdItems, keepgoing,
                                              executionContext.getStepNumber());
    }
    /**
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value
     * @return true if successful
     * @param executionContext context
     * @param failedMap failures
     * @param resultList results
     * @param iWorkflowCmdItems list of steps
     * @param keepgoing true to keepgoing on step failure
     * @param beginStepIndex beginning step index
     */
    protected WorkflowStatusResult executeWorkflowItemsForNodeSet(final StepExecutionContext executionContext,
                                                     final Map<Integer, StepExecutionResult> failedMap,
                                                     final List<StepExecutionResult> resultList,
                                                     final List<StepExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing,
                                                     final int beginStepIndex) {

        boolean workflowsuccess = true;
        String statusString=null;
        ControlBehavior controlBehavior = null;
        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        int c = beginStepIndex;
        for (final StepExecutionItem cmd : iWorkflowCmdItems) {
            if (null != wlistener) {
                wlistener.beginWorkflowItem(c, cmd);
            }
            boolean hasHandler= cmd instanceof HasFailureHandler;

            //wrap node failed listener (if any) and capture status results
            NodeRecorder stepCaptureFailedNodesListener = new NodeRecorder();
            StepExecutionContext stepContext = replaceFailedNodesListenerInContext(executionContext,
                stepCaptureFailedNodesListener);

            final FlowController stepController=new FlowController();

            StepExecutionContext controllableContext = withFlowControl(stepContext, stepController);

            Map<String,NodeStepResult> nodeFailures;

            //execute the step item, and store the results
            StepExecutionResult stepResult=null;
            Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<Integer, StepExecutionResult>();
            stepResult = executeWFItem(controllableContext, stepFailedMap, c, cmd);
            boolean stepSuccess = stepResult.isSuccess();
            nodeFailures = stepCaptureFailedNodesListener.getFailedNodes();

            if(null!=executionContext.getExecutionListener() && null!=executionContext.getExecutionListener().getFailedNodesListener()) {
                executionContext.getExecutionListener().getFailedNodesListener().matchedNodes(
                    stepCaptureFailedNodesListener.getMatchedNodes());

            }
            if (stepController.isControlled()) {

                //TODO: halt execution without running the error-handler?
                stepSuccess = stepController.isSuccess();
                statusString=stepController.getStatusString();
                controlBehavior=stepController.getControlBehavior();
                executionContext.getExecutionListener().log(
                        3,
                        controlBehavior +
                        " requested" +
                        (controlBehavior == ControlBehavior.Halt ?
                         " with result: " +
                         (null != statusString ? statusString : stepSuccess) : "")
                );
            }
            try {
                if(!stepSuccess && hasHandler) {
                    final HasFailureHandler handles = (HasFailureHandler) cmd;
                    final StepExecutionItem handler = handles.getFailureHandler();
                    if (null != handler) {
                        //if there is a failure, and a failureHandler item, execute the failure handler
                        //set keepgoing=false, and store the results
                        //will throw an exception on failure because keepgoing=false

                        NodeRecorder handlerCaptureFailedNodesListener = new NodeRecorder();
                        StepExecutionContext handlerExecContext = replaceFailedNodesListenerInContext(executionContext,
                            handlerCaptureFailedNodesListener);

                        //if multi-node, determine set of nodes to run handler on: (failed node list only)
                        if(stepCaptureFailedNodesListener.getMatchedNodes().size()>1) {
                            HashSet<String> failedNodeList = new HashSet<String>(
                                stepCaptureFailedNodesListener.getFailedNodes().keySet());

                            handlerExecContext = new ExecutionContextImpl.Builder(handlerExecContext).nodeSelector(
                                SelectorUtils.nodeList(failedNodeList)).build();

                        }

                        if(null!=stepResult) {
                            //add step failure data to data context
                            handlerExecContext = addStepFailureContextData(stepResult, handlerExecContext);

                            //extract node-specific failure and set as node-context data
                            handlerExecContext = addNodeStepFailureContextData(stepResult, handlerExecContext);
                        }
                        if (null != wlistener) {
                            wlistener.beginWorkflowItemErrorHandler(c, cmd);
                        }
                        final FlowController handlerController=new FlowController();
                        StepExecutionContext handlerControlContext = withFlowControl(
                                handlerExecContext,
                                handlerController
                        );

                        Map<Integer, StepExecutionResult> handlerFailedMap = new HashMap<Integer, StepExecutionResult>();
                        StepExecutionResult handlerResult = executeWFItem(handlerControlContext,
                                                                          handlerFailedMap,
                                                                          c,
                                                                          handler);
                        boolean handlerSuccess = handlerResult.isSuccess();

                        if (null != wlistener) {
                            wlistener.finishWorkflowItemErrorHandler(c, cmd, handlerResult);
                        }

                        if (handlerController.isControlled() &&
                            handlerController.getControlBehavior() == ControlBehavior.Halt) {
                            //handler called Halt()
                            stepSuccess = handlerController.isSuccess();
                            statusString = handlerController.getStatusString();
                            controlBehavior = handlerController.getControlBehavior();
                            executionContext.getExecutionListener().log(3,
                                                                        controlBehavior +
                                                                        " requested with result: " +
                                                                        (null != statusString ? statusString : stepSuccess)
                            );
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
                                stepSuccess = handlerSuccess;
                                stepResult = handlerResult;
                                stepFailedMap = handlerFailedMap;
                                nodeFailures = handlerCaptureFailedNodesListener.getFailedNodes();
                            }
                        }
                    }
                }
            }catch (RuntimeException t) {
                stepResult = new StepExecutionResultImpl(t, StepFailureReason.Unknown, t.getMessage());
                throw t;
            } finally {
                if (null != wlistener) {
                    wlistener.finishWorkflowItem(c, cmd, stepResult);
                }
            }
            resultList.add(stepResult);
            failedMap.putAll(stepFailedMap);
            if(!stepSuccess){
                workflowsuccess = false;
            }

            //report node failures based on results of step and handler run.
            if (null != executionContext.getExecutionListener() && null != executionContext.getExecutionListener()
                .getFailedNodesListener()) {
                if(nodeFailures.size()>0){
                    executionContext.getExecutionListener().getFailedNodesListener().nodesFailed(
                    nodeFailures);
                }else if(workflowsuccess){
                    executionContext.getExecutionListener().getFailedNodesListener().nodesSucceeded();
                }

            }

            if (controlBehavior == ControlBehavior.Halt || !stepSuccess && !keepgoing ) {
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
     * @param stepContext the context
     * @param stepController a flow control object
     * @return new context using the flow controller
     */
    private StepExecutionContext withFlowControl(
            final StepExecutionContext stepContext,
            final FlowController stepController
    )
    {
        return new ExecutionContextImpl.Builder(stepContext).flowControl(stepController).build();
    }

    /**
     * Add step result failure information to the data context
     * @param stepResult result
     * @param handlerExecContext context
     * @return new context
     */
    protected StepExecutionContext addStepFailureContextData(StepExecutionResult stepResult,
                                                           StepExecutionContext handlerExecContext) {
        HashMap<String, String>
        resultData = new HashMap<String, String>();
        if (null != stepResult.getFailureData()) {
            //convert values to string
            for (final Map.Entry<String, Object> entry : stepResult.getFailureData().entrySet()) {
                resultData.put(entry.getKey(), entry.getValue().toString());
            }
        }
        FailureReason reason = stepResult.getFailureReason();
        if(null== reason){
            reason= StepFailureReason.Unknown;
        }
        resultData.put("reason", reason.toString());
        String message = stepResult.getFailureMessage();
        if(null==message) {
            message = "No message";
        }
        resultData.put("message", message);
        //add to data context

        handlerExecContext = ExecutionContextImpl.builder(handlerExecContext).
            setContext("result", resultData)
            .build();
        return handlerExecContext;
    }

    /**
     * Add any node-specific step failure information to the node-specific data contexts
     * @param dispatcherStepResult result
     * @param handlerExecContext context
     * @return new context
     */
    protected StepExecutionContext addNodeStepFailureContextData(StepExecutionResult dispatcherStepResult,
                                                               StepExecutionContext handlerExecContext) {
        final Map<String, ? extends NodeStepResult> resultMap;
        if (NodeDispatchStepExecutor.isWrappedDispatcherResult(dispatcherStepResult)) {
            DispatcherResult dispatcherResult = NodeDispatchStepExecutor.extractDispatcherResult(dispatcherStepResult);
            resultMap = dispatcherResult.getResults();
        } else if (NodeDispatchStepExecutor.isWrappedDispatcherException(dispatcherStepResult)) {
            DispatcherException exception
                    = NodeDispatchStepExecutor.extractDispatcherException(dispatcherStepResult);
            HashMap<String, NodeStepResult> stringNodeStepResultHashMap = new HashMap<String,
                    NodeStepResult>();
            resultMap = stringNodeStepResultHashMap;
            NodeStepException nodeStepException = exception.getNodeStepException();
            if (null != nodeStepException && null != exception.getNode()) {
                NodeStepResult nodeExecutorResult = nodeStepResultFromNodeStepException(
                        exception.getNode(),
                        nodeStepException
                );
                stringNodeStepResultHashMap.put(
                        nodeStepException.getNodeName(),
                        nodeExecutorResult
                );
            }
        } else {
            return handlerExecContext;
        }
        ExecutionContextImpl.Builder builder = ExecutionContextImpl.builder(handlerExecContext);
        if(null!= resultMap){
            for (final Map.Entry<String, ? extends NodeStepResult> dentry : resultMap.entrySet()) {
                String nodename = dentry.getKey();
                NodeStepResult stepResult = dentry.getValue();
                HashMap<String, String> resultData = new HashMap<String, String>();
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
                HashMap<String, Map<String, String>> ndata = new HashMap<String, Map<String, String>>();
                ndata.put("result", resultData);
                builder.nodeDataContext(nodename, ndata);
            }
        }
        return builder.build();
    }

    protected StepExecutionContext replaceFailedNodesListenerInContext(StepExecutionContext executionContext,
                                                                 FailedNodesListener captureFailedNodesListener) {
        ExecutionListenerOverride listen=null;
        if(null!= executionContext.getExecutionListener()) {
            listen = executionContext.getExecutionListener().createOverride();
        }
        if(null!=listen){
            listen.setFailedNodesListener(captureFailedNodesListener);
        }

        return new ExecutionContextImpl.Builder(executionContext).executionListener(listen).build();
    }

    /**
     * Convert map of step execution results keyed by step number, to a collection of step execution results
     * keyed by node name
     * @param failedMap  failures
     * @return converted
     */
    protected Map<String, Collection<StepExecutionResult>> convertFailures(
        final Map<Integer, StepExecutionResult> failedMap) {

        final Map<String, Collection<StepExecutionResult>> failures
            = new HashMap<String, Collection<StepExecutionResult>>();
        for (final Map.Entry<Integer, StepExecutionResult> entry : failedMap.entrySet()) {
            final StepExecutionResult o = entry.getValue();

            if (NodeDispatchStepExecutor.isWrappedDispatcherResult(o)) {
                //indicates dispatcher returned node results
                final DispatcherResult dispatcherResult = NodeDispatchStepExecutor.extractDispatcherResult(o);

                for (final String s : dispatcherResult.getResults().keySet()) {
                    final NodeStepResult interpreterResult = dispatcherResult.getResults().get(s);
                    if (!failures.containsKey(s)) {
                        failures.put(s, new ArrayList<StepExecutionResult>());
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
                        failures.put(key, new ArrayList<StepExecutionResult>());
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
     * @return a failure result with components from an exception
     * @param node node
     * @param nodeStepException exception
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
     *
     * Creates a copy of the given data context with the secure option values obfuscated.
     * This does not modify the original data context.
     *
     * "secureOption" map values will always be obfuscated. "option" entries that are also in "secureOption"
     * will have their values obfuscated. All other maps within the data context will be added
     * directly to the copy.
     * @param optionKey key
     * @param secureOptionKey secure key
     * @param secureOptionValue secure value
     * @param dataContext data
     * @return printable data
     */
    protected Map<String, Map<String, String>> createPrintableDataContext(String optionKey,
                                                                          String secureOptionKey,
                                                                          String secureOptionValue,
                                                                          Map<String, Map<String, String>> dataContext) {
        Map<String, Map<String, String>> printableContext = new HashMap<String, Map<String, String>>();
        if (dataContext != null) {
            printableContext.putAll(dataContext);
            Set<String> secureValues = new HashSet<String>();
            if (dataContext.containsKey(secureOptionKey)) {
                Map<String, String> secureOptions = new HashMap<String, String>();
                secureOptions.putAll(dataContext.get(secureOptionKey));
                secureValues.addAll(secureOptions.values());
                for (Map.Entry<String, String> entry : secureOptions.entrySet()) {
                    entry.setValue(secureOptionValue);
                }
                printableContext.put(secureOptionKey, secureOptions);
            }

            if (dataContext.containsKey(optionKey)) {
                Map<String, String> options = new HashMap<String, String>();
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
}
