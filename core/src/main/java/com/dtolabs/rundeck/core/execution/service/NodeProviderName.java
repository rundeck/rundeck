package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;

/**
 * Determine provider name for a Service type given a node and project
 */
public interface NodeProviderName {
    /**
     * @param node    node
     * @param project project name
     * @param type    service type class
     * @param <T>     service type
     * @return provider name to use for the type
     * @throws ExecutionServiceException
     */
    <T> String getProviderNameForNodeAndProject(final INodeEntry node, final String project, Class<T> type)
            throws ExecutionServiceException;
}
