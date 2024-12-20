package com.dtolabs.rundeck.core.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility for starting ssh-agent
 */
public class SSHAgentUtil {
    static final String AGENT_PATH = "/usr/bin/ssh-agent";

    public static SSHAgent startAgent() throws IOException {
        return startAgent(AGENT_PATH, 0);
    }

    public static SSHAgent startAgent(int timeToLive) throws IOException {
        return startAgent(AGENT_PATH, timeToLive);
    }

    /**
     * Start an ssh-agent process
     *
     * @param sshAgentPath path to ssh-agent executable
     * @param timeToLive   time to live in seconds, or 0 for no time limit
     * @return SSHAgent instance
     * @throws IOException if an error occurs
     */
    public static SSHAgent startAgent(String sshAgentPath, Integer timeToLive) throws IOException {
        ProcessBuilder builder;
        if (timeToLive > 0) {
            builder = new ProcessBuilder(sshAgentPath, "-t", timeToLive.toString());
        } else {
            builder = new ProcessBuilder(sshAgentPath);
        }
        builder.redirectErrorStream(true);

        Process process = builder.start();
        InputStream is = process.getInputStream();

        byte[] buff = new byte[2048];
        is.read(buff);
        is.close();

        String agentOutput = new String(buff);
        String[] splitAgentOutput = agentOutput.split(";");

        String[] splitSocketPath = splitAgentOutput[0].split("=");
        final String socketPath = splitSocketPath[1];

        String[] splitAgentPid = splitAgentOutput[2].split("=");
        final int pid = Integer.valueOf(splitAgentPid[1]);
        return new SSHAgent() {
            @Override
            public void stopAgent() throws IOException {
                Runtime.getRuntime().exec("kill " + pid);
            }

            @Override
            public String getSocketPath() {
                return socketPath;
            }
        };
    }
}
