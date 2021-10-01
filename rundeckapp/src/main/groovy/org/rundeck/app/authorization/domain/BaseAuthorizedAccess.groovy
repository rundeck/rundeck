package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.AuthorizedAccess
import org.rundeck.app.authorization.AuthorizedResource
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
abstract class BaseAuthorizedAccess<D, A extends AuthorizedResource<D>, ID> implements AuthorizedAccess<D, A, ID> {

    @Autowired
    AppAuthContextProcessor rundeckAuthContextProcessor

}
