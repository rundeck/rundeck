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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.utils.NodeSet;

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
        boolean nodesuccess = false;

        final HashMap<String, List<StatusResult>> results = new HashMap<String, List<StatusResult>>();
        final Map<String, Collection<String>> failures = new HashMap<String, Collection<String>>();
        try {
            final NodeSet nodeSet = executionContext.getNodeSet();

            final List<ExecutionItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            //retrieve the node set
            final Collection<INodeEntry> nodes;
            final String project = executionContext.getFrameworkProject();
            try {
                nodes = framework.filterNodes(nodeSet, project);
            } catch (NodeFileParserException e) {
                throw new CoreException("Error parsing node resource file: " + e.getMessage(), e);
            }
            if (0 == nodes.size()) {
                throw new NodesetEmptyException(nodeSet);
            }
            final WorkflowExecutionItem innerLoopItem = createInnerLoopItem(item);
            final WorkflowExecutor executor = framework.getWorkflowExecutionService().getExecutorForItem(innerLoopItem);
            final DispatcherResult dispatch = framework.getExecutionService().dispatchToNodes(executionContext,
                new Dispatchable() {
                    public StatusResult dispatch(final ExecutionContext context, final INodeEntry node) throws
                        DispatcherException {
                        //use single node context
                        return executor.executeWorkflow(ExecutionContextImpl.createExecutionContextImpl(context, node),
                            innerLoopItem);
                    }
                });

            //extract node-oriented results from dispatch results
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
                    results.put(s, collection);
                }
                for (final String s : workflowExecutionResult.getFailureMessages().keySet()) {
                    final Collection<String> strings = workflowExecutionResult.getFailureMessages().get(s);
                    failures.put(s, strings);
                }
                if(null!=workflowExecutionResult.getException()) {
                    final ArrayList<String> strings = new ArrayList<String>();
                    if(null!=failures.get(nodename)) {
                        strings.addAll(failures.get(nodename));
                    }
                    strings.add(workflowExecutionResult.getException().getMessage());
                    failures.put(nodename, strings);
                }
            }

            nodesuccess = dispatch.isSuccess();

        } catch (Exception e) {
            exception = e;
        }
        final boolean success = nodesuccess;
        final Exception fexception = exception;

        return new WorkflowExecutionResult(results, failures, success, fexception);
    }


    /**
     * Create workflowExecutionItem suitable for inner loop of node-first strategy
     */
    public static WorkflowExecutionItem createInnerLoopItem(WorkflowExecutionItem item) {
        final WorkflowExecutionItemImpl workflowExecutionItem = new WorkflowExecutionItemImpl(
            new StepFirstWorkflowStrategy.stepFirstWrapper(item.getWorkflow()));
        return workflowExecutionItem;
    }

}