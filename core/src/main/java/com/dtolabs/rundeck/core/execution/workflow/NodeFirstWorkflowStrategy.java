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
import com.dtolabs.rundeck.core.execution.HasSourceResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResultImpl;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.NodeDispatchStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
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
 */
public class NodeFirstWorkflowStrategy extends BaseWorkflowStrategy {
    static final Logger logger = Logger.getLogger(NodeFirstWorkflowStrategy.class.getName());

    public NodeFirstWorkflowStrategy(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final StepExecutionContext executionContext,
                                                       final WorkflowExecutionItem item) {
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        boolean wfsuccess = true;

        final ArrayList<StepExecutionResult> results = new ArrayList<StepExecutionResult>();
        final Map<String, Collection<String>> failures = new HashMap<String, Collection<String>>();
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
            int stepCount=1;
            if(sections.size()>1){
                logger.debug("Split workflow into " + sections.size() + " sections");
            }
            assert sections.size()>=1;
            if(sections.size()<1) {
                throw new IllegalStateException();
            }
            for (final IWorkflow flowsection: sections) {
                boolean sectionSuccess=true;

                StepExecutor stepExecutor = framework.getStepExecutionService()
                    .getExecutorForItem(flowsection.getCommands().get(0));

                if(stepExecutor.isNodeDispatchStep(flowsection.getCommands().get(0))) {
                    sectionSuccess = executeWFSectionNodeDispatch(executionContext,
                                                                  stepCount,
                                                                  results,
                                                                  failures,
                                                                  flowsection
                    );
                }else {
                    //execute each item sequentially
                    sectionSuccess = executeWFSection(executionContext,
                                                      results,
                                                      failures,
                                                      stepCount,
                                                      flowsection.getCommands(),
                                                      flowsection.isKeepgoing());

                }
                if (!sectionSuccess && !item.getWorkflow().isKeepgoing()) {
                    throw new WorkflowFailureException("Some steps in the workflow failed: " + failures);
                }
                if(!sectionSuccess){
                    wfsuccess=false;
                }
                stepCount += flowsection.getCommands().size();
            }
        } catch (RuntimeException e) {
            exception = e;
            wfsuccess = false;
        } catch (DispatcherException e) {
            exception = e;
            wfsuccess = false;
        } catch (ExecutionServiceException e) {
            exception = e;
            wfsuccess = false;
        } catch (WorkflowStepFailureException e) {
            exception=e;
            wfsuccess=false;
        } catch (WorkflowFailureException e) {
            exception = e;
            wfsuccess = false;
        }
        final boolean success = wfsuccess;
        final Exception fexception = exception;

        return new BaseWorkflowExecutionResult(results, failures, success, fexception);
    }

    /**
     * Execute a workflow section that should be dispatched across nodes
     * @return true if the section was succesful
     */
    private boolean executeWFSectionNodeDispatch(StepExecutionContext executionContext,
                                                 int stepCount,
                                                 List<StepExecutionResult> results,
                                                 Map<String, Collection<String>> failures,
                                                 IWorkflow flowsection)
        throws ExecutionServiceException, DispatcherException {
        logger.debug("Node dispatch for " + flowsection.getCommands().size() + " steps");
        final DispatcherResult dispatch;
        final WorkflowExecutionItem innerLoopItem = createInnerLoopItem(flowsection);
        final WorkflowExecutor executor = framework.getWorkflowExecutionService().getExecutorForItem(innerLoopItem);
        final Dispatchable dispatchedWorkflow = new DispatchedWorkflow(executor, innerLoopItem, stepCount, executionContext.getStepContext());
        //dispatch the sequence of dispatched items to each node
        dispatch = framework.getExecutionService().dispatchToNodes(
            ExecutionContextImpl.builder(executionContext)
                .stepNumber(stepCount)
                .build(),
            dispatchedWorkflow);

        logger.debug("Node dispatch result: " + dispatch);
        extractWFDispatcherResult(dispatch, results, failures, flowsection.getCommands().size());
        return dispatch.isSuccess();
    }

    /**
     * invert the result of a DispatcherResult which contains a WorkflowResult
     */
    private void extractWFDispatcherResult(DispatcherResult dispatcherResult,
                                           List<StepExecutionResult> results,
                                           Map<String, Collection<String>> failures,
                                           int max) {
        ArrayList<HashMap<String, NodeStepResult>> full = new ArrayList<HashMap<String, NodeStepResult>>(max);
        ArrayList<Boolean> successes = new ArrayList<Boolean>(max);
        HashMap<String, ArrayList<NodeStepResult>> im = new HashMap<String, ArrayList<NodeStepResult>>();
        //Convert a dispatcher result to a list of StepExecutionResults.
        //each result for node in the dispatcheresult contains a workflow result
            //unroll each workflow result, append the result of each step into map of node results

        //DispatcherResult contains map {nodename: NodeStepResult}
        for (final String nodeName : dispatcherResult.getResults().keySet()) {
            final NodeStepResult stepResult = dispatcherResult.getResults().get(nodeName);

            //This NodeStepResult is produced by the DispatchedWorkflow wrapper
            WorkflowExecutionResult result = DispatchedWorkflow.extractStepResult(stepResult);

            if (null == failures.get(nodeName)) {
                failures.put(nodeName, new ArrayList<String>());
            }

            final Collection<String> strings = result.getFailureMessages().get(nodeName);
            if(null!=strings && strings.size()>0){
                failures.get(nodeName).addAll(strings);
            }
            if (null != result.getException()) {
                failures.get(nodeName).add(result.getException().getMessage());
            }
            //The WorkflowExecutionResult has a list of StepExecutionResults produced by NodeDispatchStepExecutor
            int i=0;
            for (final StepExecutionResult partStepResult : result.getResultSet()) {

                while (full.size() <= i) {
                    full.add(new HashMap<String, NodeStepResult>());
                }
                while (successes.size() <= i) {
                    successes.add(Boolean.TRUE);
                }
                HashMap<String, NodeStepResult> map = full.get(i);

                if(NodeDispatchStepExecutor.isWrappedDispatcherResult(partStepResult)){
                    DispatcherResult subresult = NodeDispatchStepExecutor.extractDispatcherResult(partStepResult);

                    NodeStepResult result1 = subresult.getResults().get(nodeName);
                    map.put(nodeName, result1);

                    if (!result1.isSuccess()) {
                        successes.set(i, false);
                        failures.get(nodeName).add(result1.toString());
                    }
                }else if (null!=partStepResult && !partStepResult.isSuccess()) {
                    successes.set(i, false);
                    if (null!=partStepResult.getException()) {
                        failures.get(nodeName).add(partStepResult.getException().toString());
                    } else {
                        failures.get(nodeName).add(partStepResult.toString());
                    }
                }

                i++;
            }


        }

        //add a new wrapped DispatcherResults for each original step
        int x=0;
        for (final HashMap<String, NodeStepResult> map : full) {
            Boolean success = successes.get(x);
            DispatcherResult r = new DispatcherResultImpl(map, null != success ? success : false);
            results.add(NodeDispatchStepExecutor.wrapDispatcherResult(r));
            x++;
        }

    }

    /**
     * Execute non-dispatch steps of a workflow
     * @return success if all steps were successful
     */
    private boolean executeWFSection(StepExecutionContext executionContext,
                                     List<StepExecutionResult> results,
                                     Map<String, Collection<String>> failures,
                                     int stepCount,
                                     final List<StepExecutionItem> commands, final boolean keepgoing)
        throws WorkflowStepFailureException {
        Map<Integer, Object> failedMap = new HashMap<Integer, Object>();

        boolean workflowsuccess=executeWorkflowItemsForNodeSet(executionContext,
                                       failedMap,
                                       results,
                                       commands,
                                       keepgoing, stepCount);

        logger.debug("Aggregate results: " + workflowsuccess + " " + results + ", " + failedMap);
        Map<String, Collection<String>> localFailure = convertFailures(failedMap);

        mergeFailure(failures, localFailure);
        return workflowsuccess;
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

        public NodeStepResult dispatch(final ExecutionContext context,
                                     final INodeEntry node) throws DispatcherException {
            //XXX: not necessary, use passed in context, will be in single node context already
            final ExecutionContextImpl newcontext = new ExecutionContextImpl.Builder(context)
                .nodeSelector(SelectorUtils.singleNode(node.getNodename()))
                .stepNumber(beginStep)
                .stepContext(stack)
                .build();
            WorkflowExecutionResult result = executor.executeWorkflow(newcontext, workflowItem);
            NodeStepResultImpl result1 = new NodeStepResultImpl(result.isSuccess(), node);
            result1.setSourceResult(result);
            return result1;
        }

        static WorkflowExecutionResult extractStepResult(NodeStepResult dispatcherResult) {
            assert dispatcherResult instanceof HasSourceResult;
            if (!(dispatcherResult instanceof HasSourceResult)) {
                throw new IllegalArgumentException("Cannot extract source result from dispatcher result");
            }
            HasSourceResult sourced = (HasSourceResult) dispatcherResult;
            StatusResult sourceResult = sourced.getSourceResult();
            assert sourceResult instanceof WorkflowExecutionResult;
            if (!(sourceResult instanceof WorkflowExecutionResult)) {
                throw new IllegalArgumentException("Cannot extract workflow result from dispatcher result");
            }
            WorkflowExecutionResult wfresult = (WorkflowExecutionResult) sourceResult;
            return wfresult;
        }
    }
    /**
     * Splits a workflow into a sequence of sub-workflows, separated along boundaries of node-dispatch sets.
     */
    private List<IWorkflow> splitWorkflowDispatchedSections(IWorkflow workflow) throws ExecutionServiceException {
        ArrayList<StepExecutionItem> dispatchItems = new ArrayList<StepExecutionItem>();
        ArrayList<IWorkflow> sections = new ArrayList<IWorkflow>();
        for (final StepExecutionItem item : workflow.getCommands()) {
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
                    dispatchItems = new ArrayList<StepExecutionItem>();
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