package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.AuthResource;

import javax.security.auth.Subject;
import java.util.Optional;

/**
 * Provides base implementation for authorized resource of a specific type without ID (singleton)
 *
 * @param <T> resource type
 */
public abstract class BaseAuthorizingResource<T>
        extends BaseAuthorizingAccess
        implements AuthorizingResource<T>
{

    public BaseAuthorizingResource(
            final AuthContextProcessor rundeckAuthContextProcessor,
            final Subject subject,
            final NamedAuthProvider namedAuthActions
    )
    {
        super(rundeckAuthContextProcessor, subject, namedAuthActions);
    }

    /**
     * @return constructed authorization map for the resource
     */
    protected abstract AuthResource getAuthResource(T resource);

    /**
     * @return resource type name
     */
    protected abstract String getResourceTypeName();

    /**
     * @return primary ID value
     */
    protected abstract String getResourceIdent();

    /**
     * @return resource or null if it does not exist
     */
    protected abstract Optional<T> retrieve();

    /**
     * @return resource or null if it does not exist
     */
    protected abstract boolean exists();


    @Override
    public T access(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        return requireActions(actions);
    }

    @Override
    public void authorize(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        requireActions(actions);
    }

    @Override
    protected AuthResource getAuthResource() throws NotFound {
        Optional<T> res = retrieve();
        return getAuthResource(
                res.orElseThrow(() -> new NotFound(getResourceTypeName(), getResourceIdent()))
        );
    }

    public T requireActions(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        if (!isAuthorized(actions)) {
            throw new UnauthorizedAccess(
                    actions.getDescription(),
                    getResourceTypeName(),
                    getResourceIdent()
            );
        }
        return getResource();
    }

    @Override
    public T getResource() throws NotFound {
        Optional<T> retrieve = retrieve();
        return retrieve.orElseThrow(() -> new NotFound(getResourceTypeName(), getResourceIdent()));
    }
}
