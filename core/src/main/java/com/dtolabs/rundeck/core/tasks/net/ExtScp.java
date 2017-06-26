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

package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.core.utils.SSHAgentProcess;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;
import org.apache.tools.ant.types.FileSet;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ExtScp is ...
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
    public void setSshConfig(Map<String, String> config) {
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

    public Map<String, String> getSshConfig() {
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

    public SSHAgentProcess getSSHAgentProcess() {
        return null;
    }

    public void setSSHAgentProcess(SSHAgentProcess sshAgentProcess) {
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
