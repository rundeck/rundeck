package com.dtolabs.rundeck.core.authorization;

import java.util.List;

/**
 * Auth context with a name and role list
 */
public interface UserAndRolesAuthContext extends NamedAuthContext, UserAndRoles {
    /**
     * @param authorization additional authorization
     *
     * @return new AuthContext combined with the additional authorization
     */
    UserAndRolesAuthContext combineWith(Authorization authorization);
}
