package org.rundeck.app.authorization.domain.project

import com.dtolabs.rundeck.core.common.ProjectManager
import groovy.transform.CompileStatic
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.app.authorization.domain.BaseResourceIdAuthorizingProvider
import org.rundeck.app.data.model.v1.project.RdProject
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NamedAuthProvider
import org.rundeck.core.auth.access.ProjectIdentifier
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.RundeckAccess
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

@CompileStatic
class AppProjectResourceAuthorizingProvider extends BaseResourceIdAuthorizingProvider<RdProject, AuthorizingProject, ProjectIdentifier> {
    @Autowired
    NamedAuthProvider namedAuthProvider
    @Autowired
    ProjectManager projectManagerService

    AuthorizingProject getAuthorizingResource(final Subject subject, String project)
        throws MissingParameter {
        return new AppAuthorizingProject(
            rundeckAuthContextProcessor,
            subject,
            namedAuthProvider,
            project,
            projectManagerService
        )
    }

    @Override
    AuthorizingProject getAuthorizingResource(final Subject subject, final ResIdResolver resolver)
        throws MissingParameter {
        return getAuthorizingResource(subject, resolver.idForType(RundeckAccess.Project.TYPE))
    }
}
