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
        UserAndRolesAuthContext authContext = getAuthContext()
        T res = retrieve(identifier)
        if (!res) {
            throw new NotFound(getResourceTypeName(), getPrimary(identifier))
        }
        String projectLevel = getProject(res)
        if (actions.requiredActions) {
            if (projectLevel) {
                if (!rundeckAuthContextProcessor.authorizeProjectResourceAll(
                    authContext,
                    authresMapForResource(res),
                    actions.requiredActions,
                    projectLevel
                )) {
                    throw new UnauthorizedAccess(
                        actions.requiredActions.first(),
                        resourceTypeName,
                        getPrimary(identifier)
                    )
                }
            } else {
                if (!rundeckAuthContextProcessor
                    .authorizeApplicationResourceAll(
                        authContext,
                        authresMapForResource(res),
                        actions.requiredActions
                    )) {
                    throw new UnauthorizedAccess(
                        actions.requiredActions.first(),
                        resourceTypeName,
                        getPrimary(identifier)
                    )
                }
            }

        } else if (actions.anyActions) {
            if (projectLevel) {
                if (!rundeckAuthContextProcessor.authorizeProjectResourceAny(
                    authContext,
                    authresMapForResource(res),
                    actions.requiredActions,
                    projectLevel
                )) {
                    throw new UnauthorizedAccess(
                        actions.requiredActions.first(),
                        resourceTypeName,
                        getPrimary(identifier)
                    )
                }
            } else {
                if (!rundeckAuthContextProcessor
                    .authorizeApplicationResourceAny(
                        authContext,
                        authresMapForResource(res),
                        actions.requiredActions
                    )) {
                    throw new UnauthorizedAccess(
                        actions.requiredActions.first(),
                        resourceTypeName,
                        getPrimary(identifier)
                    )
                }
            }
        } else {
            throw new UnauthorizedAccess('none', resourceTypeName, getPrimary(identifier))
        }
        return res
    }

    public UserAndRolesAuthContext getAuthContext() {
        AuthContext authContext
        if (identifier instanceof ProjectIdentifier) {
            authContext = rundeckAuthContextProcessor.getAuthContextForSubjectAndProject(subject, identifier.project)
        } else {
            authContext = rundeckAuthContextProcessor.getAuthContextForSubject(subject)
        }
        authContext
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

    String getProjectFromId() {
        if (identifier instanceof ProjectIdentifier) {
            return identifier.project
        }
        return null
    }
}
