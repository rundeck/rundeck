package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.ExecutionReference;
import com.dtolabs.rundeck.core.plugins.PluginConfigSet;

public interface IExecutionLifecyclePluginService {
    /**
     * Creates a handler with configured plugins for the execution reference, may return null if no plugins are configured,
     * or job plugins are disabled
     *
     * @param configurations     configurations
     * @param executionReference reference
     * @return handler to process events for the execution, or null
     */
    ExecutionLifecyclePluginHandler getExecutionHandler(PluginConfigSet configurations, ExecutionReference executionReference);

}
