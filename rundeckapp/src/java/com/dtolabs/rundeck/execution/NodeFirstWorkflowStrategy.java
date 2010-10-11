/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* WFFirstWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:16:49 PM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.cli.DefaultNodeDispatcher;
import com.dtolabs.rundeck.core.cli.NodeCallableFactory;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.execution.BaseExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.tools.ant.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * NodeFirstWorkflowStrategy Iterates over the matched nodes first, so that each node executes the full workflow
 * sequentially
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodeFirstWorkflowStrategy extends BaseWorkflowStrategy implements NodeCallableFactory {
    private DefaultNodeDispatcher nodeDispatcher;
    final List resultList;

    public NodeFirstWorkflowStrategy(final WorkflowExecutionItem item, final ExecutionService executionService,
                                     final ExecutionListener listener, final Framework framework) {
        super(item,executionService, listener, framework);
        nodeDispatcher = new DefaultNodeDispatcher();
        resultList = new ArrayList();
    }

    public ExecutionResult executeWorkflow() {

        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        boolean nodesuccess=false;
        try {
            final NodeSet baseNodeSet = item.getNodeSet();
            listener.log(Constants.DEBUG_LEVEL, "NodeSet: " + baseNodeSet);
            listener.log(Constants.DEBUG_LEVEL, "Workflow: " + workflow);
            listener.log(Constants.DEBUG_LEVEL, "data context: " + item.getDataContext());

            final List<IWorkflowCmdItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                listener.log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            //retrieve the node set
            final Collection<INodeEntry> nodes;
            try {
                nodes = framework.filterNodes(baseNodeSet, item.getProject());
            } catch (NodeFileParserException e) {
                throw new CoreException("Error parsing node resource file: " + e.getMessage(), e);
            }
            if (0 == nodes.size()) {
                throw new NodesetEmptyException(baseNodeSet);
            }

            nodeDispatcher.executeNodedispatch(new Project(), nodes, baseNodeSet.getThreadCount(),
                baseNodeSet.isKeepgoing(), listener.getFailedNodesListener(), this);
            nodesuccess=true;

        } catch (Exception e) {
            exception = e;
        }
        if (nodesuccess) {
            return BaseExecutionResult.createSuccess(resultList);
        } else {
            return BaseExecutionResult.createFailure(exception);
        }

    }

    /**
     * Create Callables to execute the workflow on a single node
     * @param node
     * @return
     */
    public Callable createCallable(final INodeEntry node) {
        //create temporary nodeset for the single node
        final NodeSet tempNodeset = new NodeSet();
        tempNodeset.setSingleNodeName(node.getNodename());
        final IWorkflow workflow = item.getWorkflow();
        final List<IWorkflowCmdItem> iWorkflowCmdItems = workflow.getCommands();
        return new Callable() {
            public Object call() throws Exception {
                final List<String> failedList = new ArrayList<String>();
                if (!executeWorkflowItemsForNodeSet(workflow,failedList, resultList, iWorkflowCmdItems, tempNodeset)) {
                    throw new WorkflowAction.WorkflowFailureException(
                        "Some steps in the workflow failed: " + failedList);
                }
                return resultList;
            }
        };

    }
}