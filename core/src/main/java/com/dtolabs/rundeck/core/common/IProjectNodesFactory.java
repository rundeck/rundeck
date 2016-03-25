package com.dtolabs.rundeck.core.common;

/**
 * Construct Project nodes for a project
 */
public interface IProjectNodesFactory {
    IProjectNodes getNodes(final String name);
    void refreshProjectNodes(String name);
}
