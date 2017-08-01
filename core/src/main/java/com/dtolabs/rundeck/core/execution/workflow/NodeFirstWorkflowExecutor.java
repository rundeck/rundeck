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
import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.HasSourceResult;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResultImpl;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * NodeFirstWorkflowStrategy Iterates over the matched nodes first, so that each node executes the full workflow
 * sequentially
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 * @deprecated
 */
public class NodeFirstWorkflowExecutor extends BaseWorkflowExecutor {
    static final Logger logger = Logger.getLogger(NodeFirstWorkflowExecutor.class.getName());

    public NodeFirstWorkflowExecutor(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final StepExecutionContext executionContext,
                                                       final WorkflowExecutionItem item) {
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        WorkflowStatusResult wfresult= null;

        final ArrayList<StepExecutionResult> results = new ArrayList<>();
        final Map<String, Collection<StepExecutionResult>> failures
            = new HashMap<>();
        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<>();

        boolean workflowsuccess = true;
        String statusString=null;
        ControlBehavior controlBehavior = null;

        final WFSharedContext wfCurrentContext = WFSharedContext.withBase(executionContext.getSharedDataContext());
        try {
            final NodesSelector nodeSelector = executionContext.getNodeSelector();

            if (workflow.getCommands().size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            validateNodeSet(executionContext, nodeSelector);
            logger.debug("Begin loop");

            //split workflow around non-node dispatched items, and loop
            //each dispatched sequence should be wrapped in a separate dispatch
            //and each non-dispatched step performed separately
            final List<IWorkflow> sections = splitWorkflowDispatchedSections(workflow);
            int stepCount = 1;
            if (sections.size() > 1) {
                logger.debug("Split workflow into " + sections.size() + " sections");
            }
            assert sections.size() >= 1;
            if (sections.size() < 1) {
                throw new IllegalStateException();
            }
            for (final IWorkflow flowsection : sections) {
                WorkflowStatusResult sectionSuccess;
                WorkflowDataResult sectionData;

                StepExecutor stepExecutor = getFramework().getStepExecutionService()
                                                          .getExecutorForItem(flowsection.getCommands().get(0));

                if (stepExecutor.isNodeDispatchStep(flowsection.getCommands().get(0))) {
                    WorkflowStatusDataResult workflowStatusDataResult = executeWFSectionNodeDispatch(
                            executionContext,
                            stepCount,
                            results,
                            failures,
                            stepFailures,
                            flowsection,
                            wfCurrentContext
                    );
                    sectionSuccess = workflowStatusDataResult;
                    sectionData = workflowStatusDataResult;
                } else {
                    //run the Workflow Step section as a normal step-first strategy workflow
                    //execute each item sequentially
                    WorkflowExecutionItem innerLoopItem = createInnerLoopItem(flowsection);
                    WorkflowExecutor executorForItem =
                            getFramework().getWorkflowExecutionService().getExecutorForItem(innerLoopItem);

                    WFSharedContext currentData = new WFSharedContext(wfCurrentContext);

                    StepExecutionContext newContext =
                            ExecutionContextImpl.builder(executionContext)
                                                .sharedDataContext(currentData)
                                                .stepNumber(stepCount)
                                                .build();
                    WorkflowExecutionResult workflowExecutionResult = executorForItem.executeWorkflow(
                            newContext,
                            innerLoopItem
                    );

                    results.addAll(workflowExecutionResult.getResultSet());
                    mergeFailure(failures, workflowExecutionResult.getNodeFailures());
                    stepFailures.putAll(workflowExecutionResult.getStepFailures());
                    sectionSuccess = workflowExecutionResult;
                    sectionData = workflowExecutionResult;
                    //
//                    sectionSuccess = executeWFSection(executionContext,
//                                                      results,
//                                                      failures,
//                                                      stepFailures,
//                                                      stepCount,
//                                                      flowsection.getCommands(),
//                                                      flowsection.isKeepgoing(),
//                                                      wfCurrentContext
//                    );

                }
                //combine output results for the section
                wfCurrentContext.merge(sectionData.getSharedContext());


                wfresult = sectionSuccess;
                if(!sectionSuccess.isSuccess()) {
                    workflowsuccess = false;
                }
                if (sectionSuccess.getControlBehavior() != null &&
                    sectionSuccess.getControlBehavior() != ControlBehavior.Continue) {
                    controlBehavior = sectionSuccess.getControlBehavior();
                }
                if(sectionSuccess.getStatusString() !=null) {
                    statusString = sectionSuccess.getStatusString();
                }
                if (!workflowsuccess && !item.getWorkflow().isKeepgoing() || controlBehavior == ControlBehavior.Halt) {
                    break;
                }
                stepCount += flowsection.getCommands().size();
            }
            wfresult = workflowResult(workflowsuccess, statusString, controlBehavior, wfCurrentContext);
        } catch (NodesetEmptyException e) {
            Boolean successOnEmptyNodeFilter = Boolean.valueOf(executionContext.getDataContext()
                                                                               .get("job")
                                                                               .get("successOnEmptyNodeFilter"));
            if (!successOnEmptyNodeFilter) {
                exception = e;
                e.printStackTrace();
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                        .getMessage());
                wfresult = WorkflowResultFailed;
            } else {
                logger.debug("No matched nodes");
                wfresult = workflowResult(true, null, ControlBehavior.Continue, wfCurrentContext);
            }
        } catch (RuntimeException e) {
            exception = e;
            e.printStackTrace();
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
            wfresult = WorkflowResultFailed;
        } catch (DispatcherException | ExecutionServiceException e) {
            exception = e;
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
            wfresult = WorkflowResultFailed;
        }
        final Exception fexception = exception;

        return new BaseWorkflowExecutionResult(
                results,
                failures,
                stepFailures,
                fexception,
                wfresult,
                wfCurrentContext
        );
    }

    /**
     * Execute a workflow section that should be dispatched across nodes
     *
     * @return true if the section was succesful
     */
    private WorkflowStatusDataResult executeWFSectionNodeDispatch(
            StepExecutionContext executionContext,
            int stepCount,
            List<StepExecutionResult> results,
            Map<String, Collection<StepExecutionResult>> failures,
            final Map<Integer, StepExecutionResult> stepFailures,
            IWorkflow flowsection,
            WFSharedContext sharedContext
    )
            throws ExecutionServiceException, DispatcherException
    {
        logger.debug("Node dispatch for " + flowsection.getCommands().size() + " steps");
        final DispatcherResult dispatch;
        final WorkflowExecutionItem innerLoopItem = createInnerLoopItem(flowsection);
        final WorkflowExecutor executor = getFramework().getWorkflowExecutionService().getExecutorForItem(innerLoopItem);
        final Dispatchable dispatchedWorkflow = new DispatchedWorkflow(executor,
                                                                       innerLoopItem,
                                                                       stepCount,
                                                                       executionContext.getStepContext());

        WFSharedContext dispatchSharedContext = new WFSharedContext(sharedContext);
        //dispatch the sequence of dispatched items to each node
        dispatch = getFramework().getExecutionService().dispatchToNodes(
                ExecutionContextImpl.builder(executionContext)
                                    .sharedDataContext(dispatchSharedContext)
                                    .stepNumber(stepCount)
                                    .build(),
                dispatchedWorkflow
        );

        logger.debug("Node dispatch result: " + dispatch);
        WFSharedContext resultData = extractWFDispatcherResult(
                dispatch,
                results,
                failures,
                stepFailures,
                flowsection.getCommands().size(),
                stepCount
        );
        return workflowResult(dispatch.isSuccess(), null, ControlBehavior.Continue, resultData);
    }

    /**
     * invert the result of a DispatcherResult where each NodeStepResult  contains a WorkflowResult
     */
    private WFSharedContext extractWFDispatcherResult(
            final DispatcherResult dispatcherResult,
            final List<StepExecutionResult> results,
            final Map<String, Collection<StepExecutionResult>> failures,
            final Map<Integer, StepExecutionResult> stepFailures,
            int index,
            int max
    )
    {
        WFSharedContext wfSharedContext = new WFSharedContext();
        ArrayList<HashMap<String, NodeStepResult>> mergedStepResults = new ArrayList<>(max);
        ArrayList<Boolean> successes = new ArrayList<>(max);
        HashMap<Integer, Map<String, NodeStepResult>> mergedStepFailures
            = new HashMap<>();


        //Convert a dispatcher result to a list of StepExecutionResults.
        //each result for node in the dispatcheresult contains a workflow result
        //unroll each workflow result, append the result of each step into map of node results
        //merge each step result with the mergedStepResults

        //DispatcherResult contains map {nodename: NodeStepResult}
        for (final String nodeName : dispatcherResult.getResults().keySet()) {
            final NodeStepResult stepResult = dispatcherResult.getResults().get(nodeName);

            //This NodeStepResult is produced by the DispatchedWorkflow wrapper
            WorkflowExecutionResult result = DispatchedWorkflow.extractWorkflowResult(stepResult);

            failures.computeIfAbsent(nodeName, k -> new ArrayList<>());

            //extract failures for this node
            final Collection<StepExecutionResult> thisNodeFailures = result.getNodeFailures().get(nodeName);
            if (null != thisNodeFailures && thisNodeFailures.size() > 0) {
                failures.get(nodeName).addAll(thisNodeFailures);
            }

            //extract failures by step (for this node)
            Map<Integer, NodeStepResult> perStepFailures = DispatchedWorkflow.extractStepFailures(result,
                                                                                                  stepResult.getNode());
            for (final Map.Entry<Integer, NodeStepResult> entry : perStepFailures.entrySet()) {
                Integer stepNum = entry.getKey();
                NodeStepResult value = entry.getValue();
                mergedStepFailures.computeIfAbsent(stepNum, k -> new HashMap<>());
                mergedStepFailures.get(stepNum).put(nodeName, value);
            }
            if (result.getResultSet().size() < 1 && result.getNodeFailures().size() < 1 && result.getStepFailures()
                    .size() < 1 && !result.isSuccess()) {

                //failure could be prior to any node step

                mergedStepFailures.computeIfAbsent(0, k -> new HashMap<>());
                mergedStepFailures.get(0).put(nodeName, stepResult);
            }
            //The WorkflowExecutionResult has a list of StepExecutionResults produced by NodeDispatchStepExecutor
            List<NodeStepResult> results1 = DispatchedWorkflow.extractNodeStepResults(result, stepResult.getNode());
            int i = 0;
            for (final NodeStepResult nodeStepResult : results1) {
                while (mergedStepResults.size() <= i) {
                    mergedStepResults.add(new HashMap<>());
                }
                while (successes.size() <= i) {
                    successes.add(Boolean.TRUE);
                }
                HashMap<String, NodeStepResult> map = mergedStepResults.get(i);

                map.put(nodeName, nodeStepResult);

                if (!nodeStepResult.isSuccess()) {
                    successes.set(i, false);
//                    failures.get(nodeName).add(nodeStepResult);
                }
                i++;
            }
        }

        //add a new wrapped DispatcherResults for each original step
        int x = 0;
        for (final HashMap<String, NodeStepResult> map : mergedStepResults) {
            Boolean success = successes.get(x);
            DispatcherResult r = new DispatcherResultImpl(map, null != success ? success : false);
            results.add(NodeDispatchStepExecutor.wrapDispatcherResult(r));
            x++;
        }

        //merge failures for each step
        for (final Integer integer : mergedStepFailures.keySet()) {
            Map<String, NodeStepResult> map = mergedStepFailures.get(integer);

            DispatcherResult r = new DispatcherResultImpl(map, false);
            stepFailures.put(integer, NodeDispatchStepExecutor.wrapDispatcherResult(r));
        }
        return wfSharedContext;
    }
//
//    /**
//     * Execute non-dispatch steps of a workflow
//     *
//     * @return success if all steps were successful
//     */
//    private WorkflowStatusResult executeWFSection(
//            final StepExecutionContext executionContext,
//            final List<StepExecutionResult> results,
//            final Map<String, Collection<StepExecutionResult>> failures,
//            final Map<Integer, StepExecutionResult> stepFailures,
//            final int stepCount,
//            final List<StepExecutionItem> commands,
//            final boolean keepgoing,
//            WFSharedContext sharedContext
//    )
//    {
//
//        WorkflowStatusResult workflowsuccess = executeWorkflowItemsForNodeSet(
//                executionContext,
//                stepFailures,
//                results,
//                commands,
//                keepgoing,
//                stepCount,
//                sharedContext
//        );
//
//        logger.debug("Aggregate results: " + workflowsuccess + " " + results + ", " + stepFailures);
//        Map<String, Collection<StepExecutionResult>> localFailure = convertFailures(stepFailures);
//
//        mergeFailure(failures, localFailure);
//        return workflowsuccess;
//    }

    private void mergeFailure(Map<String, Collection<StepExecutionResult>> destination, Map<String, Collection<StepExecutionResult>> source) {
        for (final String s : source.keySet()) {
            if (null == destination.get(s)) {
                destination.put(s, new ArrayList<>());
            }
            destination.get(s).addAll(source.get(s));
        }
    }

    private void mergeResult(HashMap<String, List<StatusResult>> destination,
                             HashMap<String, List<StatusResult>> source) {
        for (final String s : source.keySet()) {
            if (null == destination.get(s)) {
                destination.put(s, new ArrayList<>());
            }
            destination.get(s).addAll(source.get(s));
        }
    }

    private void validateNodeSet(ExecutionContext executionContext, NodesSelector nodeSelector) {
        if (0 == executionContext.getNodes().getNodes().size()) {
            throw new NodesetEmptyException(nodeSelector);
        }
    }

    static enum Reason implements FailureReason {
        WorkflowSequenceFailures
    }

    /**
     * Workflow execution logic to dispatch an entire workflow sequence to a single node.
     */
    static class DispatchedWorkflow implements Dispatchable {
        WorkflowExecutor executor;
        WorkflowExecutionItem workflowItem;
        int beginStep;
        List<Integer> stack;

        DispatchedWorkflow(WorkflowExecutor executor,
                           WorkflowExecutionItem workflowItem,
                           int beginStep,
                           List<Integer> stack) {
            this.executor = executor;
            this.workflowItem = workflowItem;
            this.beginStep = beginStep;
            this.stack = stack;
        }

        public NodeStepResult dispatch(final ExecutionContext context, final INodeEntry node) {
            final ExecutionContextImpl newcontext = new ExecutionContextImpl.Builder(context)
                .singleNodeContext(node, true)
                .stepNumber(beginStep)
                .stepContext(stack)
                .build();
            WorkflowExecutionResult result = executor.executeWorkflow(newcontext, workflowItem);
            NodeStepResultImpl result1;
            if (result.isSuccess()) {
                result1 = new NodeStepResultImpl(node);
            } else {
                result1 = new NodeStepResultImpl(result.getException(),
                        Reason.WorkflowSequenceFailures,
                        null == result.getException() ? "Sequence failed" : "Exception: " + result.getException()
                                .getClass() + ": " + result.getException().getMessage(),
                        node);
            }
            result1.setSourceResult(result);
            return result1;
        }

        static WorkflowExecutionResult extractWorkflowResult(NodeStepResult dispatcherResult) {
            assert dispatcherResult instanceof HasSourceResult;
            if (!(dispatcherResult instanceof HasSourceResult)) {
                throw new IllegalArgumentException("Cannot extract source result from dispatcher result");
            }
            HasSourceResult sourced = (HasSourceResult) dispatcherResult;
            StatusResult sourceResult = sourced.getSourceResult();
            while (!(sourceResult instanceof WorkflowExecutionResult) && (sourceResult instanceof HasSourceResult)) {
                sourceResult = ((HasSourceResult) sourceResult).getSourceResult();
            }

            if (!(sourceResult instanceof WorkflowExecutionResult)) {
                throw new IllegalArgumentException("Cannot extract workflow result from dispatcher result: " +
                                                   sourceResult.getClass().getName());
            }
            WorkflowExecutionResult wfresult = (WorkflowExecutionResult) sourceResult;
            return wfresult;
        }
        static List<NodeStepResult> extractNodeStepResults(WorkflowExecutionResult result, INodeEntry node) {
            ArrayList<NodeStepResult> results = new ArrayList<>();
            for (final StepExecutionResult executionResult : result.getResultSet()) {

                if (NodeDispatchStepExecutor.isWrappedDispatcherResult(executionResult)) {
                    DispatcherResult dispatcherResult
                        = NodeDispatchStepExecutor.extractDispatcherResult(executionResult);
                    NodeStepResult stepResult = dispatcherResult.getResults().get(node.getNodename());
                    if(null!=stepResult){
                        results.add(stepResult);
                    }
                }else if (NodeDispatchStepExecutor.isWrappedDispatcherException(executionResult)) {
                    DispatcherException exception
                        = NodeDispatchStepExecutor.extractDispatcherException(executionResult);
                    NodeStepException nodeStepException = exception.getNodeStepException();
                    if (null != nodeStepException) {
                        results.add(nodeStepResultFromNodeStepException(node, nodeStepException));
                    }
                }
            }
            return results;
        }
        static Map<Integer,NodeStepResult> extractStepFailures(WorkflowExecutionResult result, INodeEntry node) {
            Map<Integer, NodeStepResult> results = new HashMap<>();
            for (final Map.Entry<Integer, StepExecutionResult> entry : result.getStepFailures().entrySet()) {
                int num = entry.getKey();
                StepExecutionResult executionResult = entry.getValue();
                if (NodeDispatchStepExecutor.isWrappedDispatcherResult(executionResult)) {
                    DispatcherResult dispatcherResult
                        = NodeDispatchStepExecutor.extractDispatcherResult(executionResult);
                    NodeStepResult stepResult = dispatcherResult.getResults().get(node.getNodename());
                    if (null != stepResult) {
                        results.put(num, stepResult);
                    }
                } else if (NodeDispatchStepExecutor.isWrappedDispatcherException(executionResult)) {
                    DispatcherException exception
                        = NodeDispatchStepExecutor.extractDispatcherException(executionResult);
                    NodeStepException nodeStepException = exception.getNodeStepException();
                    if (null != nodeStepException) {
                        results.put(
                                num,
                                nodeStepResultFromNodeStepException(node, nodeStepException)
                        );
                    }
                }
            }
            return results;
        }
    }

    /**
     * Splits a workflow into a sequence of sub-workflows, separated along boundaries of node-dispatch sets.
     */
    private List<IWorkflow> splitWorkflowDispatchedSections(IWorkflow workflow) throws ExecutionServiceException {
        ArrayList<StepExecutionItem> dispatchItems = new ArrayList<>();
        ArrayList<IWorkflow> sections = new ArrayList<>();
        for (final StepExecutionItem item : workflow.getCommands()) {
            StepExecutor executor = getFramework().getStepExecutionService().getExecutorForItem(item);
            if (executor.isNodeDispatchStep(item)) {
                dispatchItems.add(item);
            } else {
                if (dispatchItems.size() > 0) {
                    //add workflow section
                    sections.add(new WorkflowImpl(dispatchItems,
                                                  workflow.getThreadcount(),
                                                  workflow.isKeepgoing(),
                                                  workflow.getStrategy()));
                    dispatchItems = new ArrayList<>();
                }

                sections.add(new WorkflowImpl(Collections.singletonList(item),
                                              workflow.getThreadcount(),
                                              workflow.isKeepgoing(),
                                              workflow.getStrategy()));
            }
        }
        if (null != dispatchItems && dispatchItems.size() > 0) {
            //add workflow section
            sections.add(new WorkflowImpl(dispatchItems,
                                          workflow.getThreadcount(),
                                          workflow.isKeepgoing(),
                                          workflow.getStrategy()));
        }
        return sections;
    }


    /**
     * Create workflowExecutionItem suitable for inner loop of node-first strategy
     * @param item workflow item
     *             @return inner loop
     */
    public static WorkflowExecutionItem createInnerLoopItem(IWorkflow item) {
        return new WorkflowExecutionItemImpl(new StepFirstWrapper(item));
    }

}
