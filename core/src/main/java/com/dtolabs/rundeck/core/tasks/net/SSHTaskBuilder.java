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

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.utils.SSHAgentProcess;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.utils.Streams;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.apache.tools.ant.types.Environment;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * SSHTaskFactory constructs a ExtSSHExec task
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SSHTaskBuilder {
    public static final String SSH_CONFIG_SERVER_ALIVE_COUNT_MAX = "ServerAliveCountMax";
    public static final String SSH_CONFIG_SERVER_ALIVE_INTERVAL = "ServerAliveInterval";

    private static Map<String, String> DEFAULT_SSH_CONFIG = Collections.unmodifiableMap(new HashMap<String, String>() {{
        //use keyboard-interactive last
        put("PreferredAuthentications", "publickey,password,keyboard-interactive");
        put("MaxAuthTries", "1");
    }});

    public static Map<String, String> getDefaultSshConfig() {
        return DEFAULT_SSH_CONFIG;
    }
    static int getJschLogLevel(int antLogLevel) {
        // reassign log levels, to quell Jsch logging at normal levels, but
        // pass more log info at verbose levels
        //
        switch (antLogLevel){
            case Project.MSG_DEBUG:
                return Logger.DEBUG;
            case Project.MSG_VERBOSE:
                return Logger.INFO;
            case Project.MSG_ERR:
                return Logger.FATAL;
            case Project.MSG_WARN:
            case Project.MSG_INFO:
            default:
                return Logger.ERROR;
        }
    }
    /**
     * Open Jsch session, applies private key configuration, timeout and custom ssh configuration
     * @param base base
     * @return session
     * @throws JSchException on jsch error
     */
    public static Session openSession(SSHBaseInterface base) throws JSchException {
        JSch jsch = new JSch();

        //will set Jsch static logger
        ThreadBoundJschLogger.getInstance(
                base.getPluginLogger(),
                getJschLogLevel(
                        base.getVerbose()
                        ? Project.MSG_DEBUG
                        : Project.MSG_INFO
                )
        );


        if (base.getEnableSSHAgent()) {
            ConnectorFactory cf = ConnectorFactory.getDefault();
            try {
                base.setSSHAgentProcess(new SSHAgentProcess(base.getTtlSSHAgent()));
                cf.setUSocketPath(base.getSSHAgentProcess().getSocketPath());
                cf.setPreferredUSocketFactories("jna,nc");
                base.getPluginLogger().log(
                        Project.MSG_DEBUG,
                        "ssh-agent started with ttl " +
                        base.getTtlSSHAgent().toString()
                );
                try {
                    Connector c = cf.createConnector();
                    RemoteIdentityRepository identRepo = new RemoteIdentityRepository(c);
                    jsch.setIdentityRepository(identRepo);
                    base.getPluginLogger().log(
                            Project.MSG_DEBUG,
                            "ssh-agent used as identity repository."
                    );
                    base.getSshConfig().put("ForwardAgent", "yes");
                } catch (AgentProxyException e) {
                    throw new JSchException("Unable to add key to ssh-agent: " + e);
                }
            } catch (AgentProxyException e) {
                throw new JSchException("Unable to start ssh-agent: " + e);
            }
        }
        
        if (null != base.getSshKeyData()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Using stored private key data.");
            //XXX: reset password to null, which was non-null to bypass Ant's behavior
            base.setPassword(null);
            try {
                jsch.addIdentity("sshkey", SSHTaskBuilder.streamBytes(base.getSshKeyData()), null, null);
            } catch (IOException e) {
                throw new JSchException("Failed to ready private ssh key data");
            }
        }else  if (null != base.getUserInfo().getKeyfile()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Using private key file: "+base.getUserInfo().getKeyfile());
            jsch.addIdentity(base.getUserInfo().getKeyfile());
        }

        if (!base.getUserInfo().getTrust() && base.getKnownhosts() != null) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Using known hosts: " + base.getKnownhosts());
            jsch.setKnownHosts(base.getKnownhosts());
        }

        Session session = jsch.getSession(base.getUserInfo().getName(), base.getHost(), base.getPort());
        session.setTimeout((int) base.getTimeout());
        if (base.getVerbose()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Set timeout to " + base.getTimeout());
        }

        session.setUserInfo(base.getUserInfo());
        if (base.getVerbose()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Connecting to " + base.getHost() + ":" + base.getPort());
        }
        SSHTaskBuilder.configureSession(base.getSshConfig(), session);

        session.connect();
        return session;
    }

    public static void configureSession(Map<String, String> config, Session session) {
        Properties newconf = new Properties();
        newconf.putAll(config);
        session.setConfig(newconf);
        try {
            configureSessionServerAliveInterval(config, session);
            configureSessionServerAliveCountMax(config, session);
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    private static void configureSessionServerAliveInterval(Map<String, String> config, Session session) throws JSchException {
        String serverAliveInterval = config.get(SSH_CONFIG_SERVER_ALIVE_INTERVAL);
        if (serverAliveInterval != null) {
            try {
                session.setServerAliveInterval(Integer.parseInt(serverAliveInterval) * 1000);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private static void configureSessionServerAliveCountMax(Map<String, String> config, Session session) {
        String serverAliveCountMax = config.get(SSH_CONFIG_SERVER_ALIVE_COUNT_MAX);
        if (serverAliveCountMax != null) {
            try {
                session.setServerAliveCountMax(Integer.parseInt(serverAliveCountMax));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] streamBytes(InputStream sshKeyData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.copyStream(sshKeyData, out);
        return out.toByteArray();
    }

    /**
     * interface that mimics SSHBase methods called
     */
    public static interface SSHBaseInterface {
        SSHUserInfo getUserInfo();

        void setSSHAgentProcess(SSHAgentProcess sshAgentProcess);

        void setFailonerror(boolean b);

        void setTrust(boolean b);

        void setProject(Project project);

        void setVerbose(boolean b);

        boolean getVerbose();

        void setHost(String s);

        String getHost();

        void setPort(int portNum);

        int getPort();

        void setUsername(String username);

        public void setTimeout(long sshTimeout);

        long getTimeout();

        void setKeyfile(String sshKeypath);

        String getKeyfile();

        void setSshKeyData(InputStream sshKeyData);

        InputStream getSshKeyData();

        void setPassphrase(String s);

        void setPassword(String password);

        void setKnownhosts(String knownhosts);

        String getKnownhosts();

        void setSshConfig(Map<String, String> config);

        Map<String, String> getSshConfig();

        public PluginLogger getPluginLogger();

        public void setPluginLogger(PluginLogger pluginLogger);
    
        public void setEnableSSHAgent(Boolean enableSSHAgent);
        
        public Boolean getEnableSSHAgent();
        
        public SSHAgentProcess getSSHAgentProcess();

        public void setTtlSSHAgent(Integer ttlSSHAgent);
        
        public Integer getTtlSSHAgent();
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
        SSHBaseInterface instance;

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

        public void setTimeout(long sshTimeout) {
            instance.setTimeout(sshTimeout);
        }

        public void setKeyfile(String sshKeypath) {
            instance.setKeyfile(sshKeypath);
        }

        @Override
        public void setSshKeyData(InputStream sshKeyData) {
            instance.setSshKeyData(sshKeyData);
        }

        public void setPassphrase(String s) {
            instance.setPassphrase(s);
        }

        public void setPassword(String password) {
            instance.setPassword(password);
        }

        private SSHBaseImpl(SSHBaseInterface instance) {
            this.instance = instance;
        }

        public void setKnownhosts(String knownhosts) {
            instance.setKnownhosts(knownhosts);
        }

        @Override
        public SSHUserInfo getUserInfo() {
            return instance.getUserInfo();
        }

        @Override
        public boolean getVerbose() {
            return instance.getVerbose();
        }

        @Override
        public String getHost() {
            return instance.getHost();
        }

        @Override
        public int getPort() {
            return instance.getPort();
        }

        @Override
        public long getTimeout() {
            return instance.getTimeout();
        }

        @Override
        public String getKeyfile() {
            return instance.getKeyfile();
        }

        @Override
        public InputStream getSshKeyData() {
            return instance.getSshKeyData();
        }

        @Override
        public void setEnableSSHAgent(Boolean enableSSHAgent) {
            instance.setEnableSSHAgent(enableSSHAgent);
        }
        
        @Override
        public Boolean getEnableSSHAgent() {
            return instance.getEnableSSHAgent();
        }
        
        @Override
        public void setSSHAgentProcess(SSHAgentProcess sshAgentProcess) {
             instance.setSSHAgentProcess(sshAgentProcess);
        }
        
        @Override
        public SSHAgentProcess getSSHAgentProcess() {
            return instance.getSSHAgentProcess();
        }
        
        
        @Override
        public void setTtlSSHAgent(Integer ttlSSHAgent) {
            instance.setTtlSSHAgent(ttlSSHAgent);
        }
        
        @Override
        public Integer getTtlSSHAgent() {
            return instance.getTtlSSHAgent();
        }
        
        @Override
        public String getKnownhosts() {
            return instance.getKnownhosts();
        }

        @Override
        public void setSshConfig(Map<String, String> config) {
            instance.setSshConfig(config);
        }

        @Override
        public Map<String, String> getSshConfig() {
            return instance.getSshConfig();
        }

        @Override
        public PluginLogger getPluginLogger() {
            return instance.getPluginLogger();
        }

        @Override
        public void setPluginLogger(PluginLogger pluginLogger) {
            instance.setPluginLogger(pluginLogger);
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


        public void setOutputproperty(String s) {
            instance.setOutputproperty(s);
        }

        public void addEnv(Environment.Variable env) {
            instance.addEnv(env);
        }

    }

    private static final class SCPImpl extends SSHBaseImpl implements SCPInterface {
        ExtScp instance;

        private SCPImpl(ExtScp instance) {
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
     * @param loglevel  level
     * @param nodeentry   target node
     * @param args        arguments
     * @param project     ant project
     * @param dataContext data
     * @param logger logger
     * @param sshConnectionInfo connection info
     *
     * @return task
     * @throws BuilderException on error
     */
    public static ExtSSHExec build(final INodeEntry nodeentry, final String[] args,
            final Project project,
            final Map<String, Map<String, String>> dataContext,
            final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {


        final ExtSSHExec extSSHExec = new ExtSSHExec();
        build(extSSHExec, nodeentry, args, project, dataContext, sshConnectionInfo,
                loglevel, logger);

        extSSHExec.setAntLogLevel(loglevel);
        return extSSHExec;

    }

    static void build(final SSHExecInterface sshexecTask,
            final INodeEntry nodeentry,
            final String[] args, final Project project,
            final Map<String, Map<String, String>> dataContext,
            final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {

        configureSSHBase(nodeentry, project, sshConnectionInfo, sshexecTask, loglevel, logger);

        //nb: args are already quoted as necessary
        final String commandString = StringUtils.join(args, " ");
        sshexecTask.setCommand(commandString);
        sshexecTask.setTimeout(sshConnectionInfo.getSSHTimeout());

        DataContextUtils.addEnvVars(sshexecTask, dataContext);
    }

    private static void configureSSHBase(final INodeEntry nodeentry, final Project project,
            final SSHConnectionInfo sshConnectionInfo,
            final SSHBaseInterface sshbase, final double loglevel, final PluginLogger logger) throws
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
        if (null == authenticationType) {
            throw new BuilderException("SSH authentication type undetermined");
        }
        switch (authenticationType) {
            case privateKey:
                /**
                 * Configure keybased authentication
                 */
                final String sshKeypath = sshConnectionInfo.getPrivateKeyfilePath();
                final String sshKeyResource = sshConnectionInfo.getPrivateKeyStoragePath();
                if (null != sshKeyResource) {
                    if (!PathUtil.hasRoot(sshKeyResource, "keys/")) {
                        throw new BuilderException("SSH Private key path is expected to start with \"keys/\": " +
                                sshKeyResource);
                    }
                    logger.log(Project.MSG_DEBUG, "Using ssh key storage path: " + sshKeyResource);
                    try {
                        InputStream privateKeyResourceData = sshConnectionInfo.getPrivateKeyStorageData();
                        sshbase.setSshKeyData(privateKeyResourceData);
                    } catch (StorageException | IOException e) {
                        logger.log(Project.MSG_ERR,"Failed to read SSH Private key stored at path: " +
                                sshKeyResource+": "+ e);
                        throw new BuilderException("Failed to read SSH Private key stored at path: " +
                                sshKeyResource,e);
                    }
                    //XXX: bypass password & keyfile null check in Ant 1.8.3's Scp.java:370, is restored to null in {@link #openSession}.
                    sshbase.setPassword("");
                } else if (null != sshKeypath && !"".equals(sshKeypath)) {
                    if (!new File(sshKeypath).exists()) {
                        throw new BuilderException("SSH Keyfile does not exist: " + sshKeypath);
                    }
                    logger.log(Project.MSG_DEBUG,"Using ssh keyfile: " + sshKeypath);
                    sshbase.setKeyfile(sshKeypath);
                } else {
                    throw new BuilderException("SSH Keyfile or storage path must be set to use privateKey " +
                            "authentication");
                }

                //load private key passphrase from storage if a path is set
                String privateKeyPassphraseStoragePath = sshConnectionInfo.getPrivateKeyPassphraseStoragePath();
                if (null != privateKeyPassphraseStoragePath) {
                    if (!PathUtil.hasRoot(privateKeyPassphraseStoragePath, "keys")) {
                        throw new BuilderException(
                                "SSH Private key path is expected to start with \"keys/\": " +
                                privateKeyPassphraseStoragePath
                        );
                    }
                    logger.log(Project.MSG_DEBUG,
                               "Using ssh key passphrase storage path: " +
                               privateKeyPassphraseStoragePath
                    );
                    try {
                        byte[] passphrase = sshConnectionInfo.getPrivateKeyPassphraseStorageData();
                        sshbase.setPassphrase(new String(passphrase));
                    } catch (StorageException | IOException e) {
                        logger.log(
                                Project.MSG_ERR, "Failed to read SSH Private key passphrase stored at path: " +
                                                 privateKeyPassphraseStoragePath + ": " + e
                        );
                        throw new BuilderException(
                                "Failed to read SSH Private key passphrase stored at path: " +
                                privateKeyPassphraseStoragePath, e
                        );
                    }
                    //XXX: bypass password & keyfile null check in Ant 1.8.3's Scp.java:370, is restored to null in
                    // {@link #openSession}.
                    sshbase.setPassword("");
                }else if (null != sshConnectionInfo.getPrivateKeyPassphrase()) {
                    sshbase.setPassphrase(sshConnectionInfo.getPrivateKeyPassphrase());
                } else {
                    sshbase.setPassphrase(""); // set empty otherwise password will be required
                }
                break;
            case password:
                final String passwordStoragePath = sshConnectionInfo.getPasswordStoragePath();
                if (null != passwordStoragePath) {
                    if (!PathUtil.asPath(passwordStoragePath).getPath().startsWith("keys/")) {
                        throw new BuilderException("SSH Password storage path is expected to start with \"keys/\": " +
                                passwordStoragePath);
                    }
                    logger.log(Project.MSG_DEBUG, "Using ssh password storage path: " + passwordStoragePath);
                    try {
                        byte[] data = sshConnectionInfo.getPasswordStorageData();
                        String password = new String(data);
                        if ("".equals(password)) {
                            throw new BuilderException("SSH Password was not set");
                        }
                        sshbase.setPassword(password);
                    } catch (StorageException e) {
                        logger.log(Project.MSG_ERR, "Failed to read SSH Password stored at path: " +
                                passwordStoragePath + ": " + e);
                        throw new BuilderException("Failed to read SSH Password stored at path: " +
                                passwordStoragePath, e);
                    } catch (IOException e) {
                        logger.log(Project.MSG_ERR, "Failed to read SSH Password stored at path: " +
                                passwordStoragePath + ": " + e);
                        throw new BuilderException("Failed to read SSH Password stored at path: " +
                                passwordStoragePath, e);
                    }
                }else{
                    final String password = sshConnectionInfo.getPassword();
                    final boolean valid = null != password && !"".equals(password);
                    if (!valid) {
                        throw new BuilderException("SSH Password was not set");
                    }
                    sshbase.setPassword(password);
                }
                break;
        }
        Map<String, String> sshConfig = sshConnectionInfo.getSshConfig();
        Map<String, String> baseConfig = new HashMap<String, String>(getDefaultSshConfig());
        if(null!=sshConfig) {
            baseConfig.putAll(sshConfig);
        }
        sshbase.setSshConfig(baseConfig);
        sshbase.setEnableSSHAgent(sshConnectionInfo.getLocalSSHAgent());
        sshbase.setTtlSSHAgent(sshConnectionInfo.getTtlSSHAgent());
        sshbase.setPluginLogger(logger);
    }

    public static Scp buildScp(final INodeEntry nodeentry, final Project project,
            final String remotepath, final File sourceFile,
            final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {


        final ExtScp scp = new ExtScp();
        buildScp(scp, nodeentry, project, remotepath, sourceFile, sshConnectionInfo, loglevel, logger);
        return scp;
    }

    static void buildScp(final SCPInterface scp, final INodeEntry nodeentry,
            final Project project, final String remotepath, final File sourceFile,
            final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {

        if (null == sourceFile) {
            throw new BuilderException("sourceFile was not set");
        }
        if (null == remotepath) {
            throw new BuilderException("remotePath was not set");
        }
        final String username = sshConnectionInfo.getUsername();
        if (null == username) {
            throw new BuilderException("username was not set");
        }

        configureSSHBase(nodeentry, project, sshConnectionInfo, scp, loglevel, logger);

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

        public String getPrivateKeyStoragePath();
        public String getPasswordStoragePath();

        public InputStream getPrivateKeyStorageData() throws IOException;

        public byte[] getPasswordStorageData() throws IOException;

        String getSudoPasswordStoragePath(String prefix);

        byte[] getSudoPasswordStorageData(String prefix) throws IOException;

        String getSudoPassword(String prefix);


        /**
         * @return the private key passphrase if set, or null.
         */
        public String getPrivateKeyPassphrase();
        public String getPrivateKeyPassphraseStoragePath();
        public byte[] getPrivateKeyPassphraseStorageData() throws IOException;

        public String getPassword();

        public int getSSHTimeout();

        public String getUsername();
        
        public Boolean getLocalSSHAgent();
        
        public Integer getTtlSSHAgent();

        public Map<String,String> getSshConfig();
    }

}
