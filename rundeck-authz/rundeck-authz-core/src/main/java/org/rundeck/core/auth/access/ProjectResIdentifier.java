package org.rundeck.core.auth.access;

/**
 * Identifies a resource by ID within a project
 */
public interface ProjectResIdentifier
        extends ProjectIdentifier
{
    String getId();
}
