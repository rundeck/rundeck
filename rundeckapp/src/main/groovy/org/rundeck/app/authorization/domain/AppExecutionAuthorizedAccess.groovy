package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic

import javax.security.auth.Subject

@CompileStatic
class AppExecutionAuthorizedAccess extends BaseAuthorizedAccess implements ExecutionAuthorizedAccess {
    @Override
    AuthorizedExecution accessResource(final Subject subject, final ExecIdentifier identifier) {
        return new AppAuthorizedExecution(rundeckAuthContextProcessor, subject, identifier)
    }

}
