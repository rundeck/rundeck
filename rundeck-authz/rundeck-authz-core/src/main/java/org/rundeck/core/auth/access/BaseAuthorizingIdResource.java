package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.AuthResource;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import lombok.Getter;
import org.rundeck.core.auth.app.RundeckAccess;

import javax.security.auth.Subject;

/**
 * Provides base implementation for authorized resource of specific type and ID
 *
 * @param <T>  resource type
 * @param <ID> ID type
 */
public abstract class BaseAuthorizingIdResource<T, ID>
        extends BaseAuthorizingResource<T>
        implements AuthorizingIdResource<T, ID>
{
    @Getter private final ID identifier;

    public BaseAuthorizingIdResource(
            final AuthContextProcessor rundeckAuthContextProcessor,
            final Subject subject,
            final NamedAuthProvider namedAuthActions,
            final ID identifier
    )
    {
        super(rundeckAuthContextProcessor, subject, namedAuthActions);
        this.identifier = identifier;
    }


    /**
     * @return project name for resource, or from ID, or null
     */
    protected abstract String getProject();

    /**
     * @return true if project value must be set, false otherwise
     */
    protected boolean isAuthContextWithProject() {
        return true;
    }


    @Override
    public UserAndRolesAuthContext getAuthContext() {
        return getAuthContext(getProject());
    }

    private UserAndRolesAuthContext loadedAuthContext = null;

    public UserAndRolesAuthContext getAuthContext(String project) {
        if (null == loadedAuthContext) {
            if (isAuthContextWithProject()) {
                loadedAuthContext =
                        getRundeckAuthContextProcessor().getAuthContextForSubjectAndProject(getSubject(), project);
            } else {
                loadedAuthContext = getRundeckAuthContextProcessor().getAuthContextForSubject(getSubject());
            }
        }
        return loadedAuthContext;
    }

    @Override
    public boolean isAuthorized(final AuthActions actions) throws NotFound {
        T res = getResource();

        String projectLevel = getProject();
        AuthResource authResource = getAuthResource(res);

        if (projectLevel == null || authResource.getContext() == AuthResource.Context.System) {
            return super.isAuthorized(actions);
        }


        UserAndRolesAuthContext authContext = getAuthContext(projectLevel);

        return getRundeckAuthContextProcessor().authorizeProjectResourceAny(
                authContext,
                authResource.getResourceMap(),
                actions.getActions(),
                projectLevel
        );
    }
}
