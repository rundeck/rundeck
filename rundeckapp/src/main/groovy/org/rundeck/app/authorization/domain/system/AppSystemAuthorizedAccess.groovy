package org.rundeck.app.authorization.domain.system

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAuthorizedAccess
import org.rundeck.app.authorization.domain.BaseAuthorizedIdAccess

import javax.security.auth.Subject

@CompileStatic
class AppSystemAuthorizedAccess extends BaseAuthorizedAccess<Void, AuthorizedSystem> implements SystemAuthorizedAccess {
    @Override
    AuthorizedSystem accessResource(final Subject subject) {
        return new AppAuthorizedSystem(rundeckAuthContextProcessor, subject)
    }
}
