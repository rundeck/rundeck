package org.rundeck.app.authorization.domain.execution

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.AppProjectResIdentifier
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizer
import org.rundeck.core.auth.access.ProjectResIdentifier
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.RundeckAccess
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution

import javax.security.auth.Subject

@CompileStatic
class AppExecutionResourceAuthorizer extends BaseResourceIdAuthorizer<Execution, AuthorizingExecution, ProjectResIdentifier> {

    @Autowired
    NamedAuthProvider namedAuthProvider;

    AuthorizingExecution getAuthorizingResource(final Subject subject, String execId, String project) {
        return new AppAuthorizingExecution(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            new AppProjectResIdentifier(project, execId)
        )
    }

    @Override
    AuthorizingExecution getAuthorizingResource(final Subject subject, final ResIdResolver resolver) {
        return getAuthorizingResource(
            subject,
            resolver.idForTypeOptional(RundeckAccess.Project.TYPE).orElse(null),
            resolver.idForType(RundeckAccess.Execution.TYPE)
        )
    }
}
