/*
 * Copyright 2014 Salesforce, Inc. (http://salesforce.com)
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
* ParallelWorkflowStrategy.java
* 
* User: Murat Ezbiderli <a href="mailto:mezbiderli@salesforce.com.com">mezbiderli@salesforce.com</a>
* Created: April 30, 2014
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.SelectorUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * ParallelWorkflowStrategy dispatches all steps in parallel to all nodes they are configured to run
 * and waits for their completion before terminating.
 *
 * Failure of 'any' sub-steps results in failure of the workflow regardless of keep-going
 *
 * TODO: parallel execution can be extended into and_parallel, or_parallel, xor_parallel, .. based on how failures are handled
 * TODO: Discuss and implement 'aborting' ongoing parallel steps if any of parallel steps fail beforehand and keep_going == false
 *
 * <br>
 * The WorkflowExecutionResult will contain as the resultSet a map of Node name to list of step execution results on that node
 *
 * @author Murat Ezbiderli <a href="mailto:mezbiderli@salesforce.com">mezbiderli@salesforce.com</a>
 * @version $Revision$
 */
public class ParallelWorkflowStrategy extends BaseWorkflowStrategy {

    protected static final String DATA_CONTEXT_PREFIX = "data context: ";

    public ParallelWorkflowStrategy(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final StepExecutionContext executionContext,
                                                       final WorkflowExecutionItem item) {
        WorkflowStatusResult workflowResult = WorkflowResultFailed;
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<Integer, StepExecutionResult>();
        final List<StepExecutionResult> stepResults = new ArrayList<StepExecutionResult>();
        try {
            // Log stuff
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                        "NodeSet: " + executionContext.getNodeSelector());
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Workflow: " + workflow);
            
            Map<String, Map<String, String>> printableContext =
                    createPrintableDataContext("option", "secureOption", "****", executionContext.getDataContext());
            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL, String.format("%s %s", DATA_CONTEXT_PREFIX, printableContext));

            final List<StepExecutionItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }

            workflowResult = executeWorkflowItemsInParallel(executionContext, stepFailures, stepResults,
                    iWorkflowCmdItems, workflow.isKeepgoing());
        } catch (RuntimeException e) {
            exception = e;
            e.printStackTrace();
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
        }
        final Exception orig = exception;
        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                orig,
                workflowResult
        );
    }

    /**
     *
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value, return true if
     * successful
     * @param executionContext context
     * @param failedMap failures
     * @param resultList results
     * @param iWorkflowCmdItems steps
     * @param keepgoing true to keepgoing if a step fails
     * @return true if successful
     */
    protected WorkflowStatusResult executeWorkflowItemsInParallel(final StepExecutionContext executionContext,
                                                     final Map<Integer, StepExecutionResult> failedMap,
                                                     final List<StepExecutionResult> resultList,
                                                     final List<StepExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing) {

        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);

        // Prepare runnables for each step
        int numCommands = iWorkflowCmdItems.size();
        ExecutorService es = Executors.newFixedThreadPool(numCommands);
        List<Map<Integer, StepExecutionResult>> stepFailedMaps = new ArrayList<Map<Integer, StepExecutionResult>>(numCommands);
        final Map<Integer, StepExecutionResult> stepExecutionResults =  new HashMap<Integer, StepExecutionResult>();
        List<Runnable> parallelSteps = new ArrayList<Runnable>();
        for (int i = 0; i < iWorkflowCmdItems.size(); i++) {
            final Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<Integer, StepExecutionResult>();
            final StepExecutionItem cmd = iWorkflowCmdItems.get(i);
            final int stepNum = executionContext.getStepNumber() + i;
            stepFailedMaps.add(stepFailedMap);

            Runnable cmdExecution = new Runnable() {
                @Override
                public void run() {
                    try {
                        StepExecutionResult result = executeWorkflowStep(
                                wlistener,
                                cmd,
                                executionContext,
                                stepFailedMap,
                                stepNum);
                        stepExecutionResults.put(stepNum, result);
                        failedMap.putAll(stepFailedMap);
                    } catch (Exception ex) {
                        String message = String.format("Exception while executing step [%i]: \t[%s]",
                                stepNum,
                                ex.getMessage());
                        executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
                        throw new RuntimeException(ex);
                    }
                }
            };
            parallelSteps.add(cmdExecution);
        }

        // Execute them
        for (Runnable step : parallelSteps) {
            es.execute(step);
        }

        // Wait for them to complete.
        // TODO: Write runnable consumer that polls workflow results and fails upon first failure if keepGoing == false
        // TODO: handle condition where step uses FlowControl to call Halt()
        es.shutdown();
        try {
            if (!es.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES)) {
              es.shutdownNow();
            }
        }catch (InterruptedException ioex) {
            executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow execution interrupted");
        }
        // Poll results, fail if there is any result is missing or not successful
        for (int i = 0; i < iWorkflowCmdItems.size(); i++) {
            int stepNum = i + executionContext.getStepNumber();
            if (null == stepExecutionResults.get(stepNum) ||
                !stepExecutionResults.get(stepNum).isSuccess()) {
                return WorkflowResultFailed;
            }
        }
        return workflowResult(true, null, ControlBehavior.Continue);
    }

    /**
     * Executes a step
     *
     * @param wlistener listener
     * @param cmd step
     * @param executionContext context
     * @param stepFailedMap failures
     * @param stepNum index
     * @return result
     */
    private StepExecutionResult executeWorkflowStep(
        WorkflowExecutionListener wlistener,
        StepExecutionItem cmd,
        StepExecutionContext executionContext,
        Map<Integer, StepExecutionResult> stepFailedMap,
        int stepNum
    ) {

        boolean stepSuccess=false;
        if (null != wlistener) {
            wlistener.beginWorkflowItem(stepNum, cmd);
        }

        //wrap node failed listener (if any) and capture status results
        NodeRecorder stepCaptureFailedNodesListener = new NodeRecorder();
        StepExecutionContext stepContext = replaceFailedNodesListenerInContext(executionContext,
                stepCaptureFailedNodesListener);
        Map<String,NodeStepResult> nodeFailures;

        //execute the step item, and store the results
        StepExecutionResult stepResult = null;
        //TODO: add FlowControl to context, handle HALT request
        stepResult = executeWFItem(stepContext, stepFailedMap, stepNum, cmd);
        stepSuccess = stepResult.isSuccess();
        nodeFailures = stepCaptureFailedNodesListener.getFailedNodes();

        if(null!=executionContext.getExecutionListener() && null!=executionContext.getExecutionListener().getFailedNodesListener()) {
            executionContext.getExecutionListener().getFailedNodesListener().matchedNodes(
                    stepCaptureFailedNodesListener.getMatchedNodes());
        }

        try {
            if(!stepSuccess && cmd instanceof HasFailureHandler) {
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

                    //add step failure data to data context
                    handlerExecContext = addStepFailureContextData(stepResult, handlerExecContext);

                    //extract node-specific failure and set as node-context data
                    handlerExecContext = addNodeStepFailureContextData(stepResult, handlerExecContext);

                    Map<Integer, StepExecutionResult> handlerFailedMap = new HashMap<Integer, StepExecutionResult>();
                    StepExecutionResult handlerResult = executeWFItem(handlerExecContext,
                            handlerFailedMap,
                            stepNum,
                            handler);
                    boolean handlerSuccess = handlerResult.isSuccess();

                    boolean useHandlerResults=true;
                    if(handlerSuccess && handler instanceof HandlerExecutionItem) {
                        useHandlerResults= ((HandlerExecutionItem) handler).isKeepgoingOnSuccess();
                    }
                    if(useHandlerResults){
                        stepSuccess = handlerSuccess;
                        stepResult=handlerResult;
                        stepFailedMap = handlerFailedMap;
                        nodeFailures = handlerCaptureFailedNodesListener.getFailedNodes();
                    }
                }
            }
        } finally {
            if (null != wlistener) {
                wlistener.finishWorkflowItem(stepNum, cmd, stepResult);
            }
        }

        //report node failures based on results of step and handler run.
        if (null != executionContext.getExecutionListener() && null != executionContext.getExecutionListener()
                .getFailedNodesListener()) {
            if(nodeFailures.size()>0){
                executionContext.getExecutionListener().getFailedNodesListener().nodesFailed(
                        nodeFailures);
            }else if(stepSuccess){
                executionContext.getExecutionListener().getFailedNodesListener().nodesSucceeded();
            }

        }
        return stepResult;
    }
}
