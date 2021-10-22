package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import lombok.Getter;

import javax.security.auth.Subject;
import java.util.List;
import java.util.Map;

/**
 * Provides base implementation for authorized resource of a specific type without ID (singleton)
 *
 * @param <T> resource type
 */
public abstract class BaseAuthorizingResource<T>
        implements AuthorizingResource<T>
{

    @Getter private final AuthContextProcessor rundeckAuthContextProcessor;
    @Getter private final Subject subject;

    public BaseAuthorizingResource(final AuthContextProcessor rundeckAuthContextProcessor, final Subject subject) {
        this.rundeckAuthContextProcessor = rundeckAuthContextProcessor;
        this.subject = subject;
    }

    /**
     * @return constructed authorization map for the resource
     */
    protected abstract Map<String, String> authresMapForResource(T resource);

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

    private UserAndRolesAuthContext authContext = null;

    public UserAndRolesAuthContext getAuthContext() {
        if (null == authContext) {
            authContext = getRundeckAuthContextProcessor().getAuthContextForSubject(getSubject());
        }
        return authContext;
    }

    @Override
    public Accessor<T> access(final AuthActions actions) {
        return new AccessorImpl<T>(actions, this::requireActions, this::canPerform, this::retrieve);
    }

    public T requireActions(final AuthActions actions) throws UnauthorizedAccess, NotFound {
        T res = retrieve();
        if (res == null) {
            throw new NotFound(getResourceTypeName(), getResourceIdent());
        }

        UserAndRolesAuthContext authContext = getAuthContext();
        boolean authorized = false;
        List<String> actionSet;
        if (actions.getAnyActions() != null && actions.getAnyActions().size() > 0) {
            actionSet = actions.getAnyActions();
            authorized =
                    getRundeckAuthContextProcessor().authorizeApplicationResourceAny(
                            authContext,
                            authresMapForResource(res),
                            actionSet
                    );

        } else {
            throw new IllegalArgumentException("Access actions were not defined");
        }

        if (!authorized) {
            throw new UnauthorizedAccess(actionSet.get(0), getResourceTypeName(), getResourceIdent());
        }

        return res;
    }

    public boolean canPerform(final AuthActions actions) throws NotFound {
        try {
            requireActions(actions);
        } catch (UnauthorizedAccess ignored) {
            return false;
        }

        return true;
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
    public Accessor<T> getRead() {
        return access(AccessLevels.APP_READ);
    }

    @Override
    public Accessor<T> getAppAdmin() {
        return access(AccessLevels.APP_ADMIN);
    }

    @Override
    public Accessor<T> getOpsAdmin() {
        return access(AccessLevels.OPS_ADMIN);
    }


}
