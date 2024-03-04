/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.core.utils;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jsch.agentproxy.AgentProxyException;

public class SSHAgentProcess {
    private String  socketPath;
    private Integer pid;

    public String getSocketPath() {
        return socketPath;
    }

    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }

    public SSHAgentProcess() throws AgentProxyException {
        this("/usr/bin/ssh-agent", 0);
    }

    public SSHAgentProcess(Integer timeToLive) throws AgentProxyException {
        this("/usr/bin/ssh-agent", timeToLive);
    }

    public SSHAgentProcess(String sshAgentPath, Integer timeToLive) throws AgentProxyException {
        ProcessBuilder builder;
        if (timeToLive > 0) {
            builder = new ProcessBuilder(sshAgentPath, "-t", timeToLive.toString());
        } else {
            builder = new ProcessBuilder(sshAgentPath);
        }
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            InputStream is = process.getInputStream();

            byte[] buff = new byte[2048];
            is.read(buff);
            is.close();

            String agentOutput = new String(buff);
            String[] splitAgentOutput = agentOutput.split(";");

            String[] splitSocketPath = splitAgentOutput[0].split("=");
            socketPath = splitSocketPath[1];

            String[] splitAgentPid = splitAgentOutput[2].split("=");
            pid = Integer.valueOf(splitAgentPid[1]);
        } catch (Exception e) {
            throw new AgentProxyException("Unable to parse ssh-agent output.");
        }
    }

    public void stopAgent() throws IOException {
        if (this.pid != null) {
            Runtime.getRuntime().exec("kill " + this.pid);
        }
    }
}
