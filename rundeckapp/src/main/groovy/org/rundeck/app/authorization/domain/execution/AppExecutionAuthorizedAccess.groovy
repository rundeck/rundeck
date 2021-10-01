package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseAuthorizedIdAccess
import org.rundeck.core.auth.access.AuthorizedIdResource

import javax.security.auth.Subject

@CompileStatic
class AppExecutionAuthorizedAccess extends BaseAuthorizedIdAccess implements ExecutionAuthorizedAccess {
    @Override
    AuthorizedExecution accessResource(final Subject subject, final ExecIdentifier identifier) {
        return new AppAuthorizedExecution(rundeckAuthContextProcessor, subject, identifier)
    }
}
