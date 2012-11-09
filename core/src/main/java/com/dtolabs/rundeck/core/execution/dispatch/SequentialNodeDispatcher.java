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
import com.dtolabs.rundeck.core.NodesetFailureException;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

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

    public DispatcherResult dispatch(final ExecutionContext context,
                                     final NodeStepExecutionItem item) throws
        DispatcherException {
        return dispatch(context, item, null);
    }

    public DispatcherResult dispatch(final ExecutionContext context,
                                     final Dispatchable item) throws
        DispatcherException {
        return dispatch(context, null, item);
    }

    public DispatcherResult dispatch(final ExecutionContext context,
                                     final NodeStepExecutionItem item, final Dispatchable toDispatch) throws
        DispatcherException {
        final NodesSelector nodesSelector = context.getNodeSelector();
        INodeSet nodes = null;
        try {
            nodes = framework.filterAuthorizedNodes(context.getFrameworkProject(),
                new HashSet<String>(Arrays.asList("read", "run")),
                framework.filterNodeSet(nodesSelector, context.getFrameworkProject(), context.getNodesFile()));
        } catch (NodeFileParserException e) {
            throw new DispatcherException(e);
        }
        if(nodes.getNodes().size()<1) {
            throw new DispatcherException("No nodes matched");
        }
        boolean keepgoing = context.isKeepgoing();

        context.getExecutionListener().log(4, "preparing for sequential execution on " + nodes.getNodes().size() + " nodes");
        final HashSet<String> nodeNames = new HashSet<String>(nodes.getNodeNames());
        final HashMap<String,Object> failures = new HashMap<String,Object>();
        FailedNodesListener failedListener = context.getExecutionListener().getFailedNodesListener();
        if (null != failedListener) {
            failedListener.matchedNodes(nodeNames);
        }
        boolean interrupted = false;
        final Thread thread = Thread.currentThread();
        boolean success = true;
        final HashMap<String, NodeStepResult> resultMap = new HashMap<String, NodeStepResult>();
        final Collection<INodeEntry> nodes1 = nodes.getNodes();
        //reorder based on configured rank property and order
        final String rankProperty = null != context.getNodeRankAttribute() ? context.getNodeRankAttribute() : "nodename";
        final boolean rankAscending = context.isNodeRankOrderAscending();
        final INodeEntryComparator comparator = new INodeEntryComparator(rankProperty);
        final TreeSet<INodeEntry> orderedNodes = new TreeSet<INodeEntry>(
            rankAscending ? comparator : Collections.reverseOrder(comparator));
        orderedNodes.addAll(nodes1);
        Throwable caught=null;
        INodeEntry failedNode=null;
        for (final Object node1 : orderedNodes) {
            if (thread.isInterrupted()
                || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                interrupted = true;
                break;
            }
            final INodeEntry node = (INodeEntry) node1;
            context.getExecutionListener().log(Constants.DEBUG_LEVEL,
                "Executing command on node: " + node.getNodename() + ", " + node.toString());
            try {

                if (thread.isInterrupted()
                    || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                    interrupted = true;
                    break;
                }
                final NodeStepResult result;
                final ExecutionContext interimcontext = new ExecutionContextImpl.Builder(context).nodeSelector(
                    SelectorUtils.singleNode(node.getNodename())).build();

                //execute the step or dispatchable
                if (null != item) {
                    result = framework.getExecutionService().executeNodeStep(interimcontext, item, node);
                } else {
                    result = toDispatch.dispatch(interimcontext, node);

                }
                if (null != result) {
                    resultMap.put(node.getNodename(), result);
                }
                if (null == result || !result.isSuccess()) {
                    success = false;
//                    context.getExecutionListener().log(Constants.ERR_LEVEL,
//                        "Failed execution for node " + node.getNodename() + ": " + result);
                    if(null!=result) {
                        failures.put(node.getNodename(), result);
                    }else{
                        failures.put(node.getNodename(),
                            "Failed execution, result was null");
                    }
                    if (!keepgoing) {
                        failedNode=node;
                        break;
                    }
                } else {
                    nodeNames.remove(node.getNodename());
                }
            } catch (Throwable e) {
                success = false;
                failures.put(node.getNodename(), "Error dispatching command to the node: " + e.getMessage());
                context.getExecutionListener().log(Constants.ERR_LEVEL,
                    "Failed dispatching to node " + node.getNodename() + ": " + e.getMessage());

                final StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                context.getExecutionListener().log(Constants.DEBUG_LEVEL,
                    "Failed dispatching to node " + node.getNodename() + ": " + stringWriter.toString());

                if (!keepgoing) {
                    failedNode=node;
                    caught=e;
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
            //XXX: needs to change from exception
            throw new NodesetFailureException(failures);
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
