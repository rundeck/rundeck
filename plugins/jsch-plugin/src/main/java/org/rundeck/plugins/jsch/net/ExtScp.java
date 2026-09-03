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

package org.rundeck.plugins.jsch.net;

import com.dtolabs.rundeck.core.utils.SSHAgent;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.Directory;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.apache.tools.ant.taskdefs.optional.ssh.ScpToMessage;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * ExtScp extends Ant's {@link Scp} task for Rundeck. Host, port and credentials are configured directly on the task
 * by {@code SSHTaskBuilder}; the remote destination is supplied verbatim through {@link #setRemotePath(String)} and
 * uploaded by {@link #execute()} without going through Ant's {@code user@host:path} URI parsing, which splits on the
 * last colon and therefore breaks Windows drive-letter paths such as {@code C:\WINDOWS\TEMP}.
 *
 * @author greg
 * @since 2014-03-20
 */
public class ExtScp extends Scp implements SSHTaskBuilder.SCPInterface {

    private String              knownhosts;
    private InputStream         sshKeyData;
    private long                timeout;
    private long connectTimeout;
    private long commandTimeout;
    private Map<String, String> sshConfig;
    private PluginLogger        pluginLogger;
    private String toDir;
    private List<FileSet> fileSets;
    private String bindAddress;
    private String remotePath;
    private String localFile;

    @Override
    public void setTodir(final String aToUri) {
        this.toDir = aToUri;
        super.setTodir(aToUri);
    }

    @Override
    public void addFileset(final FileSet set) {
        if (fileSets == null) {
            fileSets = new ArrayList<>();
        }
        fileSets.add(set);
        super.addFileset(set);
    }

    @Override
    public void setLocalFile(final String aFromUri) {
        this.localFile = aFromUri;
        super.setLocalFile(aFromUri);
    }

    /**
     * Set the remote destination path used verbatim by {@link #execute()}, bypassing Ant's URI parsing.
     *
     * @param remotePath remote file or directory path, may contain colons (e.g. Windows drive letters)
     */
    @Override
    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    /**
     * @return the remote path set via {@link #setRemotePath(String)}, or null
     */
    public String getIfaceRemotePath() {
        return remotePath;
    }

    /**
     * Upload the local file or nested filesets to {@link #setRemotePath(String) remotePath} using the host and
     * credentials already configured on this task. Falls back to Ant's {@link Scp#execute()} when no remote path
     * was set. Mirrors Ant's error contract: failures are raised as {@link BuildException} with the task location
     * when {@code failonerror} is true, otherwise logged.
     *
     * @throws BuildException on failure when failonerror is true
     */
    @Override
    public void execute() throws BuildException {
        if (remotePath == null) {
            super.execute();
            return;
        }
        try {
            if (localFile == null && (fileSets == null || fileSets.isEmpty())) {
                throw new BuildException("Either 'localFile' or one or more nested filesets are required.");
            }
            Session session = null;
            try {
                session = openSession();
                if (localFile != null) {
                    sendMessage(new ScpToMessage(getVerbose(), session, getProject().resolveFile(localFile), remotePath));
                } else {
                    final List<Directory> list = new ArrayList<>(fileSets.size());
                    for (FileSet set : fileSets) {
                        final Directory d = createDirectory(set);
                        if (d != null) {
                            list.add(d);
                        }
                    }
                    if (!list.isEmpty()) {
                        sendMessage(new ScpToMessage(getVerbose(), session, list, remotePath));
                    }
                }
            } finally {
                if (session != null) {
                    session.disconnect();
                }
            }
        } catch (final Exception e) {
            if (getFailonerror()) {
                final BuildException be = e instanceof BuildException ? (BuildException) e : new BuildException(e);
                if (be.getLocation() == null || Location.UNKNOWN_LOCATION.equals(be.getLocation())) {
                    be.setLocation(getLocation());
                }
                throw be;
            }
            log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
        }
    }

    /**
     * Perform the transfer for a prepared message. Extracted so tests can intercept the message.
     *
     * @param message prepared upload message
     * @throws IOException   on i/o error
     * @throws JSchException on scp error
     */
    protected void sendMessage(final ScpToMessage message) throws IOException, JSchException {
        message.setLogListener(this);
        message.execute();
    }

    /**
     * Build the {@link Directory} tree for a fileset, equivalent to Ant's private {@code Scp.createDirectory}.
     *
     * @param set fileset to scan
     * @return directory tree, or null if the fileset matched no files
     */
    private Directory createDirectory(final FileSet set) {
        final DirectoryScanner scanner = set.getDirectoryScanner(getProject());
        final String[] files = scanner.getIncludedFiles();
        if (files.length == 0) {
            return null;
        }
        final Directory root = new Directory(scanner.getBasedir());
        Stream.of(files).map(Directory::getPath).forEach(path -> {
            Directory current = root;
            File currentParent = scanner.getBasedir();
            for (String element : path) {
                final File file = new File(currentParent, element);
                if (file.isDirectory()) {
                    current.addDirectory(new Directory(file));
                    current = current.getChild(file);
                    currentParent = current.getDirectory();
                } else if (file.isFile()) {
                    current.addFile(file);
                }
            }
        });
        return root;
    }

    @Override
    public void setSshConfigSession(Map<String, String> config) {
        this.sshConfig = config;
    }

    protected Session openSession() throws JSchException {
        return SSHTaskBuilder.openSession(this);
    }

    public String getKnownhosts() {
        return knownhosts;
    }

    public void setKnownhosts(String knownhosts) {
        this.knownhosts = knownhosts;
        super.setKnownhosts(knownhosts);
    }

    public InputStream getSshKeyData() {
        return sshKeyData;
    }

    public void setSshKeyData(InputStream sshKeyData) {
        this.sshKeyData = sshKeyData;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getSshConfigSession() {
        return sshConfig;
    }

    @Override
    public String getKeyfile() {
        return getUserInfo().getKeyfile();
    }

    @Override
    public SSHUserInfo getUserInfo() {
        return super.getUserInfo();
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }

    public void setPluginLogger(PluginLogger pluginLogger) {
        this.pluginLogger = pluginLogger;
    }

    public SSHAgent getSSHAgentProcess() {
        return null;
    }

    public void setSSHAgentProcess(SSHAgent sshAgentProcess) {
    }

    public void setEnableSSHAgent(Boolean enableSSHAgent) {
    }

    public Boolean getEnableSSHAgent() {
        return Boolean.FALSE;
    }

    public void setTtlSSHAgent(Integer ttlSSHAgent) {
    }

    public Integer getTtlSSHAgent() {
        return 0;
    }

    @Override
    public void setBindAddress(String bindAddress) {
        this.bindAddress=bindAddress;
    }

    @Override
    public String getBindAddress() {
        return bindAddress;
    }

    public String getIfaceToDir() {
        return toDir;
    }

    public List<FileSet> getIfaceFileSets() {
        return fileSets;
    }

    @Override
    public long getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public long getCommandTimeout() {
        return commandTimeout;
    }

    @Override
    public void setCommandTimeout(long commandTimeout) {
        this.commandTimeout = commandTimeout;
    }
}
