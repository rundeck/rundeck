package com.dtolabs.rundeck.core.authorization;

import java.util.List;
import java.util.Set;

/**
 * A username and role list
 */
public interface UserAndRoles {
    /**
     * @return username
     */
    public String getUsername();

    /**
     * @return list of user roles
     */
    public Set<String> getRoles();
}
