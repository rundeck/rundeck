package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer

import javax.security.auth.Subject

@CompileStatic
class AppExecutionResourceAuthorizer extends BaseResourceIdAuthorizer implements ExecutionResourceAuthorizer {
    @Override
    AuthorizingExecution accessResource(final Subject subject, final ExecIdentifier identifier) {
        return new AppAuthorizingExecution(rundeckAuthContextProcessor, subject, identifier)
    }
}
