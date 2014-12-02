package com.dtolabs.rundeck.core.tasks.net;

import com.dtolabs.rundeck.core.utils.SSHAgentProcess;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.taskdefs.optional.ssh.Scp;

import java.io.InputStream;
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
    private Map<String, String> sshConfig;
    private PluginLogger        pluginLogger;

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
}
