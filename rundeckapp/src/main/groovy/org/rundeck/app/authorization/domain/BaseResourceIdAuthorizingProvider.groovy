package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.ResourceIdAuthorizingProvider
import org.rundeck.core.auth.access.AuthorizingIdResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseResourceIdAuthorizingProvider<D, A extends AuthorizingIdResource<D, ID>, ID>
    implements ResourceIdAuthorizingProvider<D, A, ID> {

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor

}
