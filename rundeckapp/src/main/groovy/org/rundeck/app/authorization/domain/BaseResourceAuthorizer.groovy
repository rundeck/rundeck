package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceAuthorizer
import org.rundeck.core.auth.access.AuthorizingResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseResourceAuthorizer<D, A extends AuthorizingResource<D>>
    implements ResourceAuthorizer<D, A> {


    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor
}
