package org.rundeck.app.authorization.domain.system

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.*

import javax.security.auth.Subject

/**
 * Authorized access to System singleton resource
 */
@CompileStatic
class AppAuthorizingSystem extends BaseSingletonAuthorizingResource implements AuthorizingSystem {
    final String resourceTypeName = 'System'
    final String resourceIdent = 'resource'
    final Map<String, String> authresMapForSingleton = AuthConstants.RESOURCE_TYPE_SYSTEM

    public static final AuthActions APP_CONFIGURE =
        AccessLevels.any(
            [AuthConstants.ACTION_CONFIGURE],
            AccessLevels.ALL_ADMIN
        )

    public static final AuthActions OPS_ENABLE_EXECUTION =
        AccessLevels.any(
            [AuthConstants.ACTION_ENABLE_EXECUTIONS],
            AccessLevels.OPS_ADMIN
        )
    public static final AuthActions OPS_DISABLE_EXECUTION =
        AccessLevels.any(
            [AuthConstants.ACTION_DISABLE_EXECUTIONS],
            AccessLevels.OPS_ADMIN
        )

    AppAuthorizingSystem(
        final AuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject
    ) {
        super(rundeckAuthContextProcessor, subject)
    }

    Accessor<Singleton> getConfigure() {
        return access(APP_CONFIGURE)
    }

    Accessor<Singleton> getOpsEnableExecution() {
        return access(OPS_ENABLE_EXECUTION)
    }

    Accessor<Singleton> getOpsDisableExecution() {
        return access(OPS_DISABLE_EXECUTION)
    }

}
