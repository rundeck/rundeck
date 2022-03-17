package org.rundeck.core.auth.app.type;

import org.rundeck.core.auth.access.ProjectIdentifier;

/**
 * Identifies a resource type in a project
 */
public interface ProjectTypeIdentifier
        extends ProjectIdentifier
{
    String getType();
}
