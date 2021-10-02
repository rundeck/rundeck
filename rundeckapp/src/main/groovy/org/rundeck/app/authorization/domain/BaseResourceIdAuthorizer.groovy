package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceIdAuthorizer
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseResourceIdAuthorizer<D, A extends AuthorizingIdResource<D, ID>, ID>
    implements ResourceIdAuthorizer<D, A, ID> {

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor

}
