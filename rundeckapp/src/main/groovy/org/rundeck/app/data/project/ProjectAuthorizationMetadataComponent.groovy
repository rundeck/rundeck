package org.rundeck.app.data.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.project.ProjectMetadataComponent
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class ProjectAuthorizationMetadataComponent implements ProjectMetadataComponent {
    public static final String NAME = 'authz'
    public static final Map<String, List<String>> PROJ_AUTH_CHECK_SET = Collections.unmodifiableMap(
        [
            (AuthConstants.TYPE_JOB): [
                AuthConstants.ACTION_CREATE,
                AuthConstants.ACTION_DELETE
            ]
        ]
    )

    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor

    @Override
    Set<String> getAvailableMetadataNames() {
        return [NAME].toSet()
    }

    @Override
    List<ComponentMeta> getMetadataForProject(
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        if (!names.contains(NAME) && !names.contains('*')) {
            return null
        }
        return [ComponentMeta.with(NAME, getAuthzMeta(project, authContext))]
    }

    Map<String, Object> getAuthzMeta(String project, UserAndRolesAuthContext authContext) {
        //authorization for generic types (job)
        Map<String, Object> typeAuthz = [:]
        for (String type : PROJ_AUTH_CHECK_SET.keySet()) {
            def resource = AuthorizationUtil.resourceType(type)
            typeAuthz[type] = getAuthzMeta(resource, PROJ_AUTH_CHECK_SET.get(type), project, authContext)
        }

        return [types: typeAuthz] as Map<String, Object>
    }

    Map<String, Object> getAuthzMeta(
        Map<String, String> authResource,
        List<String> actions,
        String project,
        UserAndRolesAuthContext authContext
    ) {
        def authz = rundeckAuthContextProcessor.authorizeProjectResources(
            authContext,
            [authResource].toSet(),
            actions.toSet(),
            project
        )
        return [
            authorizations: authz.collectEntries { [it.action, it.authorized] }
        ] as Map<String, Object>
    }
}
