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
class AppAuthorizedSystem extends BaseSingletonAuthorizedResource implements AuthorizedSystem {
    final String resourceTypeName = 'System'
    final Map<String, String> authresMapForSingleton = AuthConstants.RESOURCE_TYPE_SYSTEM

    public static final AccessActions APP_CONFIGURE =
        AccessLevels.any(
            [AuthConstants.ACTION_CONFIGURE],
            AccessLevels.ALL_ADMIN
        )

    public static final AccessActions OPS_ENABLE_EXECUTION =
        AccessLevels.any(
            [AuthConstants.ACTION_ENABLE_EXECUTIONS],
            AccessLevels.OPS_ADMIN
        )
    public static final AccessActions OPS_DISABLE_EXECUTION =
        AccessLevels.any(
            [AuthConstants.ACTION_DISABLE_EXECUTIONS],
            AccessLevels.OPS_ADMIN
        )

    AppAuthorizedSystem(
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
