/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.app.authorization


import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.common.IFrameworkNodes
import com.dtolabs.rundeck.core.common.INodeSet
import grails.compiler.GrailsCompileStatic
import org.rundeck.core.auth.AuthConstants
import rundeck.Execution
import rundeck.ScheduledExecution

import java.util.function.Function

/**
 * Implements AppAuthContextEvaluator for checking authorization for common resources using AuthContext
 */
@GrailsCompileStatic
class BaseAuthContextEvaluator implements AppAuthContextEvaluator {
    AuthCache authContextEvaluatorCacheManager
    IFrameworkNodes nodeSupport

    @Override
    boolean authorizeApplicationResourceTypeAll(
        AuthContext authContext,
        String resourceType,
        Collection<String> actions
    ) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        Set<Decision> decisions = authContextEvaluatorCacheManager.evaluate(authContext,
                [AuthorizationUtil.resourceType(resourceType)] as Set,
                actions as Set, null)

        return !(decisions.find { !it.authorized })
    }


    @Override
    boolean authorizeApplicationResource(AuthContext authContext, Map<String, String> resource, String action) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }

        def decision = authContextEvaluatorCacheManager.evaluate(authContext, resource, action, null)

        return decision.authorized
    }

    @Override
    Set<Map<String, String>> authorizeApplicationResourceSet(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions
    ) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        if(resources.size()<1){
            return new HashSet<Map<String, String>>()
        }
        Set<Map<String, String>> authed=new HashSet<>()
        resloop: for (res in resources) {
            for(act in actions) {
                if (authContextEvaluatorCacheManager.evaluate(authContext, res, act,null).authorized) {
                    authed << res
                    continue resloop
                }
            }
        }
        return authed
    }


    @Override
    boolean authorizeApplicationResourceAll(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions
    ) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        Set<Decision> decisions = authContextEvaluatorCacheManager.evaluate(authContext, [resource] as Set, actions as Set, null)

        return !(decisions.find { !it.authorized })
    }

    @Override
    boolean authorizeApplicationResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            List<String> actions
    ) {
        return actions.any {
            authContextEvaluatorCacheManager.evaluate(authContext, resource, it, null).authorized
        }
    }

    @Override
    boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decision = authContextEvaluatorCacheManager.evaluate(authContext, AuthorizationUtil.resourceType(resourceType), action, null)

        return decision.authorized
    }

    @Override
    Set<Decision> authorizeProjectResources(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions,
            String project
    ) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }

        return resources.size() > 0 ?
               authContextEvaluatorCacheManager.evaluate(authContext, resources, actions, project) :
               new HashSet<Decision>()
    }


    @Override
    boolean authorizeProjectResource(
            AuthContext authContext,
            Map<String, String> resource,
            String action,
            String project
    ) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }

        def decision = authContextEvaluatorCacheManager.evaluate(authContext, resource, action, project)

        return decision.authorized
    }

    @Override
    boolean authorizeProjectResourceAll(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    ) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions = authContextEvaluatorCacheManager.evaluate(
                authContext, [resource] as Set, actions as Set, project)

        return !(decisions.find { !it.authorized })
    }

    @Override
    boolean authorizeProjectResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    ) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions = authContextEvaluatorCacheManager.evaluate(authContext, [resource] as Set, actions as Set, project)

        return (decisions.find { it.authorized })
    }


    @Override
    Map<String, String> authResourceForProject(String name) {
        return AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT, [name: name])
    }

    @Override
    Map<String, String> authResourceForProjectAcl(String name) {
        return AuthorizationUtil.resource(AuthConstants.TYPE_PROJECT_ACL, [name: name])
    }


    /**
     * Return the resource definition for a job for use by authorization checks
     * @param se
     * @return
     */
    @Override
    def Map<String,String> authResourceForJob(ScheduledExecution se) {
        return authResourceForJob(se.jobName, se.groupPath, se.extid)
    }

    /**
     * Return the resource definition for a job for use by authorization checks, using parameters as input
     * @param se
     * @return
     */
    @Override
    def Map<String,String> authResourceForJob(String name, String groupPath, String uuid) {
        return AuthorizationUtil.resource(AuthConstants.TYPE_JOB, [name: name, group: groupPath ?: '', uuid: uuid])
    }


    /**
     * Return true if the user is authorized for all actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    @Override
    boolean authorizeProjectExecutionAll(AuthContext authContext, Execution exec, Collection<String> actions) {
        def ScheduledExecution se = exec.scheduledExecution
        return se ?
               authorizeProjectJobAll(authContext, se, actions, se.project) :
               authorizeProjectResourceAll(authContext, AuthConstants.RESOURCE_ADHOC, actions, exec.project)

    }
    /**
     * Return true if the user is authorized for any actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    @Override
    boolean authorizeProjectExecutionAny(AuthContext authContext, Execution exec, Collection<String> actions) {
        def ScheduledExecution se = exec.scheduledExecution
        return se ?
               authorizeProjectJobAny(authContext, se, actions, se.project) :
               authorizeProjectResourceAny(authContext, AuthConstants.RESOURCE_ADHOC, actions, exec.project)

    }
    /**
     * Filter a list of Executions and return only the ones that the user has authorization for all actions in the
     * project context
     * @param framework
     * @param execs list of executions
     * @param actions
     * @return List of authorized executions
     */
    @Override
    List<Execution> filterAuthorizedProjectExecutionsAll(
        AuthContext authContext,
        List<Execution> execs,
        Collection<String> actions
    ) {
        def semap = [:]
        def adhocauth = null
        List<Execution> results = []
        execs.each { Execution exec ->
            def ScheduledExecution se = exec.scheduledExecution
            if (se && null == semap[se.id]) {
                semap[se.id] = authorizeProjectJobAll(authContext, se, actions, se.project)
            } else if (!se && null == adhocauth) {
                adhocauth = authorizeProjectResourceAll(
                    authContext, AuthConstants.RESOURCE_ADHOC, actions,
                    exec.project
                )
            }
            if (se ? semap[se.id] : adhocauth) {
                results << exec
            }
        }
        return results
    }
    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    @Override
    boolean authorizeProjectJobAny(
        AuthContext authContext,
        ScheduledExecution job,
        Collection<String> actions,
        String project
    ) {
        actions.any {
            authorizeProjectJobAll(authContext, job, [it], project)
        }
    }
    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    @Override
    boolean authorizeProjectJobAll(
        AuthContext authContext,
        ScheduledExecution job,
        Collection<String> actions,
        String project
    ) {
        return authorizeResourceAll(authContext, project, new HashSet<String> (actions), job, this.&authResourceForJob)
    }

    def <T> boolean authorizeResourceAll(
        AuthContext authContext,
        final String project,
        final Set<String> actions,
        final T resource,
        final Function<T, Map<String, String>> convert
    ) {
        if (null == project) {
            throw new IllegalArgumentException("null project")
        }
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decisions =
            authContextEvaluatorCacheManager.evaluate(
                authContext,
                [convert.apply(resource)] as Set,
                actions as Set,
                project
            )

        return !(decisions.find { !it.authorized })
    }

    @Override
    public <T> Set<T> filterAuthorizedResourcesAll(
        final AuthContext authContext,
        final String project,
        final Set<String> actions,
        final Set<T> resources,
        final Function<T, Map<String, String>> convert,
        final String key
    ) {
        if (null == actions || actions.size() <= 0) {
            return resources;
        }
        final Map<String,T> inputMap = new HashMap<>()
        final HashSet<Map<String, String>> authResources = new HashSet<Map<String, String>>();
        for (final T item : resources) {
            def map = convert.apply(item)
            authResources.add(map);
            inputMap.put(map.get(key),item)
        }
        final Set<Decision> decisions = authContextEvaluatorCacheManager.evaluate(
            authContext,
            authResources,
            actions,
            project
        )
        final HashSet<T> authorized = new HashSet<>();
        HashMap<String, Set<String>> authorizations = new HashMap<String, Set<String>>();
        for (final Decision decision : decisions) {
            if (decision.isAuthorized() && actions.contains(decision.getAction())) {
                final String keyVal = decision.getResource().get(key);
                authorizations.computeIfAbsent(keyVal) { k -> new HashSet<>() };
                authorizations.get(keyVal).add(decision.getAction());
            }
        }
        for (final Map.Entry<String, Set<String>> entry : authorizations.entrySet()) {
            if(entry.getValue().size()==actions.size()) {
                authorized.add(inputMap.get(entry.getKey()));
            }
        }
        return authorized;
    }
    /**
     * @return the nodeset consisting only of the input nodes where the specified actions are all authorized
     * @param project project name
     * @param actions action set
     * @param unfiltered nodes
     * @param authContext authoriziation
     */
    @Override
    public INodeSet filterAuthorizedNodes(
        final String project, final Set<String> actions, final INodeSet unfiltered,
        AuthContext authContext
    ) {
        return nodeSupport.filterAuthorizedNodes(project, actions, unfiltered, authContext)
    }

    @Override
    boolean authorizeProjectConfigure(final AuthContext authContext, final String project) {
        return authorizeApplicationResourceAny(
            authContext,
            authResourceForProject(project),
            [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        )
    }
}
