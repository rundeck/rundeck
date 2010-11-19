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
* ScriptfileAction.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 3:27:27 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.core.authentication.INodeAuthResolutionStrategy;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * ScriptfileAction is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */

/**
 * Action to execute a scriptfile
 */
class ScriptfileAction extends CommandAction {

    /**
     * Create the ScriptfileAction
     *
     * @param framework framework
     * @param context   context
     * @param listener  listener
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException if an IO error occurs
     */
    ScriptfileAction(final Framework framework, final IDispatchedScript context,
                     final ExecutionListener listener) throws ExecutionException {
        super(framework, context, listener);
        if (null != context.getServerScriptFilePath()) {
            final File file = new File(context.getServerScriptFilePath());
            if (!file.exists()) {
                throw new CoreException("Could not execute specified script. file not found: " + context
                    .getServerScriptFilePath());
            }
            this.scriptfile = file;
        }
    }


    /**
     * Copy the embedded script content, or the script source stream, into a temp file, and replace embedded tokens with
     * values from the dataContext. Marks the file as executable and delete-on-exit.
     *
     * @return temp file path
     *
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException
     *          if an IO problem occurs
     */
    protected File writeScriptTempFile(final File original, final Map<String, Map<String, String>> dataContext) throws
        ExecutionException {
        File tempfile = null;
        try {
            if (null != original) {
                tempfile = DataContextUtils.replaceTokensInFile(original, dataContext, getFramework());
            } else if (null != getContext().getScript()) {
                tempfile = DataContextUtils.replaceTokensInScript(getContext().getScript(),
                    dataContext, getFramework());
            } else if (null != getContext().getScriptAsStream()) {
                tempfile = DataContextUtils.replaceTokensInStream(getContext().getScriptAsStream(),
                    dataContext, getFramework());
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ExecutionException("error writing script to tempfile: " + e.getMessage(), e);
        }
        debug("Wrote script content to file: " + tempfile);
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            warn(
                "Failed to set execute permissions on tempfile, execution may fail: " + tempfile.getAbsolutePath());
        }
        return tempfile;
    }

    /**
     * Return true if the action should be treated as a simple command
     *
     * @return false
     */
    public boolean isCommandAction() {
        return false;
    }

    /**
     * Overrides {@link CommandAction} to first scp the file to the remote host.
     */
    protected Task createRemoteCommandProxy(final INodeEntry nodeentry,
                                            final Project project) throws ExecutionException {

        final Sequential seq = new Sequential();
        seq.setProject(project);
        final long time = System.currentTimeMillis();
        final String prefix = time + "-" + nodeentry.getNodename();
        final String remoteFilename =
            null == scriptfile ? prefix + "-dispatch-script" : prefix + "-" + scriptfile.getName();
        /**
        * Define the remote directory where the script file
        * will be remotely copied.
        */
        final String remotefile;
        final String[] remotecmd;
        final String[] args = getContext().getArgs();
        if ("unix".equalsIgnoreCase(nodeentry.getOsFamily().trim())) {
            final String temp;
            temp = "/tmp/";

            remotefile = temp + remoteFilename;

        } else if ("cygwin".equalsIgnoreCase(nodeentry.getOsFamily().trim())) {
            final String temp;
                temp = "/tmp/";
            remotefile = temp + remoteFilename;
        } else if ("windows".equalsIgnoreCase(nodeentry.getOsFamily().trim()) || "cygwin".equalsIgnoreCase(
            nodeentry.getOsFamily().trim())) {
            String tempfs = "C:/WINDOWS/TEMP/";
            remotefile = tempfs + remoteFilename + (remoteFilename.endsWith(".bat") ? "" : ".bat");
        } else {
            throw new CoreException("Could not create a remote command proxy for node: "
                                   + nodeentry.getNodename() +
                                   " Unrecognized os family: " + nodeentry.getOsFamily());
        }

        //create new dataContext with the node data, and write the script (file,content or strea) to a temp file
        //using the dataContext for substitution.
        final Map<String, Map<String, String>> origContext = getContext().getDataContext();
        final Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("node",
            DataContextUtils.nodeData(nodeentry), origContext);

        //write the temp file and replace tokens in the script with values from the dataContext
        final File temp = writeScriptTempFile(scriptfile, dataContext);

        logger.debug(
            "temp file for node " + nodeentry.getNodename() + ": " + temp.getAbsolutePath() + ", datacontext: "
            + dataContext);
        final Task scp = createScp(nodeentry, project, remotefile, temp);

        remotecmd = new String[(null != args ? args.length : 0) + 1];
        remotecmd[0] = remotefile;
        if (null != args && args.length > 0) {
            System.arraycopy(args, 0, remotecmd, 1, args.length);
        }
        /**
         * Copy the file over
         */
        seq.addTask(createEchoVerbose("copying scriptfile: '" + temp.getAbsolutePath()
                                      + "' to: '" + nodeentry.getNodename() + ":" + remotefile + "'", project));
        seq.addTask(scp);
        /**
         * TODO: Avoid this horrific hack. Discover how to get SCP task to preserve the execute bit.
         */
        if ("unix".equalsIgnoreCase(nodeentry.getOsFamily()) || "cygwin".equalsIgnoreCase(nodeentry.getOsFamily())) {
            seq.addTask(createEchoVerbose("ensuring execute bit set for: " +
                                          nodeentry.getHostname() + ":" + remotefile, project));
            try {
                seq.addTask(createRemoteCommandProxy(nodeentry, new String[]{"chmod", "+x", remotefile}, project,
                    getFramework()));
            } catch (SSHTaskBuilder.BuilderException e) {
                throw new ExecutionException(e);
            }

        }
        /**
         * Prepare the script execution
         */
        seq.addTask(createEchoVerbose("executing scriptfile: " + nodeentry.getHostname() + ":" + remotefile, project));
        final Task sshexec;
        try {
            sshexec = createRemoteCommandProxy(nodeentry, remotecmd, project, getFramework());
        } catch (SSHTaskBuilder.BuilderException e) {
            throw new ExecutionException(e);
        }
        seq.addTask(sshexec);
        return seq;
    }

    @Override
    /**
     * overridden to write the script to a temp file, then pass the temp filepath to the ExecTaskParameterGenerator.
     */
    protected Task createLocalCommandProxy(final INodeEntry nodeentry, final Project project) throws
        ExecutionException {
        final Map<String, Map<String, String>> dataContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(nodeentry), getContext().getDataContext());
        final String[] newargs = DataContextUtils.replaceDataReferences(getContext().getArgs(), dataContext);

        //generate a temp file containing the script
        final File temp = writeScriptTempFile(scriptfile, dataContext);

        return createLocalCommandProxy(nodeentry, project, getParameterGenerator().generate(nodeentry, isCommandAction(), temp,
            newargs), dataContext);

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
        return createEcho(message, project, "verbose");
    }

    /**
     * Create Scp task to copy the scriptfile
     *
     * @param nodeentry node
     * @param project project
     * @param remotepath path
     *
     * @param sourceFile
     * @return Scp object
     */
    protected Scp createScp(final INodeEntry nodeentry, final Project project, final String remotepath,
                            final File sourceFile) {
        final INodeAuthResolutionStrategy nodeAuth = getFramework().getNodeAuthResolutionStrategy();

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
            final String sshKeypath = getFramework().getProperty(Constants.SSH_KEYPATH_PROP);
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

        /**
         * Set the local and remote file paths
         */
        scp.setLocalFile(sourceFile.getAbsolutePath());
        final String sshUriPrefix = nodeentry.extractUserName() + "@" + nodeentry.extractHostname() + ":";
        scp.setRemoteTofile(sshUriPrefix + remotepath);

        scp.setPassphrase(""); // set empty otherwise password will be required
        scp.setVerbose(getContext().getLoglevel() >= Project.MSG_VERBOSE);
        debug("Created scp action to copy file: " + remotepath);
        return scp;
    }

}

