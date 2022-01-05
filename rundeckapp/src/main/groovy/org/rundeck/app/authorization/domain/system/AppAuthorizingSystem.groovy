package org.rundeck.app.authorization.domain.system

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthResource
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import org.rundeck.core.auth.access.*
import org.rundeck.core.auth.app.type.AuthorizingSystem

import javax.security.auth.Subject

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
        final Subject subject,
        final NamedAuthProvider namedAuthActions
    ) {
        super(rundeckAuthContextProcessor, subject, namedAuthActions)
    }
}
