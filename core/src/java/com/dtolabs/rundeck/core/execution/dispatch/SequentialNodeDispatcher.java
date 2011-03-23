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

import com.dtolabs.rundeck.core.NodesetFailureException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.ExecutionServiceThread;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
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

    public DispatcherResult dispatch(ExecutionContext context, CommandInterpreter interpreter, ExecutionItem item) throws
        DispatcherException {
        final NodeSet nodeset = context.getNodeSet();
        Collection<INodeEntry> nodes = null;
        try {
            nodes = framework.filterNodes(nodeset, context.getFrameworkProject());
        } catch (NodeFileParserException e) {
            throw new DispatcherException(e);
        }
        boolean keepgoing = nodeset.isKeepgoing();

//        project.log("preparing for sequential execution...", Project.MSG_DEBUG);
        final HashSet<String> nodeNames = new HashSet<String>();
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
        boolean success=true;
        final HashMap<String, InterpreterResult> resultMap=new HashMap<String, InterpreterResult>();
        for (final Object node1 : nodes) {
            if (thread.isInterrupted()
                || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                interrupted = true;
                break;
            }
            final INodeEntry node = (INodeEntry) node1;
//                project.log("Executing command on node: " + node.getNodename() + ", " + node.toString(), Project.MSG_DEBUG);
            try {
//                final Callable task = factory.createCallable(node);

                if (thread.isInterrupted()
                    || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                    interrupted = true;
                    break;
                }
//                task.call();
                final InterpreterResult interpreterResult = interpreter.interpretCommand(context, item, node);
                if(null!=interpreterResult){
                    resultMap.put(node.getNodename(), interpreterResult);
                }
                if (null == interpreterResult || !interpreterResult.isSuccess()) {
                    success = false;
                    if (!keepgoing) {
                        break;
                    }
                } else {
                    nodeNames.remove(node.getNodename());
                }
            } catch (Throwable e) {
                success=false;
                if (!keepgoing) {
                    if (nodeNames.size() > 0 && null != failedListener) {
                        //tell listener of failed node list
                        failedListener.nodesFailed(nodeNames);
                    }
//                    if (e instanceof BuildException) {
//                        throw (BuildException) e;
//                    } else {
                    throw new DispatcherException("Error dispatching execution", e);
//                    }
                }else{
                    //TODO: need to report failure of node
//                    project.log("Failed execution for node: " + node.getNodename() + ": " + e.getMessage(), Project.MSG_ERR);
                }
            }
        }
        if (keepgoing && nodeNames.size() > 0) {
            if (null != failedListener) {
                //tell listener of failed node list
                failedListener.nodesFailed(nodeNames);
            }
            //now fail
            //XXX: needs to change from exception
            throw new NodesetFailureException(nodeNames);
        } else if (null != failedListener && nodeNames.isEmpty() && !interrupted) {
            failedListener.nodesSucceeded();
        }
        if (interrupted) {
            throw new DispatcherException("Node dispatch interrupted");
        }

        final boolean status=success;
        return new DispatcherResult() {
            public Map<String, InterpreterResult> getResults() {
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
