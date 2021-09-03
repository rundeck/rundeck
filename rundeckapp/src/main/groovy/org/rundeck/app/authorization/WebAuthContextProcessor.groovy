package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants
import rundeck.Execution
import rundeck.ScheduledExecution

import java.util.function.Function

/**
 * Throws exceptions when authorization is not allowed.
 */
@CompileStatic
class WebAuthContextProcessor implements AppAuthContextProcessor{
    @Delegate
    AppAuthContextProcessor authContextProcessor

    @Override
    boolean authorizeApplicationResourceTypeAll(
        final AuthContext authContext,
        final String resourceType,
        final Collection<String> actions
    ) {
        if(!authContextProcessor.authorizeApplicationResourceTypeAll(authContext,resourceType,actions)) {
            throw new UnauthorizedAccess(actions.join(', '), resourceType, "")
        }
        return true
    }

    @Override
    boolean authorizeApplicationResourceType(
        final AuthContext authContext,
        final String resourceType,
        final String action
    ) {
        if(!authContextProcessor.authorizeApplicationResourceType(authContext,resourceType,action)) {
            throw new UnauthorizedAccess(action, resourceType, "")
        }
        return true
    }

    @Override
    boolean authorizeApplicationResourceAny(
        final AuthContext authContext,
        final Map<String, String> resource,
        final List<String> actions
    ) {
        if(!authContextProcessor.authorizeApplicationResourceAny(authContext,resource,actions)) {
            throw new UnauthorizedAccess(actions[0], resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }

    @Override
    boolean authorizeApplicationResourceAll(
        final AuthContext authContext,
        final Map<String, String> resource,
        final Collection<String> actions
    ) {
        if(!authContextProcessor.authorizeApplicationResourceAll(authContext,resource,actions)) {
            throw new UnauthorizedAccess(actions[0], resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }


    @Override
    boolean authorizeApplicationResource(
        final AuthContext authContext,
        final Map<String, String> resource,
        final String action
    ) {
        if(!authContextProcessor.authorizeApplicationResource(authContext,resource,action)) {
            throw new UnauthorizedAccess(action, resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }

    @Override
    boolean authorizeProjectResourceAny(
        final AuthContext authContext,
        final Map<String, String> resource,
        final Collection<String> actions,
        final String project
    ) {
        if (!authContextProcessor.authorizeProjectResourceAny(authContext, resource, actions, project)) {
            throw new UnauthorizedAccess(actions[0], resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }

    @Override
    boolean authorizeProjectResourceAll(
        final AuthContext authContext,
        final Map<String, String> resource,
        final Collection<String> actions,
        final String project
    ) {
        if (!authContextProcessor.authorizeProjectResourceAll(authContext, resource, actions, project)) {
            throw new UnauthorizedAccess(actions[0], resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }

    @Override
    boolean authorizeProjectResource(
        final AuthContext authContext,
        final Map<String, String> resource,
        final String action,
        final String project
    ) {
        if (!authContextProcessor.authorizeProjectResource(authContext, resource, action, project)) {
            throw new UnauthorizedAccess(action, resource.get("kind")?:resource.get("type"), resource.get("name")?:'')
        }
        return true
    }

    @Override
    boolean authorizeProjectConfigure(final AuthContext authContext, final String project) {

        if (!authContextProcessor.authorizeProjectConfigure(authContext, project)) {
            throw new UnauthorizedAccess(AuthConstants.ACTION_CONFIGURE, "project", project)
        }
        return true
    }

    @Override
    boolean authorizeProjectExecutionAll(
        final AuthContext authContext,
        final Execution exec,
        final Collection<String> actions
    ) {
        if (!authContextProcessor.authorizeProjectExecutionAll(authContext, exec,actions)) {
            throw new UnauthorizedAccess(actions[0], "Execution", exec.id.toString())
        }
        return true
    }

    @Override
    boolean authorizeProjectExecutionAny(
        final AuthContext authContext,
        final Execution exec,
        final Collection<String> actions
    ) {
        if (!authContextProcessor.authorizeProjectExecutionAny(authContext, exec,actions)) {
            throw new UnauthorizedAccess(actions[0], "Execution", exec.id.toString())
        }
        return true
    }


    @Override
    boolean authorizeProjectJobAny(
        final AuthContext authContext,
        final ScheduledExecution job,
        final Collection<String> actions,
        final String project
    ) {
        if (!authContextProcessor.authorizeProjectJobAny(authContext, job, actions, project)) {
            throw new UnauthorizedAccess(actions[0], "Job", job.generateFullName())
        }
        return true
    }

    @Override
    boolean authorizeProjectJobAll(
        final AuthContext authContext,
        final ScheduledExecution job,
        final Collection<String> actions,
        final String project
    ) {
        if (!authContextProcessor.authorizeProjectJobAll(authContext, job, actions, project)) {
            throw new UnauthorizedAccess(actions[0], "Job", job.generateFullName())
        }
        return true
    }

}
