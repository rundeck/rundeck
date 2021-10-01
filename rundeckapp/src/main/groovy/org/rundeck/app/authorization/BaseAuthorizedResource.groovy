package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic

import javax.security.auth.Subject

@CompileStatic
abstract class BaseAuthorizedResource<T, ID> implements AuthorizedResource<T> {
    final AppAuthContextProcessor rundeckAuthContextProcessor
    final Subject subject
    final ID identifier

    BaseAuthorizedResource(
        final AppAuthContextProcessor rundeckAuthContextProcessor,
        final Subject subject,
        final ID identifier
    ) {
        this.rundeckAuthContextProcessor = rundeckAuthContextProcessor
        this.subject = subject
        this.identifier = identifier
    }

    T requireRead() throws UnauthorizedAccess, NotFound {
        return requireActions(AccessLevels.APP_READ)
    }

    boolean canRead() throws NotFound {
        return canPerform(AccessLevels.APP_READ)
    }

    T requireDelete() throws UnauthorizedAccess, NotFound {
        return requireActions(AccessLevels.APP_DELETE)
    }

    boolean canDelete() throws NotFound {
        return canPerform(AccessLevels.APP_DELETE)
    }

    T requireAppAdmin() throws UnauthorizedAccess, NotFound {
        return requireActions(AccessLevels.APP_ADMIN)
    }

    boolean canAppAdmin() throws NotFound {
        return canPerform(AccessLevels.APP_ADMIN)
    }

    T requireOpsAdmin() throws UnauthorizedAccess, NotFound {
        return requireActions(AccessLevels.OPS_ADMIN)
    }

    boolean canOpsAdmin() throws NotFound {
        return canPerform(AccessLevels.OPS_ADMIN)
    }

    /**
     *
     * @param resource
     * @return constructed authorization map for the resource
     */
    protected abstract Map authresMapForResource(T resource)

    /**
     *
     * @param id
     * @return resource
     */
    protected abstract T retrieve(ID id)

    /**
     *
     * @return resource type name
     */
    protected abstract String getResourceTypeName()

    /**
     *
     * @param id
     * @return primary ID value
     */
    protected abstract String getPrimary(ID id)
    /**
     *
     * @param resource
     * @return project name for resource, or ID, or null
     */
    protected abstract String getProject(T resource)

    @Override
    T requireActions(final AccessActions actions) throws UnauthorizedAccess, NotFound {
        T res = retrieve(identifier)
        if (!res) {
            throw new NotFound(getResourceTypeName(), getPrimary(identifier))
        }
        UserAndRolesAuthContext authContext = getAuthContext(res)
        String projectLevel = getProject(res)
        boolean authorized = false
        List<String> actionSet = ['none']
        if (actions.requiredActions) {
            actionSet = actions.requiredActions
            if (projectLevel) {
                authorized = rundeckAuthContextProcessor.authorizeProjectResourceAll(
                    authContext,
                    authresMapForResource(res),
                    actionSet,
                    projectLevel
                )
            } else {
                authorized = rundeckAuthContextProcessor
                    .authorizeApplicationResourceAll(
                        authContext,
                        authresMapForResource(res),
                        actionSet
                    )
            }
        } else if (actions.anyActions) {
            actionSet = actions.anyActions
            if (projectLevel) {
                authorized = rundeckAuthContextProcessor.authorizeProjectResourceAny(
                    authContext,
                    authresMapForResource(res),
                    actionSet,
                    projectLevel
                )
            } else {
                authorized = rundeckAuthContextProcessor
                    .authorizeApplicationResourceAny(
                        authContext,
                        authresMapForResource(res),
                        actionSet
                    )
            }
        }
        if (!authorized) {
            throw new UnauthorizedAccess(
                actionSet.first(),
                resourceTypeName,
                getPrimary(identifier)
            )
        }
        return res
    }

    @Override
    UserAndRolesAuthContext getAuthContext() {
        getAuthContext(retrieve(identifier))
    }

    UserAndRolesAuthContext getAuthContext(T resource) {
        String project = null
        if (identifier instanceof ProjectIdentifier) {
            project = identifier.project
        } else {
            project = getProject(resource)
        }
        if (project) {
            return rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(subject, project)
        }
        return rundeckAuthContextProcessor.getAuthContextForSubject(subject)
    }

    @Override
    boolean canPerform(final AccessActions actions) throws NotFound {
        try {
            requireActions(actions)
        } catch (UnauthorizedAccess ignored) {
            return false
        }
        return true
    }
}
