package com.dtolabs.rundeck.core.execution.service;

import java.util.Map;

/**
 * Define default providers for NodeExecutorService
 */
public interface NodeExecutorServiceProfile {
    /**
     * @return default local provider name
     */
    String getDefaultLocalProvider();

    /**
     * @return default remote provider name
     */
    String getDefaultRemoteProvider();

    /**
     * @return local provider registry
     */
    Map<String, Class<? extends NodeExecutor>> getLocalRegistry();
}
