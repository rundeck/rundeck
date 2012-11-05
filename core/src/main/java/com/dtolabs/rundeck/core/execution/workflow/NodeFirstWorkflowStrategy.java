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
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;

import java.util.*;

/**
 * NodeFirstWorkflowStrategy Iterates over the matched nodes first, so that each node executes the full workflow
 * sequentially
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeFirstWorkflowStrategy extends BaseWorkflowStrategy {

    public NodeFirstWorkflowStrategy(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final ExecutionContext executionContext,
                                                       final WorkflowExecutionItem item) {
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        boolean wfsuccess = false;

        final HashMap<String, List<StatusResult>> results = new HashMap<String, List<StatusResult>>();
        final Map<String, Collection<String>> failures = new HashMap<String, Collection<String>>();
        try {
            final NodesSelector nodeSelector = executionContext.getNodeSelector();

            if (workflow.getCommands().size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            validateNodeSet(executionContext, nodeSelector);

            //split workflow around non-node dispatched items, and loop
            //each dispatched sequence should be wrapped in a separate dispatch
            //and each non-dispatched step performed separately
            final List<IWorkflow> sections = splitWorkflowDispatchedSections(workflow);
            int stepCount=1;
            for (IWorkflow flowsection: sections) {
                final ExecutionContext dispatchContext;

                StepExecutor stepExecutor = framework.getStepExecutionService()
                    .getExecutorForItem(flowsection.getCommands().get(0));

                if(stepExecutor.isNodeDispatchStep(flowsection.getCommands().get(0))){
                    final DispatcherResult dispatch;
                    final WorkflowExecutionItem innerLoopItem = createInnerLoopItem(flowsection);
                    final WorkflowExecutor executor = framework.getWorkflowExecutionService()
                        .getExecutorForItem(innerLoopItem);
                    final Dispatchable dispatchedWorkflow = new DispatchedWorkflow(executor, innerLoopItem);
                    //dispatch the sequence of dispatched items to each node
                    dispatchContext=executionContext;
                    dispatch = framework.getExecutionService().dispatchToNodes(dispatchContext, dispatchedWorkflow);

                    if (!dispatch.isSuccess() && !item.getWorkflow().isKeepgoing()) {
                        throw new WorkflowFailureException("Some steps in the workflow failed: " + dispatch);
                    }
                    if (!extractResults(results, failures, dispatch)) {
                        wfsuccess = false;
                    }
                }else {
                    //execute each item sequentially
                    Map<Integer, Object> failedMap = new HashMap<Integer, Object>();
                    List<DispatcherResult> results1 = new ArrayList<DispatcherResult>();

                    boolean workflowsuccess=executeWorkflowItemsForNodeSet(executionContext,
                                                   failedMap,
                                                   results1,
                                                   flowsection.getCommands(),
                                                   flowsection.isKeepgoing(), stepCount);
                    HashMap<String, List<StatusResult>> localResult = convertResults(results1);
                    Map<String, Collection<String>> localFailure = convertFailures(failedMap);
                    mergeResult(results, localResult);
                    mergeFailure(failures, localFailure);
                    if (!workflowsuccess) {
                        throw new WorkflowFailureException("Some steps in the workflow failed: " + failures);
                    }
//                    dispatchContext = new ExecutionContextImpl.Builder(executionContext)
//                        .nodeSelector(SelectorUtils.singleNode(framework.getFrameworkNodeName()))
//                        .build();
//                    dispatch = framework.getExecutionService().dispatchToNodes(dispatchContext, dispatchedWorkflow);
                }
                stepCount += flowsection.getCommands().size();
            }
            wfsuccess=true;
        } catch (RuntimeException e) {
            exception = e;
        } catch (DispatcherException e) {
            exception = e;
        } catch (ExecutionServiceException e) {
            exception = e;
        } catch (WorkflowStepFailureException e) {
            exception=e;
            wfsuccess=false;
        } catch (WorkflowFailureException e) {
            exception = e;
            wfsuccess = false;
        }
        final boolean success = wfsuccess;
        final Exception fexception = exception;

        return new WorkflowExecutionResult(results, failures, success, fexception);
    }

    private void mergeFailure(Map<String, Collection<String>> destination, Map<String, Collection<String>> source) {
        for (final String s : source.keySet()) {
            if (null == destination.get(s)) {
                destination.put(s, new ArrayList<String>());
            }
            destination.get(s).addAll(source.get(s));
        }
    }

    private void mergeResult(HashMap<String, List<StatusResult>> destination, HashMap<String, List<StatusResult>> source) {
        for (final String s : source.keySet()) {
            if(null== destination.get(s)) {
                destination.put(s, new ArrayList<StatusResult>());
            }
            destination.get(s).addAll(source.get(s));
        }
    }

    private void validateNodeSet(ExecutionContext executionContext, NodesSelector nodeSelector) {
        //retrieve the node set
        final INodeSet nodes;
        final String project = executionContext.getFrameworkProject();
        try {
            nodes = framework.filterAuthorizedNodes(project,
                new HashSet<String>(Arrays.asList("read", "run")),
                framework.filterNodeSet(nodeSelector, project, executionContext.getNodesFile()));
        } catch (NodeFileParserException e) {
            throw new CoreException("Error parsing node resource file: " + e.getMessage(), e);
        }
        if (0 == nodes.getNodes().size()) {
            throw new NodesetEmptyException(nodeSelector);
        }
    }

    /**
     * Workflow execution logic to dispatch an entire workflow sequence to a single node.
     */
    static class DispatchedWorkflow implements Dispatchable{
        WorkflowExecutor executor;
        WorkflowExecutionItem workflowItem;

        DispatchedWorkflow(final WorkflowExecutor executor, final WorkflowExecutionItem workflowItem) {
            this.executor = executor;
            this.workflowItem = workflowItem;
        }

        public StatusResult dispatch(final ExecutionContext context,
                                     final INodeEntry node) throws DispatcherException {
            //use single node context
            final ExecutionContextImpl newcontext = new ExecutionContextImpl.Builder(context)
                .nodeSelector(SelectorUtils.singleNode(node.getNodename()))
                .build();
            return executor.executeWorkflow(newcontext, workflowItem);
        }
    }
    /**
     * Splits a workflow into a sequence of sub-workflows, separated along boundaries of node-dispatch sets.
     */
    private List<IWorkflow> splitWorkflowDispatchedSections(IWorkflow workflow) throws ExecutionServiceException {
        ArrayList<ExecutionItem> dispatchItems = new ArrayList<ExecutionItem>();
        ArrayList<IWorkflow> sections = new ArrayList<IWorkflow>();
        for (final ExecutionItem item : workflow.getCommands()) {
            StepExecutor executor = framework.getStepExecutionService().getExecutorForItem(item);
            if (executor.isNodeDispatchStep(item)) {
                dispatchItems.add(item);
            }else{
                if(dispatchItems.size()>0) {
                    //add workflow section
                    sections.add(new WorkflowImpl(dispatchItems,
                                                  workflow.getThreadcount(),
                                                  workflow.isKeepgoing(),
                                                  workflow.getStrategy()));
                    dispatchItems = new ArrayList<ExecutionItem>();
                }

                sections.add(new WorkflowImpl(Collections.singletonList(item),
                                              workflow.getThreadcount(),
                                              workflow.isKeepgoing(),
                                              workflow.getStrategy()));
            }
        }
        if (null!=dispatchItems && dispatchItems.size() > 0) {
            //add workflow section
            sections.add(new WorkflowImpl(dispatchItems,
                                          workflow.getThreadcount(),
                                          workflow.isKeepgoing(),
                                          workflow.getStrategy()));
        }
        return sections;
    }

    private boolean extractResults(HashMap<String, List<StatusResult>> results,
                                Map<String, Collection<String>> failures,
                                DispatcherResult dispatch) {
        boolean result=true;
        for (final String nodename : dispatch.getResults().keySet()) {

            //each dispatch result for a node will be complete workflow result for step-first workflow strategy
            final StatusResult interpreterResult = dispatch.getResults().get(nodename);
            final WorkflowExecutionResult workflowExecutionResult =
                (WorkflowExecutionResult) interpreterResult;
            /**
             * Specific result for workflow on single node
             */
            for (final String s : workflowExecutionResult.getResultSet().keySet()) {
                //should be single key in this result set, corresponding to single node used for step first workflow exec
                /**
                 * Collection of results for each workflow item
                 */
                final List<StatusResult> collection = workflowExecutionResult.getResultSet().get(s);
                //include any failures in the failures group

                final ArrayList<StatusResult> statusResults = new ArrayList<StatusResult>();
                for (final StatusResult statusResult : collection) {
                    if (!statusResult.isSuccess()) {
                        if (null == failures.get(s)) {
                            failures.put(s, new ArrayList<String>());
                        }
                        failures.get(s).add(statusResult.toString());
                    } else {
                        statusResults.add(statusResult);
                    }
                }
                results.put(s, statusResults);
            }
            for (final String s : workflowExecutionResult.getFailureMessages().keySet()) {
                final Collection<String> strings = workflowExecutionResult.getFailureMessages().get(s);
                if (null == failures.get(s)) {
                    failures.put(s, new ArrayList<String>());
                }
                failures.get(s).addAll(strings);
            }
            if (null != workflowExecutionResult.getException()) {
                if (null == failures.get(nodename)) {
                    failures.put(nodename, new ArrayList<String>());
                }
                failures.get(nodename).add(workflowExecutionResult.getException().getMessage());
            }
            if (!dispatch.isSuccess()) {
                result = false;
            }
        }
        return result;
    }


    /**
     * Create workflowExecutionItem suitable for inner loop of node-first strategy
     */
    public static WorkflowExecutionItem createInnerLoopItem(WorkflowExecutionItem item) {
        return createInnerLoopItem(item.getWorkflow());
    }


    /**
     * Create workflowExecutionItem suitable for inner loop of node-first strategy
     */
    public static WorkflowExecutionItem createInnerLoopItem(IWorkflow item) {
        final WorkflowExecutionItemImpl workflowExecutionItem = new WorkflowExecutionItemImpl(
            new StepFirstWorkflowStrategy.stepFirstWrapper(item));
        return workflowExecutionItem;
    }

}