package org.rundeck.core.auth.app.type;

import org.rundeck.core.auth.access.AuthorizingIdResource;
import org.rundeck.core.auth.access.Singleton;

public interface AuthorizingProjectAcl
        extends AuthorizingIdResource<Singleton, String>
{
}
