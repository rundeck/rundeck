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

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHBase;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.apache.tools.ant.types.Environment;

import java.io.File;
import java.util.Map;

/**
 * SSHTaskFactory constructs a ExtSSHExec task
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SSHTaskBuilder {

    /**
     * interface that mimics SSHBase methods called
     */
    static interface SSHBaseInterface {

        void setFailonerror(boolean b);

        void setTrust(boolean b);

        void setProject(Project project);

        void setVerbose(boolean b);

        void setHost(String s);

        void setPort(int portNum);

        void setUsername(String username);

        void setKeyfile(String sshKeypath);

        void setPassphrase(String s);

        void setPassword(String password);

        void setKnownhosts(String knownhosts);
    }

    static interface SSHExecInterface extends SSHBaseInterface, DataContextUtils.EnvironmentConfigurable {

        void setCommand(String commandString);

        void setTimeout(long sshTimeout);

        void setOutputproperty(String s);
    }

    static interface SCPInterface extends SSHBaseInterface {

        void setLocalFile(String absolutePath);

        void setRemoteTofile(String s);
    }

    private static abstract class SSHBaseImpl implements SSHBaseInterface {
        SSHBase instance;

        public void setFailonerror(boolean b) {
            instance.setFailonerror(b);
        }

        public void setTrust(boolean b) {
            instance.setTrust(b);
        }

        public void setProject(Project project) {
            instance.setProject(project);
        }

        public void setVerbose(boolean b) {
            instance.setVerbose(b);
        }

        public void setHost(String s) {
            instance.setHost(s);
        }

        public void setPort(int portNum) {
            instance.setPort(portNum);
        }

        public void setUsername(String username) {

            instance.setUsername(username);
        }

        public void setKeyfile(String sshKeypath) {
            instance.setKeyfile(sshKeypath);
        }

        public void setPassphrase(String s) {
            instance.setPassphrase(s);
        }

        public void setPassword(String password) {
            instance.setPassword(password);
        }

        private SSHBaseImpl(SSHBase instance) {
            this.instance = instance;
        }

        public void setKnownhosts(String knownhosts) {
            instance.setKnownhosts(knownhosts);
        }
    }

    private static final class SSHExecImpl extends SSHBaseImpl implements SSHExecInterface {
        ExtSSHExec instance;

        private SSHExecImpl(ExtSSHExec instance) {
            super(instance);
            this.instance = instance;
        }

        public void setCommand(String commandString) {
            instance.setCommand(commandString);
        }

        public void setTimeout(long sshTimeout) {
            instance.setTimeout(sshTimeout);
        }

        public void setOutputproperty(String s) {
            instance.setOutputproperty(s);
        }

        public void addEnv(Environment.Variable env) {
            instance.addEnv(env);
        }

    }

    private static final class SCPImpl extends SSHBaseImpl implements SCPInterface {
        Scp instance;

        private SCPImpl(Scp instance) {
            super(instance);
            this.instance = instance;
        }

        public void setLocalFile(String absolutePath) {
            instance.setLocalFile(absolutePath);
        }

        public void setRemoteTofile(String s) {
            instance.setRemoteTofile(s);
        }
    }

    /**
     * Build a Task that performs SSH command
     *
     * @param loglevel
     * @param nodeentry   target node
     * @param args        arguments
     * @param project     ant project
     * @param dataContext
     * @param finder
     *
     * @return task
     */
    public static ExtSSHExec build(final INodeEntry nodeentry, final String[] args,
                                   final Project project,
                                   final Map<String, Map<String, String>> dataContext,
                                   final SSHConnectionInfo sshConnectionInfo, final int loglevel) throws
        BuilderException {


        final ExtSSHExec extSSHExec = new ExtSSHExec();
        build(new SSHExecImpl(extSSHExec), nodeentry, args, project, dataContext, sshConnectionInfo,
            loglevel);
        return extSSHExec;

    }

    static void build(final SSHExecInterface sshexecTask,
                      final INodeEntry nodeentry,
                      final String[] args, final Project project,
                      final Map<String, Map<String, String>> dataContext,
                      final SSHConnectionInfo sshConnectionInfo, final int loglevel) throws
        BuilderException {

        configureSSHBase(nodeentry, project, sshConnectionInfo, sshexecTask, loglevel);

        final String commandString = CLIUtils.generateArgline(null, args);
        sshexecTask.setCommand(commandString);
        sshexecTask.setTimeout(sshConnectionInfo.getSSHTimeout());
        sshexecTask.setOutputproperty("sshexec.output");


        DataContextUtils.addEnvVars(sshexecTask, dataContext);
    }

    private static void configureSSHBase(final INodeEntry nodeentry, final Project project,
                                         final SSHConnectionInfo sshConnectionInfo,
                                         final SSHBaseInterface sshbase, final double loglevel) throws
        BuilderException {

        sshbase.setFailonerror(true);
        sshbase.setTrust(true); // set this true to avoid  "reject HostKey" errors
        sshbase.setProject(project);
        sshbase.setVerbose(loglevel >= Project.MSG_VERBOSE);
        sshbase.setHost(nodeentry.extractHostname());
        // If the node entry contains a non-default port, configure the connection to use it.
        if (nodeentry.containsPort()) {
            final int portNum;
            try {
                portNum = Integer.parseInt(nodeentry.extractPort());
            } catch (NumberFormatException e) {
                throw new BuilderException("Port number is not valid: " + nodeentry.extractPort(), e);
            }
            sshbase.setPort(portNum);
        }
        final String username = sshConnectionInfo.getUsername();
        if (null == username) {
            throw new BuilderException("username was not set");
        }
        sshbase.setUsername(username);

        final AuthenticationType authenticationType = sshConnectionInfo.getAuthenticationType();
        if(null==authenticationType) {
            throw new BuilderException("SSH authentication type undetermined");
        }
        switch (authenticationType) {
            case privateKey:
                /**
                 * Configure keybased authentication
                 */
                final String sshKeypath = sshConnectionInfo.getPrivateKeyfilePath();
                if(null == sshKeypath || "".equals(sshKeypath)){
                    throw new BuilderException("SSH Keyfile path was not set");
                }
                if (!new File(sshKeypath).exists()) {
                    throw new BuilderException("SSH Keyfile does not exist: " + sshKeypath);
                }
                project.log("Using ssh keyfile: " + sshKeypath, Project.MSG_DEBUG);
                sshbase.setKeyfile(sshKeypath);
                sshbase.setPassphrase(""); // set empty otherwise password will be required
                break;
            case password:
                final String password = sshConnectionInfo.getPassword();
                final boolean valid = null != password && !"".equals(password);
                if (!valid) {
                    throw new BuilderException("SSH Password was not set");
                }
                sshbase.setPassword(password);
                break;
        }
    }

    public static Scp buildScp(final INodeEntry nodeentry, final Project project,
                               final String remotepath, final File sourceFile,
                               final SSHConnectionInfo sshConnectionInfo, final int loglevel) throws BuilderException {


        final Scp scp = new Scp();
        buildScp(new SCPImpl(scp), nodeentry, project, remotepath, sourceFile, sshConnectionInfo, loglevel);
        return scp;
    }

    static void buildScp(final SCPInterface scp, final INodeEntry nodeentry,
                         final Project project, final String remotepath, final File sourceFile,
                         final SSHConnectionInfo sshConnectionInfo, final int loglevel) throws
        BuilderException {

        if(null==sourceFile) {
            throw new BuilderException("sourceFile was not set");
        }
        if(null==remotepath) {
            throw new BuilderException("remotePath was not set");
        }
        final String username = sshConnectionInfo.getUsername();
        if(null==username){
            throw new BuilderException("username was not set");
        }
        
        configureSSHBase(nodeentry, project, sshConnectionInfo, scp, loglevel);

        //Set the local and remote file paths
        
        scp.setLocalFile(sourceFile.getAbsolutePath());
        final String sshUriPrefix = username + "@" + nodeentry.extractHostname() + ":";
        scp.setRemoteTofile(sshUriPrefix + remotepath);
    }

    public static class BuilderException extends Exception {
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


    public static enum AuthenticationType {
        privateKey,
        password
    }

    /**
     * Defines the authentication input for a build
     */
    public static interface SSHConnectionInfo {
        public AuthenticationType getAuthenticationType();

        public String getPrivateKeyfilePath();

        public String getPassword();

        public int getSSHTimeout();

        public String getUsername();
    }

}
