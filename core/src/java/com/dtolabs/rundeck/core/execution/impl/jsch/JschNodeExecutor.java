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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.tasks.net.ExtSSHExec;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.jcraft.jsch.JSchException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.text.MessageFormat;

/**
 * JschNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JschNodeExecutor implements NodeExecutor {
    public static final String SERVICE_PROVIDER_TYPE = "jsch-ssh";
    public static final String FWK_PROP_AUTH_CANCEL_MSG = "framework.messages.error.ssh.authcancel";
    public static final String FWK_PROP_AUTH_CANCEL_MSG_DEFAULT =
        "Authentication failure connecting to node: \"{0}\". Make sure your resource definitions and credentials are up to date.";
    private Framework framework;

    public JschNodeExecutor(final Framework framework) {
        this.framework = framework;
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node) throws
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
        final int timeout = getFrameworkSSHTimeout(framework);
        //perform jsch sssh command
        try {
            sshexec = buildSSHTask(context, node, command, project, framework);
        } catch (SSHTaskBuilder.BuilderException e) {
            throw new ExecutionException(e);
        }
        String errormsg = null;
        try {
            sshexec.execute();
            success = true;
        } catch (BuildException e) {
            if ((e.getMessage().contains("Timeout period exceeded, connection dropped"))) {
                errormsg =
                    "Failed execution for node: " + node.getNodename() + ": Execution Timeout period exceeded (after "
                    + timeout + "ms), connection dropped";
            } else if (null != e.getCause() && e.getCause() instanceof JSchException && (e.getCause().getMessage()
                .contains("timeout:"))) {
                errormsg = "Failed execution for node: " + node.getNodename() + ": Connection Timeout (after " + timeout
                           + "ms): " + e.getMessage();
            } else if (null != e.getCause() && e.getCause() instanceof JSchException && e.getCause().getMessage()
                .contains("Auth cancel")) {
                String msgformat = FWK_PROP_AUTH_CANCEL_MSG_DEFAULT;
                if (framework.getPropertyLookup().hasProperty(FWK_PROP_AUTH_CANCEL_MSG)) {
                    msgformat = framework.getProperty(FWK_PROP_AUTH_CANCEL_MSG);
                }
                errormsg = MessageFormat.format(msgformat, node.getNodename(),
                    e.getMessage());
            } else {
                errormsg = e.getMessage();
            }
            context.getExecutionListener().log(0, errormsg);
        }
        final int resultCode = sshexec.getExitStatus();
        final boolean status = success;
        final String resultmsg = null != errormsg ? errormsg : null;

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
                       + getResultCode()+(null!=resultmsg?": "+resultmsg:"");

            }
        };
    }


    private ExtSSHExec buildSSHTask(final ExecutionContext context, final INodeEntry nodeentry, final String[] args,
                                    final Project project, final Framework framework) throws
        SSHTaskBuilder.BuilderException {

        //XXX:TODO use node attributes to specify ssh key/timeout

        /**
         * configure an SSH timeout
         */
        final int timeout = getFrameworkSSHTimeout(framework);
        return SSHTaskBuilder.build(nodeentry, args, project, framework, timeout, context.getDataContext());
    }

    static int getFrameworkSSHTimeout(final Framework framework) {
        int timeout=0;
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
        return timeout;
    }


}
