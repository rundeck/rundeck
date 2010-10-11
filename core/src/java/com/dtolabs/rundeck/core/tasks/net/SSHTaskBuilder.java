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
* SSHTaskFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 11:33:34 AM
* $Id$
*/
package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.authentication.INodeAuthResolutionStrategy;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.util.Map;

/**
 * SSHTaskFactory constructs a ExtSSHExec task
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SSHTaskBuilder {
    public static final String SSH_KEYPATH_PROP = Constants.SSH_KEYPATH_PROP;
    public static final String SSH_USER_PROP = Constants.SSH_USER_PROP;
    public static final String SSH_TIMEOUT_PROP = Constants.SSH_TIMEOUT_PROP;

    /**
     * Build a Task that performs SSH command
     * @param nodeentry target node
     * @param args arguments
     * @param project ant project
     * @param framework framework
     * @param timeout connection timeout
     * @param dataContext
     * @return task
     */
    public static Task build(final INodeEntry nodeentry, final String[] args, final Project project,
                             final Framework framework, final long timeout, final Map<String, Map<String, String>> dataContext) throws
        BuilderException {
        final INodeAuthResolutionStrategy nodeAuth = framework.getNodeAuthResolutionStrategy();


        final ExtSSHExec sshexecTask = new ExtSSHExec();
        sshexecTask.setFailonerror(true);
        sshexecTask.setTrust(true); // set this true to avoid  "reject HostKey" errors
        sshexecTask.setProject(project);
        sshexecTask.setHost(nodeentry.extractHostname());
        final String commandString = CLIUtils.generateArgline(null, args);
        sshexecTask.setCommand(commandString);
        sshexecTask.setVerbose(false);
        sshexecTask.setTimeout(timeout);
        sshexecTask.setOutputproperty("sshexec.output");

        // If the node entry contains a non-default port, configure the connection to use it.
        if (nodeentry.containsPort()) {
            final int portNum;
            try {
                portNum = Integer.parseInt(nodeentry.extractPort());
            } catch (NumberFormatException e) {
                throw new CoreException("extracted port value was not parseable as an integer: "
                                       + nodeentry.extractPort(), e);
            }
            sshexecTask.setPort(portNum);
        }

        /**
         * Set the username
         */
        if ((null == nodeentry.getUsername() && !nodeentry.containsUserName())
            && (project.getProperties().containsKey(SSH_USER_PROP)
                && !"".equals(project.getProperty(SSH_USER_PROP).trim()))) {
            /**
             * To keep backwards compatibility (for now), let the executing project
             * take precedence for defining the remote user. This is considered deprecated behavior.
             */
            sshexecTask.setUsername(project.getProperty(SSH_USER_PROP).trim());
            project.log("Deprecated: Defaulted SSH username to " + project.getProperty(SSH_USER_PROP)
                        + " when executing " + project.getName() + ". " +
                        "Manage the node information in nodes.properties for node: " + nodeentry.getNodename());
        } else {
            sshexecTask.setUsername(nodeentry.extractUserName());
        }

        if (nodeAuth.isKeyBasedAuthentication(nodeentry)) {
            /**
             * Configure keybased authentication
             */
            String sshKeypath = framework.getProperty(Constants.SSH_KEYPATH_PROP);
            boolean keyFileExists = null != sshKeypath && !"".equals(sshKeypath) && new File(sshKeypath).exists();
            if (!keyFileExists) {
                throw new BuilderException(
                    "SSH Keyfile, " + sshKeypath + ", does not exist and is needed: " + sshKeypath);
            }
            sshexecTask.setKeyfile(sshKeypath);

        } else if (nodeAuth.isPasswordBasedAuthentication(nodeentry)) {
            /**
             * Configure password based authentication
             */
            String password = nodeAuth.fetchPassword(nodeentry);
            if (null == password) {
                throw new BuilderException("Null password resulted from fetched for node: "
                                         + nodeentry.getNodename());
            }
            sshexecTask.setPassword(password);

        } else {

            throw new CoreException(
                "Unknown node authentication configuration for node: " + nodeentry.getNodename());
        }

        DataContextUtils.addEnvVars(sshexecTask, dataContext);
        return sshexecTask;

    }

    public static class BuilderException extends Exception{
        public BuilderException() {
        }

        public BuilderException(String s) {
            super(s);
        }

        public BuilderException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public BuilderException(Throwable throwable) {
            super(throwable);
        }
    }

}
