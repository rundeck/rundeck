/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* CommandAction.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 3:27:43 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.core.utils.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Commandline;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * CommandAction executes a simple shell command script
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
class CommandAction extends AbstractAction {
    protected ExecutionListener listener;
    File scriptfile;
    private NodeDispatcher nodeDispatcher;
    protected ExecTaskParameterGenerator parameterGenerator;


    CommandAction(final Framework framework, final IDispatchedScript context, final ExecutionListener listener) {
        super(framework, context);
        this.listener = listener;
        project = new Project();
        framework.configureProject(project);
        nodeDispatcher= ExecTool.createNodeDispatcher();
        parameterGenerator = new ExecTaskParameterGeneratorImpl();
    }

    Project project;

    Project getProject() {
        return project;
    }

    /**
     * Return true if the action should be treated as a simple command
     *
     * @return
     */
    public boolean isCommandAction() {
        return true;
    }


    /**
     * Perform the shell command action.
     * This command performs the node filter based on the Context's NodeSet, configures the logformatter and overrides
     * output streams, then executes {@link com.dtolabs.rundeck.core.cli.ExecTool#executeNodedispatch(org.apache.tools.ant.Project, java.util.Collection, int, boolean, com.dtolabs.rundeck.core.execution.FailedNodesListener, com.dtolabs.rundeck.core.cli.NodeCallableFactory)}
     *
     */
    public void doAction() {
//        if (null != listener && null != listener.getBuildListener()) {
//            project.addBuildListener(listener.getBuildListener());
//            debug("added build listener");
//        }
        final Collection<INodeEntry> c;
        try {
            c = getFramework().filterNodes(getContext().getNodeSet(),getContext().getFrameworkProject());
        } catch (NodeFileParserException e) {
            throw new CoreException("Error parsing node resource file: " + e.getMessage(), e);
        }
        if (0 == c.size()) {
            throw new NodesetEmptyException(getContext().getNodeSet());
        }

        debug("number of nodes to dispatch to: " + c.size() + ", (threadcount="
              + getContext().getNodeSet().getThreadCount() + ")");
        
        final INodeEntry singleNode;
        if (1 == c.size()) {
            singleNode = c.iterator().next();
        } else {
            singleNode = null;
        }
        final String fwkNodeName =
            null != singleNode ? singleNode.getNodename() : getFramework().getFrameworkNodeName();
        final String fwkUser = null != singleNode ? singleNode.extractUserName()
                                                  : getFramework().getAuthenticationMgr().getUserInfoWithoutPrompt()
                                                      .getUsername();

        DefaultNodeDispatcher.configureNodeContextThreadLocalsForProject(project);
        final LogReformatter gen ;
        if (null!=listener && listener.isTerse()) {
            gen=null;
        }else{
            String logformat = ExecTool.DEFAULT_LOG_FORMAT;
            if (null!=listener && null!=listener.getLogFormat()) {
                logformat = listener.getLogFormat();
            }
            gen= new LogReformatter(logformat, new MapGenerator<String, String>() {
                public Map<String, String> getMap() {
                    final HashMap<String, String> contextData = new HashMap<String, String>();
                    //discover node name and username
                    final String thrNode = DefaultNodeDispatcher.getThreadLocalForProject(
                        DefaultNodeDispatcher.NODE_NAME_LOCAL_REF_ID,
                        CommandAction.this.project);
                    if(null!=thrNode){
                        contextData.put("node", thrNode );
                    } else {
                        contextData.put("node",  fwkNodeName );
                    }
                    final String thrUser = DefaultNodeDispatcher.getThreadLocalForProject(
                        DefaultNodeDispatcher.NODE_USER_LOCAL_REF_ID,
                        CommandAction.this.project);
                    if(null!=thrUser){
                        contextData.put("user",  thrUser);
                    } else {
                        contextData.put("user", fwkUser);
                    }
                    contextData.put("command", "dispatch");
                    return contextData;
                }
            });
        }

        //bind System printstreams to the thread
        final ThreadBoundOutputStream threadBoundSysOut = ThreadBoundOutputStream.bindSystemOut();
        final ThreadBoundOutputStream threadBoundSysErr = ThreadBoundOutputStream.bindSystemErr();

        //get outputstream for reformatting destination
        final OutputStream origout = threadBoundSysOut.getThreadStream();
        final OutputStream origerr = threadBoundSysErr.getThreadStream();

        //replace any existing logreformatter
        final FormattedOutputStream outformat;
        if (origout instanceof FormattedOutputStream) {
            final OutputStream origsink = ((FormattedOutputStream) origout).getOriginalSink();
            outformat = new FormattedOutputStream(gen, origsink);
        } else {
            outformat = new FormattedOutputStream(gen, origout);
        }
        outformat.setContext("level", "INFO");

        final FormattedOutputStream errformat;
        if (origerr instanceof FormattedOutputStream) {
            final OutputStream origsink = ((FormattedOutputStream) origerr).getOriginalSink();
            errformat = new FormattedOutputStream(gen, origsink);
        } else {
            errformat = new FormattedOutputStream(gen, origerr);
        }
        errformat.setContext("level", "ERROR");

        //install the OutputStreams for the thread
        threadBoundSysOut.installThreadStream(outformat);
        threadBoundSysErr.installThreadStream(errformat);


        try {
            nodeDispatcher.executeNodedispatch(project,
                c,
                getContext().getNodeSet().getThreadCount(),
                getContext().getNodeSet().isKeepgoing(),
                null != listener ? listener.getFailedNodesListener() : null,
                new NodeCallableFactory() {
                    public Callable createCallable(final INodeEntry node) {
                        try {
                            return new TaskCallable(createCommandProxy(node));
                        } catch (ExecutionException e) {
                            throw new CoreException(e);
                        }
                    }
                }
            );
        } finally {
            threadBoundSysOut.removeThreadStream();
            threadBoundSysErr.removeThreadStream();
        }
    }



    final Task createCommandProxy(final INodeEntry nodeentry) throws ExecutionException {
        final Task task;
        if (null == project) {
            throw new IllegalStateException("Execution project not instantiated");
        }
        if (nodeentry.getNodename().equalsIgnoreCase(getFramework().getFrameworkNodeName())) {
            /**
             * it's local
             */
            verbose("preparing for local execution ...");

            task = createLocalCommandProxy(nodeentry, project);
        } else {
            verbose("preparing for remote execution ...");
            /**
             * its remote
             */

            task = createRemoteCommandProxy(nodeentry, project);
        }
        debug("command proxy instance of " + task.getClass().getName());

        return task;
    }

    /**
     * Create a task to execute the command on the local node.
     *
     * @param nodeentry the node
     * @param project   ant project
     *
     * @return a Task
     */
    protected Task createLocalCommandProxy(final INodeEntry nodeentry, final Project project) throws
        ExecutionException {

        final Map<String, Map<String, String>> dataContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(nodeentry), getContext().getDataContext());
        final String[] newargs = DataContextUtils.replaceDataReferences(getContext().getArgs(), dataContext);

        //Generate exec task parameters without a scriptfile

        return createLocalCommandProxy(nodeentry, project, parameterGenerator.generate(nodeentry, isCommandAction(), null,
            newargs), dataContext);
    }

    /**
     * Create a task to execute the command on the local node, using the specified exec task parameters
     * and data context.
     *
     * @param nodeentry the node
     * @param project   ant project
     *
     * @param taskParameters
     * @param dataContext
     * @return a Task
     */
    protected Task createLocalCommandProxy(final INodeEntry nodeentry, final Project project,
                                           final ExecTaskParameters taskParameters,
                                           final Map<String, Map<String, String>> dataContext) throws
        ExecutionException {
        final Sequential seq = new Sequential();
        seq.setProject(project);
        DefaultNodeDispatcher.addNodeContextTasks(nodeentry, project, seq);

        final ExecTask execTask = new ExecTask();
        execTask.setTaskType("exec");
        execTask.setFailonerror(true);
        execTask.setProject(project);
        final Commandline.Argument arg = execTask.createArg();

        verbose("exectask");

        execTask.setExecutable(taskParameters.getCommandexecutable());
        arg.setLine(taskParameters.getCommandargline());

        //add Env elements to pass environment variables to the exec

        DataContextUtils.addEnvVarsFromContextForExec(execTask, dataContext);

        seq.addTask(execTask);

        //add success report for current node execution, for use by parallel execution failed nodes listener
        DefaultNodeDispatcher.addNodeContextSuccessReport(nodeentry, project, seq);

        return seq;

    }


    /**
     * Create a Task which invokes the command by sending it to a remote node.
     *
     * @param nodeentry the node
     * @param project   the ant project
     *
     * @return the Task
     */
    protected Task createRemoteCommandProxy(final INodeEntry nodeentry, final Project project) throws ExecutionException{
        final String[] args = getContext().getArgs();
        //verify node entry has required values
        if (null == nodeentry.getHostname() || null == nodeentry.extractHostname() ) {
            throw new ExecutionException("Hostname must be set to connect to remote node '" + nodeentry.getNodename() + "'");
        }
        if (null == nodeentry.extractUserName() ) {
            throw new ExecutionException("Username must be set to connect to remote node '" + nodeentry.getNodename() + "'");
        }
        try {
            return createRemoteCommandProxy(nodeentry, args, project, getFramework());
        } catch (SSHTaskBuilder.BuilderException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * Create a Task which invokes the command by sending it to a remote node.
     *
     * @param nodeentry the node
     * @param args      the exact array of cli arguments (including command) to execute on the remote node
     * @param project   the ant project
     * @param framework framework
     *
     * @return the Task
     */
    protected Task createRemoteCommandProxy(final INodeEntry nodeentry, final String[] args, final Project project,
                                            final Framework framework) throws SSHTaskBuilder.BuilderException {
        final Sequential seq = new Sequential();
        seq.setProject(project);
        DefaultNodeDispatcher.addNodeContextTasks(nodeentry, project, seq);
        int timeout = 0;
        /**
         * configure an SSH timeout
         */
        if (framework.getPropertyLookup().hasProperty(Constants.SSH_TIMEOUT_PROP)) {
            final String val = framework.getProperty(Constants.SSH_TIMEOUT_PROP);
            try {
                timeout = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                debug("ssh timeout property '" + Constants.SSH_TIMEOUT_PROP
                      + "' had a non integer value: " + val
                      + " defaulting to: 0 (forever)");
            }
        }
        final Map<String, Map<String, String>> dataContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(nodeentry), getContext().getDataContext());
        //substitute any args values
        final String[] newargs = DataContextUtils.replaceDataReferences(args, dataContext);
        final Task sshexec = SSHTaskBuilder.build(nodeentry, newargs, project, framework, timeout, dataContext);
        seq.addTask(sshexec);
        DefaultNodeDispatcher.addNodeContextSuccessReport(nodeentry, project, seq);
        return seq;

    }



    public void log(final String message) {
        if(null!=listener){
            listener.log(Constants.INFO_LEVEL,message);
        }
    }

    public void error(final String message) {
        if (null != listener) {
            listener.log(Constants.ERR_LEVEL, message);
        }
    }

    public void warn(final String message) {
        if (null != listener) {
            listener.log(Constants.WARN_LEVEL, message);
        }
    }

    public void verbose(final String message) {
        if (null != listener) {
            listener.log(Constants.VERBOSE_LEVEL, message);
        }
    }

    public void debug(final String message) {
        if (null != listener) {
            listener.log(Constants.DEBUG_LEVEL, message);
        }
    }

    public NodeDispatcher getNodeDispatcher() {
        return nodeDispatcher;
    }

    public void setNodeDispatcher(final NodeDispatcher nodeDispatcher) {
        this.nodeDispatcher = nodeDispatcher;
    }

    protected ExecTaskParameterGenerator getParameterGenerator() {
        return parameterGenerator;
    }

    protected void setParameterGenerator(final ExecTaskParameterGenerator parameterGenerator) {
        this.parameterGenerator = parameterGenerator;
    }
}
