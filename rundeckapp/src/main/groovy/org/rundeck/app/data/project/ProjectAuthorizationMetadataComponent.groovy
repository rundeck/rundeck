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
    public static final Map<String, List<String>> PROJ_TYPES_AUTH_CHECK_SET = Collections.unmodifiableMap(
        [
            (AuthConstants.TYPE_JOB): [
                AuthConstants.ACTION_CREATE,
                AuthConstants.ACTION_DELETE
            ]
        ]
    )

    public static final List<String> PROJ_AUTH_CHECK_ACTIONS = Collections.unmodifiableList(
        [
            AuthConstants.ACTION_CONFIGURE,
            AuthConstants.ACTION_EXPORT,
            AuthConstants.ACTION_SCM_EXPORT,
            AuthConstants.ACTION_IMPORT,
            AuthConstants.ACTION_SCM_IMPORT,
        ]
    )
    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor

    @Override
    Set<String> getAvailableMetadataNames() {
        return [NAME].toSet()
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForProject(
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        if (!names.contains(NAME) && !names.contains('*')) {
            return Optional.empty()
        }
        Map<String, Object> result = new HashMap<String, Object>()
        result.putAll(getTypeAuthzMeta(project, authContext))
        result.putAll(getProjAuthzMeta(project, authContext, PROJ_AUTH_CHECK_ACTIONS))
        return Optional.of(
            [ComponentMeta.with(NAME, result)]
        )
    }

    /**
     * Evalutes authorizations for the actions for the project resource in the system context
     * @param project
     * @param authContext
     * @param actions
     */
    Map<String, Object> getProjAuthzMeta(
        String project, UserAndRolesAuthContext authContext, List<String> actions
    ) {
        //authorization for project resource
        def results = getAuthSystemResults(
            actions,
            project,
            authContext
        )
        return [
            project: results
        ] as Map<String, Object>
    }

    Map<String, Object> getTypeAuthzMeta(String project, UserAndRolesAuthContext authContext) {
        //authorization for generic types (job)
        Map<String, Object> typeAuthz = [:]
        for (String type : PROJ_TYPES_AUTH_CHECK_SET.keySet()) {
            def resource = AuthorizationUtil.resourceType(type)
            typeAuthz[type] = getAuthProjectResults(resource, PROJ_TYPES_AUTH_CHECK_SET.get(type), project, authContext)
        }

        return [types: typeAuthz] as Map<String, Object>
    }

    Map<String, Object> getAuthProjectResults(
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
        return authz.collectEntries { [it.action, it.authorized] } as Map<String, Object>
    }

    Map<String, Object> getAuthSystemResults(
        List<String> actions,
        String project,
        UserAndRolesAuthContext authContext
    ) {
        def result = [:]
        def authResource = rundeckAuthContextProcessor.authResourceForProject(project)
        for (String action : actions) {
            result[action] = rundeckAuthContextProcessor.authorizeApplicationResource(
                authContext,
                authResource,
                action
            )
        }
        return result as Map<String, Object>
    }
}
