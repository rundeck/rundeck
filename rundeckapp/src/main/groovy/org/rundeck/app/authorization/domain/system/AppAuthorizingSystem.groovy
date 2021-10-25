package org.rundeck.app.authorization.domain.system

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.*

import javax.security.auth.Subject

import static org.rundeck.core.auth.access.AccessLevels.action

/**
 * Authorized access to System singleton resource
 */
@CompileStatic
class AppAuthorizingSystem extends BaseSingletonAuthorizingResource implements AuthorizingSystem {
    final String resourceTypeName = 'System'
    final String resourceIdent = 'resource'
    final Map<String, String> authresMapForSingleton = AuthConstants.RESOURCE_TYPE_SYSTEM


    AppAuthorizingSystem(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject
    ) {
        super(rundeckAuthContextProcessor, subject)
    }

    Singleton getReadOrAnyAdmin() throws UnauthorizedAccess, NotFound {
        return access(READ_OR_ANY_ADMIN)
    }

    Singleton getReadOrOpsAdmin() throws UnauthorizedAccess, NotFound {
        return access(READ_OR_OPS_ADMIN)
    }

    Singleton getConfigure() throws UnauthorizedAccess, NotFound {
        return access(APP_CONFIGURE)
    }

    Singleton getOpsEnableExecution() throws UnauthorizedAccess, NotFound {
        return access(OPS_ENABLE_EXECUTION)
    }

    Singleton getOpsDisableExecution() throws UnauthorizedAccess, NotFound {
        return access(OPS_DISABLE_EXECUTION)
    }

}
