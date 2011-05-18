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
* NodeDispatcherImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 1:31:59 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.NodesetFailureException;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionServiceThread;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import com.dtolabs.rundeck.core.tasks.dispatch.NodeExecutionStatusTask;
import com.jcraft.jsch.JSchException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Parallel;
import org.apache.tools.ant.taskdefs.Sequential;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

/**
 * DefaultNodeDispatcher provides an implementation to iterate across a set of nodes and execute tasks.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class DefaultNodeDispatcher implements NodeDispatcher{
    public static final String STATUS_LISTENER_REF_ID = DefaultNodeDispatcher.class.getName() + ":status.listener";
    public static final String NODE_NAME_LOCAL_REF_ID = DefaultNodeDispatcher.class.getName() + ":node.name";
    public static final String NODE_USER_LOCAL_REF_ID = DefaultNodeDispatcher.class.getName() + ":node.user";

    /**
     * Execute a node dispatch request, in serial with parallel threads.
     *
     * @param project        Ant project
     * @param nodes          node set to iterate over
     * @param threadcount    max number of parallel threads
     * @param keepgoing      if true, continue execution even if a node fails
     * @param failedListener listener for results of failed nodes (when keepgoing is true)
     * @param factory        factory to produce executable items given input nodes
     */
    public void executeNodedispatch(final Project project, final Collection<INodeEntry> nodes,
                                    final int threadcount, final boolean keepgoing,
                                    final FailedNodesListener failedListener,
                                    final NodeCallableFactory factory) {
        project.log(
            "executeNodeDispatch: number of nodes to dispatch to: " + nodes.size() + ", (threadcount=" + threadcount
            + ")", Project.MSG_DEBUG);


        if (threadcount > 1 && nodes.size() > 1) {
            project.log(
                "preparing for parallel execution...(keepgoing? " + keepgoing + ", threads: " + threadcount + ")",
                Project.MSG_DEBUG);
            final HashSet<String> nodeNames = new HashSet<String>();
            final NodeDispatchStatusListener listener = new NodeDispatchStatusListener() {
                public void reportSuccess(final String nodename) {
                    synchronized (nodeNames){
                        nodeNames.remove(nodename);
                    }
                }
            };
             if (null == project.getReference(STATUS_LISTENER_REF_ID)) {
                project.addReference(STATUS_LISTENER_REF_ID, listener);
            }
            if(null!=failedListener) {
                failedListener.matchedNodes(nodeNames);
            }
            final Parallel parallelTask = new Parallel();
            parallelTask.setProject(project);
            parallelTask.setThreadCount(threadcount);
            parallelTask.setFailOnAny(!keepgoing);
            for (final Object node1 : nodes) {
                final INodeEntry node = (INodeEntry) node1;
                final Callable tocall;
                nodeNames.add(node.getNodename());
                try {
                    tocall = factory.createCallable(node);
                } catch (Throwable e) {
                    if (e instanceof BuildException && (e.getMessage().contains(
                        "Timeout period exceeded, connection dropped"))) {
                        project.log("Failed execution for node: " + node.getNodename() + ": "
                                    + "Execution Timeout period exceeded (after " + factory.getRemoteTimeout()
                                    + "ms), connection dropped", e,
                            Project.MSG_ERR);
                    } else if (e instanceof BuildException && null != e.getCause()
                               && e.getCause() instanceof JSchException
                               && (e.getCause().getMessage().contains("timeout:"))) {
                        project.log("Failed execution for node: " + node.getNodename() + ": "
                                    + "Connection Timeout (after " + factory.getRemoteTimeout()
                                    + "ms): " + e.getMessage(), e,
                            Project.MSG_ERR);
                    } else {
                        project.log("Failed execution for node: " + node.getNodename() + ": " + e.getMessage(),
                            Project.MSG_ERR);
                    }
                    continue;
                }
                project.log("dispatching to proxy on node: " + node.getNodename(), Project.MSG_DEBUG);
                if(tocall instanceof TaskCallable) {
                    final Task nestedTask = ((TaskCallable) tocall).getTask();
                    nestedTask.setProject(project);
                    parallelTask.addTask(nestedTask);
                }else{
                    final Task callableWrapperTask = createWrappedCallableTask(project, tocall, node);
                    parallelTask.addTask(callableWrapperTask);
                }
            }
            try {
                parallelTask.execute();
            } catch (BuildException e) {
                project.log(e.getMessage(), Project.MSG_ERR);
                if(!keepgoing){
                    throw e;
                }
            }
            //evaluate the failed nodes
            if (nodeNames.size() > 0) {
                if (null != failedListener) {
                    //tell listener of failed node list
                    failedListener.nodesFailed(nodeNames);
                }
                //now fail
                throw new NodesetFailureException(nodeNames);
            } else if (null != failedListener && nodeNames.isEmpty()) {
                failedListener.nodesSucceeded();
            }
        } else {
            project.log("preparing for sequential execution...", Project.MSG_DEBUG);
            final HashSet<String> nodeNames = new HashSet<String>();
            for (final Object node1 : nodes) {
                final INodeEntry node = (INodeEntry) node1;
                nodeNames.add(node.getNodename());
            }
            if (null != failedListener) {
                failedListener.matchedNodes(nodeNames);
            }
            boolean interrupted=false;
            final Thread thread = Thread.currentThread();
            for (final Object node1 : nodes) {
                if (thread.isInterrupted()
                    || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                    interrupted = true;
                    break;
                }
                final INodeEntry node = (INodeEntry) node1;
                project.log("Executing command on node: " + node.getNodename() + ", " + node.toString(),
                    Project.MSG_DEBUG);
                try {
                    final Callable task = factory.createCallable(node);

                    if (thread.isInterrupted()
                        || thread instanceof ExecutionServiceThread && ((ExecutionServiceThread) thread).isAborted()) {
                        interrupted = true;
                        break;
                    }
                    task.call();
                    nodeNames.remove(node.getNodename());
                } catch (Throwable e) {
                    if (e instanceof BuildException && (e.getMessage().contains(
                        "Timeout period exceeded, connection dropped"))) {
                        project.log("Failed execution for node: " + node.getNodename() + ": "
                                    + "Execution Timeout period exceeded (after " + factory.getRemoteTimeout()
                                    + "ms), connection dropped", e,
                            Project.MSG_ERR);
                    } else if (e instanceof BuildException && null != e.getCause()
                               && e.getCause() instanceof JSchException
                               && (e.getCause().getMessage().contains("timeout:"))) {
                        project.log("Failed execution for node: " + node.getNodename() + ": "
                                    + "Connection Timeout (after " + factory.getRemoteTimeout()
                                    + "ms): "+e.getMessage(), e,
                            Project.MSG_ERR);
                    } else {
                        project.log("Failed execution for node: " + node.getNodename() + ": " + e.getMessage(),
                            Project.MSG_ERR);
                    }
                    if (!keepgoing) {
                        if (nodeNames.size() > 0 && null != failedListener) {
                            //tell listener of failed node list
                            failedListener.nodesFailed(nodeNames);
                        }
                        if (e instanceof BuildException) {
                            throw (BuildException) e;
                        } else {
                            throw new CoreException("Error dispatching execution", e);
                        }
                    }
                }
            }
            if (keepgoing && nodeNames.size() > 0) {
                if (null != failedListener) {
                    //tell listener of failed node list
                    failedListener.nodesFailed(nodeNames);
                }
                //now fail
                throw new NodesetFailureException(nodeNames);
            } else if (null != failedListener && nodeNames.isEmpty() && !interrupted) {
                failedListener.nodesSucceeded();
            }
            if (interrupted) {
                throw new CoreException("Node dispatch interrupted");
            }
        }
    }

    /**
     * Add tasks to the Sequential to set threadlocal values for the node name and username
     *
     * @param nodeentry node entry
     * @param project ant Project
     * @param seq Sequential
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
     * Adds InheritableNodeLocal references to the Project for use by the node context tasks
     * @param project the project
     */
    public static  void configureNodeContextThreadLocalsForProject(final Project project) {
        final InheritableThreadLocal<String> localNodeName = new InheritableThreadLocal<String>();
        final InheritableThreadLocal<String> localUserName = new InheritableThreadLocal<String>();
        if (null == project.getReference(DefaultNodeDispatcher.NODE_NAME_LOCAL_REF_ID)) {
            project.addReference(DefaultNodeDispatcher.NODE_NAME_LOCAL_REF_ID, localNodeName);
        }
        if (null == project.getReference(DefaultNodeDispatcher.NODE_USER_LOCAL_REF_ID)) {
            project.addReference(DefaultNodeDispatcher.NODE_USER_LOCAL_REF_ID, localUserName);
        }
    }

    /**
     * Extract the threadlocal stored as a reference in the project, and return the string value or null.
     * @param nodeNameLocalRefId refid for the thread local variable
     * @param project Project
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
     * Return a task configured to set the thread local value for a particular refid
     * @param refid the refid
     * @param value the value to set
     * @return the Task
     */
    private static Task genSetThreadLocalRefValue(final String refid, final String value) {
        final setThreadLocalTask task = new setThreadLocalTask();
        task.setRefid(refid);
        task.setValue(value);
        return task;
    }


    /**
     * Task to set a threadlocal value given a refid.
     * The refid should have been set in the project already, and be an InheritableThreadLocal instance.  The
     * value will be set for the threadlocal variable
     */
    public static class setThreadLocalTask extends Task {
        private String value;
        private String refid;

        @Override
        public void execute() throws BuildException {
            final Object o = getProject().getReference(getRefid());
            if(o instanceof InheritableThreadLocal) {
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

    /**
     * Utility task to log the value of a threadlocal variable given a refid
     */
    public static class echoThreadLocalTask extends Task {
        private String refid;

        @Override
        public void execute() throws BuildException {
            final Object o = getProject().getReference(getRefid());
            if (o instanceof InheritableThreadLocal) {
                InheritableThreadLocal<String> local = (InheritableThreadLocal<String>) o;
                final Object o1 = local.get();
                getProject().log("threadLocal(" + getRefid() + "): " + o1 + "/" + Thread.currentThread().toString());
            }
        }

        public String getRefid() {
            return refid;
        }

        public void setRefid(final String refid) {
            this.refid = refid;
        }
    }

    /**
     * Return a Sequential Task that sets threadlocal values for Node context information, then executes the Callable via a CallableWrapperTask, then
     * reports node success via NodeExecutionStatusTask.
     * @param project the project
     * @param tocall the Callable
     * @param node the target node
     * @return Sequential Task instance
     */
    private Task createWrappedCallableTask(final Project project, final Callable tocall, final INodeEntry node) {
        final Sequential seq = new Sequential();
        seq.setProject(project);
        addNodeContextTasks(node, project, seq);
        final CallableWrapperTask callableWrapperTask = new CallableWrapperTask(tocall);
        callableWrapperTask.setProject(project);
        seq.addTask(callableWrapperTask);
        DefaultNodeDispatcher.addNodeContextSuccessReport(node, project, seq);
        return seq;
    }


    /**
     * Add internal success notification to inform parallel node dispatcher that execution was successful on this node.
     *
     * @param nodeentry the node
     * @param project the project
     * @param seq the Sequential task
     */
    public static void addNodeContextSuccessReport(final INodeEntry nodeentry, final Project project,
                                                   final Sequential seq) {
        final NodeExecutionStatusTask status = new NodeExecutionStatusTask();
        status.setProject(project);
        status.setNodeName(nodeentry.getNodename());
        status.setRefId(DefaultNodeDispatcher.STATUS_LISTENER_REF_ID);
        status.setFailOnError(false);

        seq.addTask(status);
    }

}
