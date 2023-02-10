package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.common.INodeSet
import groovy.transform.CompileStatic
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.data.model.v1.job.JobData
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution

import java.util.function.Function

/**
 * Wraps AppAuthContextEvaluator to provide metrics for evaluation times
 */
@CompileStatic
class TimedAuthContextEvaluator implements AppAuthContextEvaluator {

    @Autowired MetricService metricService
    AppAuthContextEvaluator rundeckAuthContextEvaluator

    @Override
    Map<String, String> authResourceForProject(String name) {
        rundeckAuthContextEvaluator.authResourceForProject(name)
    }

    @Override
    Map<String, String> authResourceForProjectAcl(String name) {
        rundeckAuthContextEvaluator.authResourceForProjectAcl(name)
    }


    @Override
    Set<Decision> authorizeProjectResources(
        AuthContext authContext,
        Set<Map<String, String>> resources,
        Set<String> actions,
        String project
    ) {
        Set<Decision> result = null
        metricService.withTimer(this.class.name, 'authorizeProjectResources') {
            result = rundeckAuthContextEvaluator.authorizeProjectResources(authContext, resources, actions, project)
        }
        return result
    }

    @Override
    boolean authorizeProjectResource(
        AuthContext authContext,
        Map<String, String> resource,
        String action,
        String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResource') {
            rundeckAuthContextEvaluator.authorizeProjectResource(authContext, resource, action, project)
        }
    }

    @Override
    boolean authorizeProjectResourceAll(
        AuthContext authContext,
        Map<String, String> resource,
        Collection<String> actions,
        String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResourceAll') {
            rundeckAuthContextEvaluator.authorizeProjectResourceAll(authContext, resource, actions, project)
        }
    }

    @Override
    boolean authorizeProjectResourceAny(
        AuthContext authContext,
        Map<String, String> resource,
        Collection<String> actions,
        String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectResourceAny') {
            rundeckAuthContextEvaluator.authorizeProjectResourceAny(authContext, resource, actions, project)
        }
    }


    @Override
    boolean authorizeApplicationResource(AuthContext authContext, Map<String, String> resource, String action) {

        metricService.withTimer(this.class.name, 'authorizeApplicationResource') {
            rundeckAuthContextEvaluator.authorizeApplicationResource(authContext, resource, action)
        }

    }

    @Override
    Set<Map<String, String>> authorizeApplicationResourceSet(
        AuthContext authContext,
        Set<Map<String, String>> resources,
        Set<String> actions
    ) {
        Set<Map<String, String>> result = null
        metricService.withTimer(this.class.name, 'authorizeApplicationResourceSet') {
            result = rundeckAuthContextEvaluator.authorizeApplicationResourceSet(authContext, resources, actions)
        }
        return result
    }


    @Override
    boolean authorizeApplicationResourceAll(
        AuthContext authContext,
        Map<String, String> resource,
        Collection<String> actions
    ) {
        metricService.withTimer(this.class.name, 'authorizeApplicationResourceAll') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceAll(authContext, resource, actions)
        }
    }

    @Override
    boolean authorizeApplicationResourceAny(
        AuthContext authContext,
        Map<String, String> resource,
        List<String> actions
    ) {
        rundeckAuthContextEvaluator.authorizeApplicationResourceAny(authContext, resource, actions)
    }

    @Override
    boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        metricService.withTimer(this.class.name, 'authorizeApplicationResourceType') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceType(authContext, resourceType, action)
        }
    }

    @Override
    boolean authorizeApplicationResourceTypeAll(
        AuthContext authContext,
        String resourceType,
        Collection<String> actions
    ) {
        return metricService.withTimer(this.class.name, 'authorizeApplicationResourceTypeAll') {
            rundeckAuthContextEvaluator.authorizeApplicationResourceTypeAll(authContext, resourceType, actions)
        }
    }

    @Override
    Map<String,String> authResourceForJob(final JobData se) {
        return rundeckAuthContextEvaluator.authResourceForJob(se)
    }

    @Override
    Map<String,String> authResourceForJob(final String name, final String groupPath, final String uuid) {
        return rundeckAuthContextEvaluator.authResourceForJob(name, groupPath, uuid)
    }

    @Override
    boolean authorizeProjectExecutionAll(
        final AuthContext authContext,
        final Execution exec,
        final Collection<String> actions
    ) {
        return rundeckAuthContextEvaluator.authorizeProjectExecutionAll(authContext, exec, actions)
    }

    @Override
    boolean authorizeProjectExecutionAny(
        final AuthContext authContext,
        final Execution exec,
        final Collection<String> actions
    ) {
        return rundeckAuthContextEvaluator.authorizeProjectExecutionAny(authContext, exec, actions)
    }

    @Override
    List<Execution> filterAuthorizedProjectExecutionsAll(
        final AuthContext authContext,
        final List<Execution> execs,
        final Collection<String> actions
    ) {
        List<Execution> result = null
        metricService.withTimer(this.class.name, 'filterAuthorizedProjectExecutionsAll') {
            result = rundeckAuthContextEvaluator.filterAuthorizedProjectExecutionsAll(authContext, execs, actions)
        }
        return result
    }

    @Override
    boolean authorizeProjectJobAny(
        final AuthContext authContext,
        final JobData job,
        final Collection<String> actions,
        final String project
    ) {
        return rundeckAuthContextEvaluator.authorizeProjectJobAny(authContext, job, actions, project)
    }

    @Override
    boolean authorizeProjectJobAll(
        final AuthContext authContext,
        final JobData job,
        final Collection<String> actions,
        final String project
    ) {
        metricService.withTimer(this.class.name, 'authorizeProjectJobAll') {
            return rundeckAuthContextEvaluator.authorizeProjectJobAll(authContext, job, actions, project)
        }
    }

    @Override
    INodeSet filterAuthorizedNodes(
        final String project,
        final Set<String> actions,
        final INodeSet unfiltered,
        final AuthContext authContext
    ) {
        return rundeckAuthContextEvaluator.filterAuthorizedNodes(project, actions, unfiltered, authContext)
    }

    @Override
    def <T> Set<T> filterAuthorizedResourcesAll(
        final AuthContext authContext,
        final String project,
        final Set<String> actions,
        final Set<T> resources,
        final Function<T, Map<String, String>> convert,
        final String key
    ) {
        return rundeckAuthContextEvaluator.
            filterAuthorizedResourcesAll(authContext, project, actions, resources, convert, key)
    }

    @Override
    boolean authorizeProjectConfigure(final AuthContext authContext, final String project) {
        return rundeckAuthContextEvaluator.authorizeProjectConfigure(authContext, project)
    }
}
