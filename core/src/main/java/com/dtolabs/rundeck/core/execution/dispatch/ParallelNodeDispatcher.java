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
* MultiNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.cli.CallableWrapperTask;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.Sequential;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * MultiNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ParallelNodeDispatcher implements NodeDispatcher {
    public static final String STATUS_LISTENER_REF_ID = ParallelNodeDispatcher.class.getName() + ":status.listener";
    public static final String NODE_NAME_LOCAL_REF_ID = ParallelNodeDispatcher.class.getName() + ":node.name";
    public static final String NODE_USER_LOCAL_REF_ID = ParallelNodeDispatcher.class.getName() + ":node.user";

    private Framework framework;

    public ParallelNodeDispatcher(Framework framework) {
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
        boolean keepgoing = context.isKeepgoing();

        final HashSet<String> nodeNames = new HashSet<String>();
        FailedNodesListener failedListener = context.getExecutionListener().getFailedNodesListener();

        Project project = new Project();

        context.getExecutionListener().log(3,
            "preparing for parallel execution...(keepgoing? " + keepgoing + ", threads: "
            + context.getThreadCount()
            + ")");
        configureNodeContextThreadLocalsForProject(project);
        final Parallel parallelTask = new Parallel();
        parallelTask.setProject(project);
        parallelTask.setThreadCount(context.getThreadCount());
        parallelTask.setFailOnAny(!keepgoing);
        boolean success = false;
        final HashMap<String, NodeStepResult> resultMap = new HashMap<String, NodeStepResult>();
        final HashMap<String, NodeStepResult> failureMap = new HashMap<String, NodeStepResult>();
        final Collection<INodeEntry> nodes1 = nodes.getNodes();
        //reorder based on configured rank property and order
        final String rankProperty = null != context.getNodeRankAttribute() ? context.getNodeRankAttribute() : "nodename";
        final boolean rankAscending = context.isNodeRankOrderAscending();
        final INodeEntryComparator comparator = new INodeEntryComparator(rankProperty);
        final TreeSet<INodeEntry> orderedNodes = new TreeSet<INodeEntry>(
            rankAscending ? comparator : Collections.reverseOrder(comparator));

        orderedNodes.addAll(nodes1);
        for (final INodeEntry node: orderedNodes) {
            final Callable tocall;
            if (null != item) {
                tocall = execItemCallable(context, item, resultMap, node, failureMap);
            } else {
                tocall = dispatchableCallable(context, toDispatch, resultMap, node, failureMap);
            }
            nodeNames.add(node.getNodename());
            context.getExecutionListener().log(3, "Create task for node: " + node.getNodename());
            final CallableWrapperTask callableWrapperTask1 = new CallableWrapperTask(tocall);
            callableWrapperTask1.setProject(project);
            parallelTask.addTask(callableWrapperTask1);
        }
        if (null != failedListener) {
            failedListener.matchedNodes(nodeNames);
        }
        context.getExecutionListener().log(3, "parallel dispatch to nodes: " + nodeNames);
        BuildException buildException;
        try {
            parallelTask.execute();
            success = true;
        } catch (BuildException e) {
            buildException=e;
            if(e.getCause() !=null && e.getCause() instanceof DispatchFailure) {
                DispatchFailure df = (DispatchFailure) e.getCause();
                //parallel step failed
                context.getExecutionListener().log(3, "Dispatch failed on node: " +df.getNode());
            }else{
                context.getExecutionListener().log(0, e.getMessage());
                if (!keepgoing) {
                    throw new DispatcherException(e);
                }
            }
        }
        //evaluate the failed nodes
        if (failureMap.size() > 0) {
            if (null != failedListener) {
                //tell listener of failed node list
                //extract status results
                failedListener.nodesFailed(failureMap);
            }
            return new DispatcherResultImpl(failureMap, false);
        } else if (null != failedListener && nodeNames.isEmpty()) {
            failedListener.nodesSucceeded();
        }

        final boolean status = success;

        return new DispatcherResultImpl(resultMap, status, "Parallel dispatch: (" + status + ") " + resultMap);
    }
    private static class DispatchFailure extends Exception{
        private String node;

        private DispatchFailure(String node) {
            super("Dispatch failed on node: " + node);
            this.node = node;
        }

        public String getNode() {
            return node;
        }
    }
    private Callable dispatchableCallable(final ExecutionContext context, final Dispatchable toDispatch,
                                          final HashMap<String, NodeStepResult> resultMap, final INodeEntry node,
                                          final Map<String, NodeStepResult> failureMap) {
        return new Callable() {
            public Object call() throws Exception {
                final NodeStepResult dispatch = toDispatch.dispatch(context, node);
                resultMap.put(node.getNodename(), dispatch);
                if (!dispatch.isSuccess()) {
                    failureMap.put(node.getNodename(), dispatch);
                    throw new DispatchFailure(node.getNodename());
                }
                return dispatch;
            }
        };
    }

    static class ExecNodeStepCallable implements Callable<NodeStepResult>{
        final StepExecutionContext context;
        final NodeStepExecutionItem item;
        final HashMap<String, NodeStepResult> resultMap;
        final INodeEntry node;
        final Map<String, NodeStepResult> failureMap;
        final Framework framework;

        ExecNodeStepCallable(StepExecutionContext context,
                             NodeStepExecutionItem item,
                             HashMap<String, NodeStepResult> resultMap,
                             INodeEntry node,
                             Map<String, NodeStepResult> failureMap,
                             Framework framework) {
            this.context = context;
            this.item = item;
            this.resultMap = resultMap;
            this.node = node;
            this.failureMap = failureMap;
            this.framework = framework;
        }

        @Override
        public NodeStepResult call() {
            try {
                final NodeStepResult interpreterResult = framework.getExecutionService().executeNodeStep(
                    context, item, node);
                if (!interpreterResult.isSuccess()) {
                    failureMap.put(node.getNodename(), interpreterResult);
                }
                resultMap.put(node.getNodename(), interpreterResult);
                return interpreterResult;
            } catch (NodeStepException e) {
                NodeStepResultImpl result = new NodeStepResultImpl(e,
                                                                   e.getFailureReason(),
                                                                   e.getMessage(),
                                                                   node);
                failureMap.put(node.getNodename(), result);
                return result;
            }
        }
    }
    private ExecNodeStepCallable execItemCallable(final StepExecutionContext context, final NodeStepExecutionItem item,
                                      final HashMap<String, NodeStepResult> resultMap, final INodeEntry node,
                                      final Map<String, NodeStepResult> failureMap) {
        return new ExecNodeStepCallable(context, item, resultMap, node, failureMap, framework);
    }

    /**
     * Adds InheritableNodeLocal references to the Project for use by the node context tasks
     *
     * @param project the project
     */
    public static void configureNodeContextThreadLocalsForProject(final Project project) {
        final InheritableThreadLocal<String> localNodeName = new InheritableThreadLocal<String>();
        final InheritableThreadLocal<String> localUserName = new InheritableThreadLocal<String>();
        if (null == project.getReference(NODE_NAME_LOCAL_REF_ID)) {
            project.addReference(NODE_NAME_LOCAL_REF_ID, localNodeName);
        }
        if (null == project.getReference(NODE_USER_LOCAL_REF_ID)) {
            project.addReference(NODE_USER_LOCAL_REF_ID, localUserName);
        }
    }

    /**
     * Extract the threadlocal stored as a reference in the project, and return the string value or null.
     *
     * @param nodeNameLocalRefId refid for the thread local variable
     * @param project            Project
     *
     * @return value of the variable, or null if it is not found or the refid doesn't refer to a valid thread local
     */
    public static String getThreadLocalForProject(final String nodeNameLocalRefId, final Project project) {
        final Object o = project.getReference(nodeNameLocalRefId);
        String thrNode = null;
        if (null != o && o instanceof InheritableThreadLocal) {
            InheritableThreadLocal<String> local = (InheritableThreadLocal<String>) o;
            thrNode = local.get();
        }
        return thrNode;
    }

    /**
     * Add tasks to the Sequential to set threadlocal values for the node name and username
     *
     * @param nodeentry node entry
     * @param project   ant Project
     * @param seq       Sequential
     */
    public static void addNodeContextTasks(final INodeEntry nodeentry, final Project project,
                                           final Sequential seq) {
        //set thread local node name
        final Task nodenamelocal = genSetThreadLocalRefValue(NODE_NAME_LOCAL_REF_ID, nodeentry.getNodename());
        nodenamelocal.setProject(project);
        seq.addTask(nodenamelocal);

        if (null != nodeentry.extractUserName()) {
            //set thread local username
            final Task userlocal = genSetThreadLocalRefValue(NODE_USER_LOCAL_REF_ID, nodeentry.extractUserName());
            userlocal.setProject(project);
            seq.addTask(userlocal);
        }

    }

    /**
     * Return a task configured to set the thread local value for a particular refid
     *
     * @param refid the refid
     * @param value the value to set
     *
     * @return the Task
     */
    private static Task genSetThreadLocalRefValue(final String refid, final String value) {
        final SetThreadLocalTask task = new SetThreadLocalTask();
        task.setRefid(refid);
        task.setValue(value);
        return task;
    }


    /**
     * Task to set a threadlocal value given a refid. The refid should have been set in the project already, and be an
     * InheritableThreadLocal instance.  The value will be set for the threadlocal variable
     */
    public static class SetThreadLocalTask extends Task {
        private String value;
        private String refid;

        @Override
        public void execute() throws BuildException {
            final Object o = getProject().getReference(getRefid());
            if (o instanceof InheritableThreadLocal) {
                final InheritableThreadLocal<String> local = (InheritableThreadLocal<String>) o;
                local.set(getValue());
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getRefid() {
            return refid;
        }

        public void setRefid(final String refid) {
            this.refid = refid;
        }
    }


}
