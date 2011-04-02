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
* JschScpFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.authentication.INodeAuthResolutionStrategy;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;

import java.io.*;

/**
 * JschScpFileCopier is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JschScpFileCopier extends BaseFileCopier implements FileCopier {
    public static final String SERVICE_PROVIDER_TYPE = "jsch-scp";

    private Framework framework;

    public JschScpFileCopier(Framework framework) {
        this.framework = framework;
    }

    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException {

        return copyFile(context, null, input, null, node);
    }

    public String copyFile(final ExecutionContext context, File scriptfile, INodeEntry node) throws
        FileCopierException {
        return copyFile(context, scriptfile, null, null, node);
    }

    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws
        FileCopierException {

        return copyFile(context, null, null, script, node);
    }


    private String copyFile(final ExecutionContext context, File scriptfile, InputStream input, String script,
                            INodeEntry node) throws FileCopierException {
        Project project = new Project();
        final Sequential seq = new Sequential();
        seq.setProject(project);

        final String remotefile = generateRemoteFilepathForNode(node, (null != scriptfile ? scriptfile.getName()
                                                                                          : "dispatch-script"));
        //write the temp file and replace tokens in the script with values from the dataContext
        final File localTempfile = writeScriptTempFile(context, scriptfile, input, script, node);


//        logger.debug("temp file for node " + node.getNodename() + ": " + temp.getAbsolutePath() + ", datacontext: " + dataContext);
        final Task scp = createScp(context, node, project, remotefile, localTempfile);

        /**
         * Copy the file over
         */
        seq.addTask(createEchoVerbose("copying scriptfile: '" + localTempfile.getAbsolutePath()
                                      + "' to: '" + node.getNodename() + ":" + remotefile + "'", project));
        seq.addTask(scp);

        seq.execute();
        if (!localTempfile.delete()) {
            context.getExecutionListener().log(Constants.WARN_LEVEL,
                "Unable to remove local temp file: " + localTempfile.getAbsolutePath());
        }
        return remotefile;
    }


    /**
     * Create Scp task to copy the scriptfile
     *
     * @param nodeentry  node
     * @param project    project
     * @param remotepath path
     * @param sourceFile
     *
     * @return Scp object
     */
    protected Scp createScp(final ExecutionContext context, final INodeEntry nodeentry, final Project project,
                            final String remotepath,
                            final File sourceFile) {
        final INodeAuthResolutionStrategy nodeAuth = framework.getNodeAuthResolutionStrategy();

        final Scp scp = new Scp();
        scp.setFailonerror(true);
        scp.setTrust(true); // set this true to avoid  "reject HostKey" errors

        scp.setProject(project);

        scp.setHost(nodeentry.extractHostname());
        scp.setUsername(nodeentry.extractUserName());

        // If the node entry contains a non-default port, configure the connection to use it.
        if (nodeentry.containsPort()) {
            final int portNum;
            try {
                portNum = Integer.parseInt(nodeentry.extractPort());
            } catch (NumberFormatException e) {
                throw new CoreException("extracted port value was not parseable as an integer: "
                                        + nodeentry.extractPort(), e);
            }
            scp.setPort(portNum);
        }

        if (nodeAuth.isKeyBasedAuthentication(nodeentry)) {
            /**
             * Configure keybased authentication
             */
            final String sshKeypath = framework.getProperty(Constants.SSH_KEYPATH_PROP);
            final boolean keyFileExists = null != sshKeypath && !"".equals(sshKeypath) && new File(sshKeypath).exists();
            if (!keyFileExists) {
                throw new CoreException("SSH Keyfile, " + sshKeypath + ", does not exist and is needed: " + sshKeypath);
            }
            scp.setKeyfile(sshKeypath);

        } else if (nodeAuth.isPasswordBasedAuthentication(nodeentry)) {
            /**
             * Configure password based authentication
             */
            final String password = nodeAuth.fetchPassword(nodeentry);
            if (null == password) {
                throw new CoreException("Null password resulted from fetched for node: "
                                        + nodeentry.getNodename());
            }
            scp.setPassword(password);

        } else {

            throw new CoreException("Unknown node authentication configuration for node: " + nodeentry.getNodename());
        }
        //XXX:TODO use node attributes to specify ssh key/timeout
        /**
         * Set the local and remote file paths
         */
        scp.setLocalFile(sourceFile.getAbsolutePath());
        final String sshUriPrefix = nodeentry.extractUserName() + "@" + nodeentry.extractHostname() + ":";
        scp.setRemoteTofile(sshUriPrefix + remotepath);

        scp.setPassphrase(""); // set empty otherwise password will be required
        scp.setVerbose(context.getLoglevel() >= Project.MSG_VERBOSE);
        return scp;
    }


    private Echo createEcho(final String message, final Project project, final String logLevel) {
        final Echo echo = new Echo();
        echo.setProject(project);
        final Echo.EchoLevel level = new Echo.EchoLevel();
        level.setValue(logLevel);
        echo.setLevel(level);
        echo.setMessage(message);
        return echo;
    }

    private Echo createEchoVerbose(final String message, final Project project) {
        return createEcho(message, project, "debug");
    }
}
