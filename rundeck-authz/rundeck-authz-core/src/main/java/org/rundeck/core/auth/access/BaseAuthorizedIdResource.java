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
public abstract class BaseAuthorizedIdResource<T, ID>
        extends BaseAuthorizedResource<T>
        implements AuthorizedIdResource<T, ID>
{
    @Getter private final ID identifier;

    public BaseAuthorizedIdResource(
            final AuthContextProcessor rundeckAuthContextProcessor,
            final Subject subject,
            final ID identifier
    )
    {
        super(rundeckAuthContextProcessor, subject);
        this.identifier = identifier;
    }

    /**
     * @return primary ID value
     */
    protected abstract String getPrimaryIdComponent();

    /**
     * @return project name for resource, or from ID, or null
     */
    protected abstract String getProject(T resource);


    @Override
    public UserAndRolesAuthContext getAuthContext() {
        return getAuthContext(getProject(retrieve()));
    }

    public UserAndRolesAuthContext getAuthContext(String project) {

        if (project != null) {
            return getRundeckAuthContextProcessor().getAuthContextForSubjectAndProject(getSubject(), project);
        }

        return getRundeckAuthContextProcessor().getAuthContextForSubject(getSubject());
    }

    @Override
    public T requireActions(final AccessActions actions) throws UnauthorizedAccess, NotFound {
        /*
         *
         */
        T res = retrieve();
        if (res == null) {
            throw new NotFound(getResourceTypeName(), getPrimaryIdComponent());
        }

        String projectLevel = getProject(res);

        if (projectLevel == null) {
            return super.requireActions(actions);
        }

        UserAndRolesAuthContext authContext = getAuthContext(projectLevel);

        boolean authorized = false;
        List<String> actionSet;
        if (actions.getRequiredActions() != null && actions.getRequiredActions().size() > 0) {
            actionSet = actions.getRequiredActions();
            authorized =
                    getRundeckAuthContextProcessor().authorizeProjectResourceAll(
                            authContext,
                            authresMapForResource(res),
                            actionSet,
                            projectLevel
                    );


        } else if (actions.getAnyActions() != null && actions.getAnyActions().size() > 0) {
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
            throw new UnauthorizedAccess(actionSet.get(0), getResourceTypeName(), getPrimaryIdComponent());
        }

        return res;
    }


    public Accessor<T> getDelete() {
        return access(AccessLevels.APP_DELETE);
    }

}
