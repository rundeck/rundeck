package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AccessAuthorizer
import org.rundeck.core.auth.access.AuthorizingAccess
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseAccessAuthorizer<A extends AuthorizingAccess>
    implements AccessAuthorizer<A> {

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor
}
