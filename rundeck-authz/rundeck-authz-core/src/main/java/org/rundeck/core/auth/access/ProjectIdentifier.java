package org.rundeck.core.auth.access;

/**
 * Identifies a project
 */
public interface ProjectIdentifier {
    /**
     * @return project name
     */
    String getProject();
}
