package com.dtolabs.rundeck.core.utils;

import java.io.IOException;

/**
 * Manage an ssh-agent process
 */
public interface SSHAgent {
    void stopAgent() throws IOException;

    String getSocketPath();
}
