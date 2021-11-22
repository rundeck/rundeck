package org.rundeck.app.auth.types;

import com.dtolabs.rundeck.core.common.IRundeckProject;
import org.rundeck.core.auth.access.AuthorizingIdResource;
import org.rundeck.core.auth.access.ProjectIdentifier;

/**
 * Authorizing resource interface for IRundeckProject
 */
public interface AuthorizingProject
    extends AuthorizingIdResource<IRundeckProject, String>
{
}
