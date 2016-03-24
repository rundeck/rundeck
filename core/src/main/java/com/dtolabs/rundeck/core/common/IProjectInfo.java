package com.dtolabs.rundeck.core.common;

/**
 * Descriptive metadata about a project
 */
public interface IProjectInfo {
    String getDescription();

    /**
     * @return readme text
     */
    String getReadme();

    /**
     * @return preprocessed readme HTML, or null to auto post-process
     */
    String getReadmeHTML();
    String getMotd();

    /**
     * @return preprocessed motd HTML, or null to auto post-process
     */
    String getMotdHTML();
}
