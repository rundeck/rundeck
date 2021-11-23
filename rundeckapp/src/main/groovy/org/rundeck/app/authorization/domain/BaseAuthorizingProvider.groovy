package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizingAccessProvider
import org.rundeck.core.auth.access.AuthorizingAccess
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseAuthorizingProvider<A extends AuthorizingAccess>
    implements AuthorizingAccessProvider<A> {

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor
}
