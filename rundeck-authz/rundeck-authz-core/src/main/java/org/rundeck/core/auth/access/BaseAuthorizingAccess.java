package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.AuthResource;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import lombok.Getter;
import org.rundeck.core.auth.app.NamedAuthRequest;

import javax.security.auth.Subject;

/**
 * Base type implementing {@link AuthorizingAccess}
 */
public abstract class BaseAuthorizingAccess
        implements AuthorizingAccess
{
    @Getter private final AuthContextProcessor rundeckAuthContextProcessor;
    @Getter private final Subject subject;
    private final NamedAuthProvider namedAuthActions;

    public BaseAuthorizingAccess(
            final AuthContextProcessor rundeckAuthContextProcessor,
            final Subject subject,
            final NamedAuthProvider namedAuthActions
    )
    {
        this.rundeckAuthContextProcessor = rundeckAuthContextProcessor;
        this.subject = subject;
        this.namedAuthActions = namedAuthActions;
    }


    private UserAndRolesAuthContext authContext = null;

    public UserAndRolesAuthContext getAuthContext() {
        if (null == authContext) {
            authContext = getRundeckAuthContextProcessor().getAuthContextForSubject(getSubject());
        }
        return authContext;
    }


    @Override
    public void authorizeNamed(NamedAuthRequest namedAuthRequest)
            throws UnauthorizedAccess, NotFound
    {
        authorize(getNamedAuth(namedAuthRequest).withDescription(namedAuthRequest.getDescription()));
    }

    public AuthActions getNamedAuth(NamedAuthRequest namedAuthRequest) {
        return namedAuthActions.getNamedAuth(namedAuthRequest.getAuthGroup(), namedAuthRequest.getNamedAuth());
    }

    @Override
    public boolean isAuthorized(NamedAuthRequest namedAuthRequest) throws NotFound {
        return isAuthorized(getNamedAuth(namedAuthRequest).withDescription(namedAuthRequest.getDescription()));
    }
    @Override
    public void authorize(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        if (!isAuthorized(actions)) {
            throw new UnauthorizedAccess(
                    actions.getDescription(),
                    getResourceTypeName(),
                    getResourceIdent()
            );
        }
    }

    @Override
    public boolean isAuthorized(final AuthActions actions) throws NotFound {
        AuthResource authResource = getAuthResource();
        if (authResource.getContext() != AuthResource.Context.System) {
            throw new IllegalStateException("Cannot authorize Project-level resource without a project name");
        }
        return getRundeckAuthContextProcessor().authorizeApplicationResourceAny(
                getAuthContext(),
                authResource.getResourceMap(),
                actions.getActions()
        );
    }

    /**
     * @return authorization resource map for the resource
     */
    protected abstract AuthResource getAuthResource() throws NotFound;

    /**
     * @return resource type name
     */
    protected abstract String getResourceTypeName();

    /**
     * @return primary ID value
     */
    protected abstract String getResourceIdent();

}
