package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import rundeck.Execution

interface AuthorizedResource<T> {
    /**
     * @return auth context
     */
    UserAndRolesAuthContext getAuthContext()

    /**
     *
     * @return true if resource exists
     */
    boolean exists()

    /**
     * Retrieve resource and require actions
     * @param actions
     * @return resource
     * @throws UnauthorizedAccess if unauthorized for required actions
     * @throws NotFound if not found
     */
    T requireActions(AccessActions actions) throws UnauthorizedAccess, NotFound

    /**
     * Test access to action requirements
     * @param actions
     * @return true if access is authorized
     * @throws NotFound if not found
     */
    boolean canPerform(AccessActions actions) throws NotFound

    /**
     * Retrieve resource and require READ access
     * @return resource
     * @throws UnauthorizedAccess if not authorized
     * @throws NotFound if not found
     */
    T requireRead() throws UnauthorizedAccess, NotFound

    /**
     *
     * @return true if READ access is allowed
     * @throws NotFound if not found
     */
    boolean canRead() throws NotFound

    T requireDelete() throws UnauthorizedAccess, NotFound

    boolean canDelete() throws NotFound

    T requireAppAdmin() throws UnauthorizedAccess, NotFound

    boolean canAppAdmin() throws NotFound

    T requireOpsAdmin() throws UnauthorizedAccess, NotFound

    boolean canOpsAdmin() throws NotFound
}