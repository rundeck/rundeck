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

/**
 * @deprecated use {@link SSHAgentUtil} instead
 */
@Deprecated
public class SSHAgentProcess
        implements SSHAgent
{
    private SSHAgent sshAgent;

    /**
     * @deprecated use {@link SSHAgentUtil} instead
     */
    @Deprecated
    public SSHAgentProcess() throws IOException {
        this("/usr/bin/ssh-agent", 0);
    }

    /**
     * @deprecated use {@link SSHAgentUtil} instead
     */
    @Deprecated
    public SSHAgentProcess(Integer timeToLive) throws IOException {
        this("/usr/bin/ssh-agent", timeToLive);
    }

    /**
     * @deprecated use {@link SSHAgentUtil} instead
     */
    @Deprecated
    public SSHAgentProcess(String sshAgentPath, Integer timeToLive) throws IOException {
        startProcess(sshAgentPath, timeToLive);
    }

    private void startProcess(String sshAgentPath, Integer timeToLive) throws IOException {
        this.sshAgent = SSHAgentUtil.startAgent(sshAgentPath, timeToLive);
    }

    @Override
    public String getSocketPath() {
        return sshAgent.getSocketPath();
    }

    @Override
    public void stopAgent() throws IOException {
        sshAgent.stopAgent();
    }
}
