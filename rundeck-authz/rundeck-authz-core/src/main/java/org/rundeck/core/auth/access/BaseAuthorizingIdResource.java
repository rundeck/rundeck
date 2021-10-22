package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import lombok.Getter;

import javax.security.auth.Subject;
import java.util.*;

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
            final ID identifier
    )
    {
        super(rundeckAuthContextProcessor, subject);
        this.identifier = identifier;
    }


    /**
     * @return project name for resource, or from ID, or null
     */
    protected abstract String getProject(T resource);


    @Override
    public UserAndRolesAuthContext getAuthContext() {
        return getAuthContext(getProject(retrieve()));
    }

    private UserAndRolesAuthContext projectAuthContext = null;

    public UserAndRolesAuthContext getAuthContext(String project) {
        if (null == projectAuthContext) {
            projectAuthContext =
                    getRundeckAuthContextProcessor().getAuthContextForSubjectAndProject(getSubject(), project);
        }
        return projectAuthContext;
    }

    @Override
    public T requireActions(final AuthActions actions, String description) throws UnauthorizedAccess, NotFound {
        /*
         *
         */
        T res = retrieve();
        if (res == null) {
            throw new NotFound(getResourceTypeName(), getResourceIdent());
        }

        String projectLevel = getProject(res);

        if (projectLevel == null) {
            return super.requireActions(actions, description);
        }

        UserAndRolesAuthContext authContext = getAuthContext(projectLevel);

        boolean authorized = false;
        List<String> actionSet;
        if (actions.getAnyActions() != null && actions.getAnyActions().size() > 0) {
            actionSet = actions.getAnyActions();
            authorized =
                    getRundeckAuthContextProcessor().authorizeProjectResourceAny(
                            authContext,
                            authresMapForResource(res),
                            actionSet,
                            projectLevel
                    );
        } else {
            throw new IllegalArgumentException("Access actions were not defined");
        }

        if (!authorized) {
            throw new UnauthorizedAccess(description!=null?description:actionSet.get(0), getResourceTypeName(), getResourceIdent());
        }

        return res;
    }


    public Accessor<T> getDelete() {
        return access(AccessLevels.APP_DELETE);
    }

}
