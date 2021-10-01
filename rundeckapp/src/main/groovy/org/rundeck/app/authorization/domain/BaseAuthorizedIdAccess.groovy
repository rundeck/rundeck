package org.rundeck.app.authorization.domain

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import groovy.transform.CompileStatic
import org.rundeck.core.auth.access.AuthorizedIdAccess
import org.rundeck.core.auth.access.AuthorizedIdResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseAuthorizedIdAccess<D, A extends AuthorizedIdResource<D, ID>, ID>
    implements AuthorizedIdAccess<D, A, ID> {

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor

}
