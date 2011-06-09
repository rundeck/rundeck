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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.utils.NodeSet;

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
                                     final ExecutionItem item) throws
        DispatcherException {
        return dispatch(context, item, null);
    }

    public DispatcherResult dispatch(final ExecutionContext context,
                                     final Dispatchable item) throws
        DispatcherException {
        return dispatch(context, null, item);
    }

    public DispatcherResult dispatch(final ExecutionContext context,
                                     final ExecutionItem item, final Dispatchable toDispatch) throws
        DispatcherException {
        final NodeSet nodeset = context.getNodeSet();
        Collection<INodeEntry> nodes = null;
        try {
            nodes = framework.filterNodes(nodeset, context.getFrameworkProject(), context.getNodesFile());
        } catch (NodeFileParserException e) {
            throw new DispatcherException(e);
        }
        boolean keepgoing = nodeset.isKeepgoing();

        context.getExecutionListener().log(4, "preparing for sequential execution on " + nodes.size() + " nodes");
        final HashSet<String> nodeNames = new HashSet<String>();
        final HashMap<String,Object> failures = new HashMap<String,Object>();
        for (final Object node1 : nodes) {
            final INodeEntry node = (INodeEntry) node1;
            nodeNames.add(node.getNodename());
        }
        FailedNodesListener failedListener = context.getExecutionListener().getFailedNodesListener();
        if (null != failedListener) {
            failedListener.matchedNodes(nodeNames);
        }
        boolean interrupted = false;
        final Thread thread = Thread.currentThread();
        boolean success = true;
        final HashMap<String, StatusResult> resultMap = new HashMap<String, StatusResult>();
        for (final Object node1 : nodes) {
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
                final StatusResult result;
                final ExecutionContext interimcontext = ExecutionContextImpl.createExecutionContextImpl(context, node);
                if (null != item) {
                    result = framework.getExecutionService().interpretCommand(
                        interimcontext, item, node);
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
                            "Failed execution, result was null: " + result);
                    }
                    if (!keepgoing) {
                        break;
                    }
                } else {
                    nodeNames.remove(node.getNodename());
                }
            } catch (Throwable e) {
                success = false;
                failures.put(node.getNodename(), "Error dispatching command to the node: " + e.getMessage());

                if (!keepgoing) {
                    if (failures.size() > 0 && null != failedListener) {
                        //tell listener of failed node list
                        failedListener.nodesFailed(failures);
                    }
                    throw new DispatcherException(
                        "Failed dispatching to node " + node.getNodename() + ": " + e.getMessage(), e, node);
                } else {
                    context.getExecutionListener().log(Constants.ERR_LEVEL,
                        "Failed dispatching to node " + node.getNodename() + ": " + e.getMessage());
                }
            }
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
        return new DispatcherResult() {
            public Map<String, ? extends StatusResult> getResults() {
                return resultMap;
            }

            public boolean isSuccess() {
                return status;
            }

            @Override
            public String toString() {
                return "DispatcherResult{"
                       + "status="
                       + isSuccess()
                       + ", "
                       + "results="
                       + getResults()
                       + "}";
            }
        };
    }
}
