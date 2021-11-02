package org.rundeck.app.authorization.domain.system

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.*

import javax.security.auth.Subject

import static org.rundeck.core.auth.access.AccessLevels.action

/**
 * Authorized access to System singleton resource
 */
@CompileStatic
class AppAuthorizingSystem extends BaseAuthorizingAccess implements AuthorizingSystem {
    final String resourceTypeName = 'System'
    final String resourceIdent = 'resource'
    final AuthResource authResource = AuthorizationUtil.systemAuthResource(AuthConstants.RESOURCE_TYPE_SYSTEM)

    AppAuthorizingSystem(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject
    ) {
        super(rundeckAuthContextProcessor, subject)
    }


    void authorizeReadOrAnyAdmin() throws UnauthorizedAccess, NotFound {
        authorize(READ_OR_ANY_ADMIN)
    }

    void authorizeReadOrOpsAdmin() throws UnauthorizedAccess, NotFound {
        authorize(READ_OR_OPS_ADMIN)
    }

    void authorizeConfigure() throws UnauthorizedAccess, NotFound {
        authorize(APP_CONFIGURE)
    }

    void authorizeOpsEnableExecution() throws UnauthorizedAccess, NotFound {
        authorize(OPS_ENABLE_EXECUTION)
    }

    void authorizeOpsDisableExecution() throws UnauthorizedAccess, NotFound {
        authorize(OPS_DISABLE_EXECUTION)
    }

}
