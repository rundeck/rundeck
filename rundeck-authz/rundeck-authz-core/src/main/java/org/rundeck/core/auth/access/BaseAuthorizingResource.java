package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.AuthResource;
import org.rundeck.core.auth.app.RundeckAccess;

import javax.security.auth.Subject;

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
    protected abstract T retrieve();

    /**
     * @return resource or null if it does not exist
     */
    protected abstract boolean exists();

    @Override
    public Accessor<T> accessor(final AuthActions actions) {
        return new AccessorImpl<T>(actions, this::requireActions, this::isAuthorized, this::retrieve);
    }

    @Override
    public T access(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        return accessor(actions).getResource();
    }

    @Override
    public void authorize(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        requireActions(actions);
    }

    @Override
    protected AuthResource getAuthResource() throws NotFound {
        T res = retrieve();
        if (res == null) {
            throw new NotFound(getResourceTypeName(), getResourceIdent());
        }
        return getAuthResource(res);
    }

    public T requireActions(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        if (!isAuthorized(actions)) {
            throw new UnauthorizedAccess(
                    actions.getDescription(),
                    getResourceTypeName(),
                    getResourceIdent()
            );
        }
        return retrieve();
    }


    @Override
    public Locator<T> getLocator() {
        return new Locator<T>() {
            @Override
            public T getResource() throws NotFound {
                T retrieve = retrieve();
                if (retrieve == null) {
                    throw new NotFound(getResourceTypeName(), getResourceIdent());
                }
                return retrieve;
            }

            @Override
            public boolean isExists() {
                return exists();
            }
        };
    }

    @Override
    public T getRead() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.General.APP_READ);
    }

    @Override
    public T getAppAdmin() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.General.APP_ADMIN);
    }

    @Override
    public T getOpsAdmin() throws UnauthorizedAccess, NotFound {
        return access(RundeckAccess.General.OPS_ADMIN);
    }


}
