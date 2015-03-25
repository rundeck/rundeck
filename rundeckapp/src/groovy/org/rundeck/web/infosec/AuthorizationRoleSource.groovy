package org.rundeck.web.infosec

import javax.servlet.http.HttpServletRequest

/**
 * Created by greg on 2/17/15.
 */
interface AuthorizationRoleSource {
    /**
     * @param username
     * @param request
     * @return collection of user role names
     */
    public Collection<String> getUserRoles(String username, HttpServletRequest request)
    /**
     * @return true if this source is enabled
     */
    public boolean isEnabled()
}