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
import com.jcraft.jsch.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHBase;
import org.apache.tools.ant.taskdefs.optional.ssh.SSHUserInfo;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileUtils;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ExtSSHSftp extends SSHBase implements SSHTaskBuilder.SSHSftpInterface {


    private File localFile;
    private String remoteFile;

    @Override
    public void setLocalFile(File file) {
        this.localFile=file;
    }

    @Override
    public void setRemoteFile(String remotePath) {
        this.remoteFile = remotePath;
    }


    /** units are milliseconds, default is 0=infinite */
    private long maxwait = 0;


    private PluginLogger logger;

    private List<Environment.Variable> envVars=null;

    private Boolean enableSSHAgent=false;
    private Integer ttlSSHAgent=0;
    private SSHAgentProcess sshAgentProcess=null;

    private static final String TIMEOUT_MESSAGE =
        "Timeout period exceeded, connection dropped.";


    public ExtSSHSftp() {
        super();
    }

    public void copy() throws BuildException {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null
                && getUserInfo().getPassword() == null
                && getSshKeyData() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (localFile == null || remoteFile == null) {
            throw new BuildException("LocalFile and remoteFile are required.");
        }


        Session session = null;

        try {
            session = openSession();

            executeCopy(session,localFile,remoteFile);
        } catch (JSchException e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } finally {
            try {
                if (null != this.sshAgentProcess) {
                    this.sshAgentProcess.stopAgent();
                }
            } catch (IOException e) {
                log( "Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }

            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }



    private void executeCopy(Session session, File local, String remote)
            throws BuildException {
        InputStream file = null;
        try {
            file = new FileInputStream(local);
            final ChannelSftp channel;
            session.setTimeout((int) maxwait);

            channel = (ChannelSftp) session.openChannel("sftp");
            if(null != this.sshAgentProcess){
                channel.setAgentForwarding(true);
            }


            /* set env vars if any are embedded */
            if(null!=envVars && envVars.size()>0){
                for(final Environment.Variable env:envVars) {
                    channel.setEnv(env.getKey(), env.getValue());
                }
            }

            channel.connect();


            String remoteDir="";
            final String separator = "/";
            if(remote.endsWith(separator)){
                remoteDir=remote;
            }else{
                int index = remote.lastIndexOf(separator);
                remoteDir = remote.substring(0,index);
            }

            String[] folders = remoteDir.split("/");
            for(String folder:folders){
                if(folder.length()==0) { //split generate a first folder empty
                    channel.cd("/");
                }else {
                    try {
                        channel.cd(folder);
                    } catch (SftpException e) {
                        channel.mkdir(folder);
                        channel.cd(folder);
                    }
                }


            }

            channel.put(file,remote);

            channel.disconnect();

        } catch (BuildException e) {
            throw e;
        } catch (JSchException e) {
            if (e.getMessage().indexOf("session is down") >= 0) {
                if (getFailonerror()) {
                    throw new BuildException(TIMEOUT_MESSAGE, e);
                } else {
                    log(TIMEOUT_MESSAGE, Project.MSG_ERR);
                }
            } else {
                if (getFailonerror()) {
                    throw new BuildException(e);
                } else {
                    log("Caught exception: " + e.getMessage(),
                            Project.MSG_ERR);
                }
            }
        } catch (Exception e) {
            if (getFailonerror()) {
                throw new BuildException(e);
            } else {
                log("Caught exception: " + e.getMessage(), Project.MSG_ERR);
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            FileUtils.close(file);
        }
    }



    public void setTimeout(long timeout) {
        maxwait = timeout;
    }
    public long getTimeout(){
        return maxwait;
    }


    public void addEnv(final Environment.Variable env){
        if(null==envVars) {
            envVars = new ArrayList<Environment.Variable>();
        }
        envVars.add(env);
    }


    public PluginLogger getPluginLogger() {
        return logger;
    }

    public void setPluginLogger(PluginLogger logger) {
        this.logger = logger;
    }


    public Map<String, String> getSshConfig() {
        return sshConfig;
    }

    public InputStream getSshKeyData() {
        return sshKeyData;
    }
    private String knownHosts;

    private InputStream sshKeyData;
    @Override
    public void setSshKeyData(InputStream sshKeyData) {
        this.sshKeyData=sshKeyData;
    }


    public void setKnownhosts(String knownHosts) {
        this.knownHosts = knownHosts;
        super.setKnownhosts(knownHosts);
    }

    private Map<String,String> sshConfig;
    @Override
    public void setSshConfig(Map<String, String> config) {
        this.sshConfig=config;
    }



    protected Session openSession() throws JSchException {
        return SSHTaskBuilder.openSession(this);
    }


    @Override
    public String getKeyfile() {
        return getUserInfo().getKeyfile();
    }

    @Override
    public String getKnownhosts() {
        return knownHosts;
    }

    public SSHUserInfo getUserInfo(){
        return super.getUserInfo();
    }

    @Override
    public void setEnableSSHAgent(Boolean enableSSHAgent) {
        this.enableSSHAgent = enableSSHAgent;
    }

    @Override
    public Boolean getEnableSSHAgent() {
        return this.enableSSHAgent;
    }

    @Override
    public void setTtlSSHAgent(Integer ttlSSHAgent) {
        this.ttlSSHAgent = ttlSSHAgent;
    }

    @Override
    public Integer getTtlSSHAgent() {
      return this.ttlSSHAgent;
    }
    public SSHAgentProcess getSSHAgentProcess() {
        return this.sshAgentProcess;
    }

    public void setSSHAgentProcess(SSHAgentProcess sshAgentProcess) {
        this.sshAgentProcess = sshAgentProcess;
    }
}
