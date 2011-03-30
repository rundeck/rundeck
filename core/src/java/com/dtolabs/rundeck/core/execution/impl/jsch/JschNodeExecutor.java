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
* JschNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:46 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.ExecTool;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionListenerBuildLogger;
import com.dtolabs.rundeck.core.execution.dispatch.ParallelNodeDispatcher;
import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.tasks.net.ExtSSHExec;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.core.utils.FormattedOutputStream;
import com.dtolabs.rundeck.core.utils.LogReformatter;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Sequential;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * JschNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JschNodeExecutor implements NodeExecutor {
    public static final String SERVICE_PROVIDER_TYPE = "jsch-ssh";
    private Framework framework;

    public JschNodeExecutor(Framework framework) {
        this.framework = framework;
    }

    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, final INodeEntry node) throws
        ExecutionException {
        if (null == node.getHostname() || null == node.extractHostname()) {
            throw new ExecutionException(
                "Hostname must be set to connect to remote node '" + node.getNodename() + "'");
        }
        if (null == node.extractUserName()) {
            throw new ExecutionException(
                "Username must be set to connect to remote node '" + node.getNodename() + "'");
        }

        
        final ExecutionListener listener = context.getExecutionListener();
        final Project project = new Project();
        AntSupport.addAntBuildListener(listener, project);
        boolean success = false;
        final ExtSSHExec sshexec;
        try {
            //perform jsch sssh command
            sshexec = buildSSHTask(context, node, command, project, framework);
            final Task taskSequence = createRemoteTaskSequence(node, project, sshexec);
//            project.log("JschNodeExecutor log",Project.MSG_ERR);
//            System.err.println("JschNodeExecutor sys.err");
//            System.out.println("JschNodeExecutor sys.out");
            taskSequence.execute();
            success = true;
        } catch (SSHTaskBuilder.BuilderException e) {
            throw new ExecutionException(e);
        }
        final int resultCode = sshexec.getExitStatus();
        final boolean status = success;

        return new NodeExecutorResult() {
            public int getResultCode() {
                return resultCode;
            }

            public boolean isSuccess() {
                return status;
            }

            @Override
            public String toString() {
                return "[jsch-ssh] result was " + (isSuccess() ? "success" : "failure") + ", resultcode: "
                       + getResultCode();
            }
        };
    }



    /**
     * Create a Task which invokes the command by sending it to a remote node.
     *
     * @param nodeentry the node
     * @param project   the ant project
     * @param sshexec
     *
     * @return the Task
     */
    protected Task createRemoteTaskSequence(final INodeEntry nodeentry,
                                            final Project project, final Task sshexec) {
        final Sequential seq = new Sequential();
        seq.setProject(project);
        ParallelNodeDispatcher.addNodeContextTasks(nodeentry, project, seq);
        seq.addTask(sshexec);
        ParallelNodeDispatcher.addNodeContextSuccessReport(nodeentry, project, seq);
        return seq;

    }

    private ExtSSHExec buildSSHTask(ExecutionContext context, INodeEntry nodeentry, String[] args, Project project,
                                    Framework framework) throws SSHTaskBuilder.BuilderException {
        //XXX:TODO use node attributes to specify ssh key/timeout
        int timeout = 0;
        /**
         * configure an SSH timeout
         */
        if (framework.getPropertyLookup().hasProperty(Constants.SSH_TIMEOUT_PROP)) {
            final String val = framework.getProperty(Constants.SSH_TIMEOUT_PROP);
            try {
                timeout = Integer.parseInt(val);
            } catch (NumberFormatException e) {
//                debug("ssh timeout property '" + Constants.SSH_TIMEOUT_PROP
//                      + "' had a non integer value: " + val
//                      + " defaulting to: 0 (forever)");
            }
        }
        final Map<String, Map<String, String>> dataContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(nodeentry), context.getDataContext());
        //substitute any args values
        final String[] newargs = DataContextUtils.replaceDataReferences(args, dataContext);
        return SSHTaskBuilder.build(nodeentry, newargs, project, framework, timeout, dataContext);
    }


}
