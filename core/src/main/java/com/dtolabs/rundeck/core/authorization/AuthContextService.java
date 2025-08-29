package com.dtolabs.rundeck.core.authorization;

import org.rundeck.app.spi.AppService;

/**
 * Provides the current UserAndRolesAuthContext
 */
public interface AuthContextService
        extends AppService
{
    /**
     * @return the current UserAndRolesAuthContext
     */
    UserAndRolesAuthContext getAuthContext();
}
