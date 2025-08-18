/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* SSHTaskFactory.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 11:33:34 AM
* $Id$
*/
package org.rundeck.plugins.jsch.net;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.SSHAgent;
import com.dtolabs.rundeck.core.utils.SSHAgentUtil;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.utils.Streams;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.AgentProxyException;
import com.jcraft.jsch.SSHAgentConnector;
import com.jcraft.jsch.AgentIdentityRepository;
import com.jcraft.jsch.SocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.FileSet;
import org.rundeck.storage.api.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

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
            try {
                base.setSSHAgentProcess(SSHAgentUtil.startAgent(base.getTtlSSHAgent()));
                base.getPluginLogger().log(
                        Project.MSG_DEBUG,
                        "ssh-agent started with ttl " +
                        base.getTtlSSHAgent().toString()
                );
                try {
                    SSHAgentConnector cf = new SSHAgentConnector(Path.of(base.getSSHAgentProcess().getSocketPath()));
                    AgentIdentityRepository identRepo = new AgentIdentityRepository(cf);
                    jsch.setIdentityRepository(identRepo);
                    base.getPluginLogger().log(
                            Project.MSG_DEBUG,
                            "ssh-agent used as identity repository."
                    );
                    base.getSshConfigSession().put("ForwardAgent", "yes");
                } catch (AgentProxyException e) {
                    throw new JSchException("Unable to add key to ssh-agent: " + e);
                }
            } catch (IOException e) {
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
        long conTimeout = base.getConnectTimeout();
        if(conTimeout<1){
            conTimeout = base.getTimeout();
        }
        session.setTimeout((int) conTimeout);
        if (base.getVerbose()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Set timeout to " + conTimeout);
        }

        session.setUserInfo(base.getUserInfo());
        if (base.getVerbose()) {
            base.getPluginLogger().log(Project.MSG_DEBUG, "Connecting to " + base.getHost() + ":" + base.getPort());
        }
        SSHTaskBuilder.configureSession(base.getSshConfigSession(), session);

        //add  bind address server
        String bindAddress=base.getBindAddress();
        if(bindAddress!=null){
            SocketFactory sfactory = new BindAddressSocketFactory(bindAddress, conTimeout);
            session.setSocketFactory(sfactory);
        }

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

        void setSSHAgentProcess(SSHAgent sshAgentProcess);

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

        void setTimeout(long sshTimeout);

        long getTimeout();

        void setConnectTimeout(long sshTimeout);

        long getConnectTimeout();

        void setCommandTimeout(long sshTimeout);

        long getCommandTimeout();

        void setKeyfile(String sshKeypath);

        String getKeyfile();

        void setSshKeyData(InputStream sshKeyData);

        InputStream getSshKeyData();

        void setPassphrase(String s);

        void setPassword(String password);

        void setKnownhosts(String knownhosts);

        String getKnownhosts();

        void setSshConfigSession(Map<String, String> config);

        Map<String, String> getSshConfigSession();

        public PluginLogger getPluginLogger();

        public void setPluginLogger(PluginLogger pluginLogger);

        public void setEnableSSHAgent(Boolean enableSSHAgent);

        public Boolean getEnableSSHAgent();

        public SSHAgent getSSHAgentProcess();

        public void setTtlSSHAgent(Integer ttlSSHAgent);

        public Integer getTtlSSHAgent();

        public void setBindAddress(String bindAddress);
        public String getBindAddress();
    }

    static interface SSHExecInterface extends SSHBaseInterface, DataContextUtils.EnvironmentConfigurable {

        void setCommand(String commandString);

        void setOutputproperty(String s);
    }

    static interface SSHSftpInterface extends SSHBaseInterface, DataContextUtils.EnvironmentConfigurable {

        void setLocalFile(File file);

        void setRemoteFile(String remotePath);
    }

    static interface SCPInterface extends SSHBaseInterface {

        void setLocalFile(String absolutePath);

        void setRemoteTofile(String s);

        void addFileset(FileSet set);

        void setTodir(String aToUri);
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

        @Override
        public void setConnectTimeout(final long sshTimeout) {
            instance.setConnectTimeout(sshTimeout);
        }

        @Override
        public long getConnectTimeout() {
            return instance.getConnectTimeout();
        }

        @Override
        public void setCommandTimeout(final long sshTimeout) {
            instance.setCommandTimeout(sshTimeout);
        }

        @Override
        public long getCommandTimeout() {
            return instance.getCommandTimeout();
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
        public void setSSHAgentProcess(SSHAgent sshAgentProcess) {
             instance.setSSHAgentProcess(sshAgentProcess);
        }

        @Override
        public SSHAgent getSSHAgentProcess() {
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
        public void setSshConfigSession(Map<String, String> config) {
            instance.setSshConfigSession(config);
        }

        @Override
        public Map<String, String> getSshConfigSession() {
            return instance.getSshConfigSession();
        }

        @Override
        public PluginLogger getPluginLogger() {
            return instance.getPluginLogger();
        }

        @Override
        public void setPluginLogger(PluginLogger pluginLogger) {
            instance.setPluginLogger(pluginLogger);
        }

        @Override
        public void setBindAddress(String bingAddress) {
            instance.setBindAddress(bingAddress);
        }

        @Override
        public String getBindAddress() {
            return instance.getBindAddress();
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

        public void addFileset(FileSet set) { instance.addFileset(set);}

        public void setTodir(String uri){instance.setTodir(uri);}

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
        sshexecTask.setTimeout(sshConnectionInfo.getTimeout());
        sshexecTask.setCommandTimeout(sshConnectionInfo.getCommandTimeout());
        sshexecTask.setConnectTimeout(sshConnectionInfo.getConnectTimeout());

        DataContextUtils.addEnvVars(sshexecTask, dataContext);
    }

    static void build(final SSHSftpInterface sshexecTask,
                      final INodeEntry nodeentry,
                      final File localFile,final String remoteFile, final Project project,
                      final Map<String, Map<String, String>> dataContext,
                      final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {

        configureSSHBase(nodeentry, project, sshConnectionInfo, sshexecTask, loglevel, logger);

        sshexecTask.setLocalFile(localFile);
        sshexecTask.setRemoteFile(remoteFile);
        sshexecTask.setTimeout(sshConnectionInfo.getConnectTimeout());

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
        String hostname = nodeentry.extractHostname();
        if (null == hostname || StringUtils.isBlank(hostname)) {
            throw new BuilderException(
                    "Hostname must be set to connect to remote node '" + nodeentry.getNodename() + "'"
            );
        }
        sshbase.setHost(hostname);
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
                /*
                 * Configure keybased authentication
                 */
                final String sshKeypath = sshConnectionInfo.getPrivateKeyfilePath();
                final String sshKeyResource = sshConnectionInfo.getPrivateKeyStoragePath();
                if (null != sshKeyResource) {
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
        sshbase.setSshConfigSession(baseConfig);
        sshbase.setEnableSSHAgent(sshConnectionInfo.getLocalSSHAgent());
        sshbase.setTtlSSHAgent(sshConnectionInfo.getTtlSSHAgent());
        sshbase.setPluginLogger(logger);
        sshbase.setBindAddress(sshConnectionInfo.getBindAddress());
    }

    public static Scp buildScp(final INodeEntry nodeentry, final Project project,
                                final String remotepath, final File sourceFile,
                                final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {


        final ExtScp scp = new ExtScp();
        buildScp(scp, nodeentry, project, remotepath, sourceFile, sshConnectionInfo, loglevel, logger);
        return scp;
    }

    public static Scp buildMultiScp(
            final INodeEntry nodeentry,
            final Project project,
            final File basedir,
            final List<File> files,
            final String remotePath,
            final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger
    ) throws
            BuilderException {


        final ExtScp scp = new ExtScp();
        buildMultiScp(scp, nodeentry, project, basedir, files, remotePath, sshConnectionInfo, loglevel, logger);
        return scp;
    }

    public static Scp buildRecursiveScp(final INodeEntry nodeentry, final Project project,
                                        final String remotepath, final File sourceFile,
                                        final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {


        final ExtScp scp = new ExtScp();
        buildRecursiveScp(scp, nodeentry, project, remotepath, sourceFile, sshConnectionInfo, loglevel, logger);
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

        scp.setTimeout(sshConnectionInfo.getTimeout());
        scp.setCommandTimeout(sshConnectionInfo.getCommandTimeout());
        scp.setConnectTimeout(sshConnectionInfo.getConnectTimeout());


        //Set the local and remote file paths

        scp.setLocalFile(sourceFile.getAbsolutePath());
        final String sshUriPrefix = username + "@" + nodeentry.extractHostname() + ":";
        scp.setRemoteTofile(sshUriPrefix + remotepath);
    }


    static void buildRecursiveScp(final SCPInterface scp, final INodeEntry nodeentry,
                         final Project project, final String remotePath, final File sourceFolder,
                         final SSHConnectionInfo sshConnectionInfo, final int loglevel, final PluginLogger logger) throws
            BuilderException {

        if (null == sourceFolder) {
            throw new BuilderException("sourceFolder was not set");
        }
        final String username = sshConnectionInfo.getUsername();
        if (null == username) {
            throw new BuilderException("username was not set");
        }

        configureSSHBase(nodeentry, project, sshConnectionInfo, scp, loglevel, logger);

        scp.setTimeout(sshConnectionInfo.getTimeout());
        scp.setCommandTimeout(sshConnectionInfo.getCommandTimeout());
        scp.setConnectTimeout(sshConnectionInfo.getConnectTimeout());

        //Set the local and remote file paths

        //scp.setLocalFile(sourceFolder.getAbsolutePath());

            FileSet set = new FileSet();
            set.setDir(sourceFolder);
            scp.addFileset(set);


        final String sshUriPrefix = username + "@" + nodeentry.extractHostname() + ":";
        scp.setTodir(sshUriPrefix + remotePath);

    }

    static void buildMultiScp(
            final SCPInterface scp,
            final INodeEntry nodeentry,
            final Project project,
            final File basedir,
            final List<File> files,
            final String remotePath,
            final SSHConnectionInfo sshConnectionInfo,
            final int loglevel,
            final PluginLogger logger
    ) throws
            BuilderException
    {

        if (null == files || files.size()==0) {
            throw new BuilderException("files was not set");
        }
        final String username = sshConnectionInfo.getUsername();
        if (null == username) {
            throw new BuilderException("username was not set");
        }

        configureSSHBase(nodeentry, project, sshConnectionInfo, scp, loglevel, logger);

        scp.setTimeout(sshConnectionInfo.getTimeout());
        scp.setCommandTimeout(sshConnectionInfo.getCommandTimeout());
        scp.setConnectTimeout(sshConnectionInfo.getConnectTimeout());

        //Set the local and remote file paths

        FileSet top = new FileSet();
        top.setProject(project);
        top.setDir(basedir);
        Map<File, String> dirs = new HashMap<>();

        //prepare include/** for each dir in the input list
        for(File source: files ){
            if (source.isDirectory() && !source.equals(basedir)) {
                String relpath = FileUtils.relativePath(basedir, source);
                dirs.put(source, relpath + "/**");
            }
        }
        //for each file in the input list, remove the parent dir/** pattern and
        //use an include pattern from the top dir
        for (File source : files) {
            if (!source.isDirectory()) {
                File parentDir = source.getParentFile();
                String dirpath = dirs.get(parentDir);
                if (null != dirpath) {
                    dirs.remove(parentDir);
                }
                top.setIncludes(FileUtils.relativePath(basedir, source));
            }
        }
        scp.addFileset(top);
        for (String dirpath : dirs.values()) {
            top.setIncludes(dirpath);
        }

        final String sshUriPrefix = username + "@" + nodeentry.extractHostname() + ":";
        scp.setTodir(sshUriPrefix + remotePath);
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

        public long getTimeout();

        public long getCommandTimeout();

        public long getConnectTimeout();

        public String getUsername();

        public Boolean getLocalSSHAgent();

        public Integer getTtlSSHAgent();

        public Map<String,String> getSshConfig();

        public String getBindAddress();
    }

}
