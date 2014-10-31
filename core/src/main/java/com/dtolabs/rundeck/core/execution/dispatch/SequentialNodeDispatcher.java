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
* SequentialNodeDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 11:54 AM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.ServiceThreadBase;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


/**
 * SequentialNodeDispatcher is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class SequentialNodeDispatcher implements NodeDispatcher {
    private Framework framework;

    public SequentialNodeDispatcher(Framework framework) {
        this.framework = framework;
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final NodeStepExecutionItem item) throws
                                                                       DispatcherException {
        return dispatch(context, item, null);
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final Dispatchable item) throws
                                                              DispatcherException {
        return dispatch(context, null, item);
    }

    public DispatcherResult dispatch(final StepExecutionContext context,
                                     final NodeStepExecutionItem item, final Dispatchable toDispatch) throws
                                                                                                      DispatcherException {

        INodeSet nodes = context.getNodes();
        if (nodes.getNodes().size() < 1) {
            throw new DispatcherException("No nodes matched");
        }
        boolean keepgoing = context.isKeepgoing();

        context.getExecutionListener()
            .log(4, "preparing for sequential execution on " + nodes.getNodes().size() + " nodes");
        final HashSet<String> nodeNames = new HashSet<String>(nodes.getNodeNames());
        final HashMap<String, NodeStepResult> failures = new HashMap<String, NodeStepResult>();
        FailedNodesListener failedListener = context.getExecutionListener().getFailedNodesListener();
        if (null != failedListener) {
            failedListener.matchedNodes(nodeNames);
        }
        boolean interrupted = false;
        final Thread thread = Thread.currentThread();
        boolean success = true;
        final HashMap<String, NodeStepResult> resultMap = new HashMap<String, NodeStepResult>();
        final List<INodeEntry> nodes1 = INodeEntryComparator.rankOrderedNodes(nodes, context
                .getNodeRankAttribute(),
                context.isNodeRankOrderAscending());
        //reorder based on configured rank property and order

        NodeStepException caught = null;
        INodeEntry failedNode = null;
        for (final INodeEntry node : nodes1) {
            if (thread.isInterrupted()
                || thread instanceof ServiceThreadBase && ((ServiceThreadBase) thread).isAborted()) {
                interrupted = true;
                break;
            }
            context.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                               "Executing command on node: " + node.getNodename() + ", "
                                               + node.toString());
            try {

                if (thread.isInterrupted()
                    || thread instanceof ServiceThreadBase && ((ServiceThreadBase) thread).isAborted()) {
                    interrupted = true;
                    break;
                }
                final NodeStepResult result;

                //execute the step or dispatchable
                if (null != item) {
                    result = framework.getExecutionService().executeNodeStep(context, item, node);
                } else {
                    result = toDispatch.dispatch(context, node);

                }
                resultMap.put(node.getNodename(), result);
                if (!result.isSuccess()) {
                    success = false;
//                    context.getExecutionListener().log(Constants.ERR_LEVEL,
//                        "Failed execution for node " + node.getNodename() + ": " + result);
                    failures.put(node.getNodename(), result);
                    if (!keepgoing) {
                        failedNode = node;
                        break;
                    }
                } else {
                    nodeNames.remove(node.getNodename());
                }
            } catch (NodeStepException e) {
                success = false;
                failures.put(node.getNodename(),
                             new NodeStepResultImpl(e, e.getFailureReason(), e.getMessage(), node)
                );
                context.getExecutionListener().log(Constants.ERR_LEVEL,
                                                   "Failed dispatching to node " + node.getNodename() + ": "
                                                   + e.getMessage());

                final StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                context.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                   "Failed dispatching to node " + node.getNodename() + ": "
                                                   + stringWriter.toString());

                if (!keepgoing) {
                    failedNode = node;
                    caught = e;
                    break;
                }
            }
        }
        if (!keepgoing && failures.size() > 0 && null != failedListener) {
            //tell listener of failed node list
            failedListener.nodesFailed(failures);
        }
        if (!keepgoing && null != caught) {
            throw new DispatcherException(
                "Failed dispatching to node " + failedNode.getNodename() + ": " + caught.getMessage(), caught,
                failedNode);
        }
        if (keepgoing && nodeNames.size() > 0) {
            if (null != failedListener) {
                //tell listener of failed node list
                failedListener.nodesFailed(failures);
            }
            //now fail
            return new DispatcherResultImpl(failures, false);
        } else if (null != failedListener && failures.isEmpty() && !interrupted) {
            failedListener.nodesSucceeded();
        }
        if (interrupted) {
            throw new DispatcherException("Node dispatch interrupted");
        }

        final boolean status = success;
        return new DispatcherResultImpl(resultMap, status);
    }
}
