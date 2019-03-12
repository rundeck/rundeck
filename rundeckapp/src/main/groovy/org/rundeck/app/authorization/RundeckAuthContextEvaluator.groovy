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
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext
import com.dtolabs.rundeck.server.authorization.AuthConstants
import groovy.transform.CompileStatic

/**
 * Implements AuthContextEvaluator for checking authorization for common resources using AuthContext
 */
@CompileStatic
class RundeckAuthContextEvaluator implements AuthContextEvaluator {

    @Override
    boolean authorizeApplicationResourceTypeAll(
            AuthContext authContext,
            String resourceType,
            Collection<String> actions
    ) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        Set<Decision> decisions =
                authContext.evaluate(
                        [AuthorizationUtil.resourceType(resourceType)] as Set,
                        actions as Set,
                        EnvironmentalContext.RUNDECK_APP_ENV
                )

        return !(decisions.find { !it.authorized })
    }


    @Override
    boolean authorizeApplicationResource(AuthContext authContext, Map<String, String> resource, String action) {
        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }

        def decision = authContext.evaluate(resource, action, EnvironmentalContext.RUNDECK_APP_ENV)

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
        def decisions = authContext.evaluate(resources, actions, EnvironmentalContext.RUNDECK_APP_ENV)

        return decisions.findAll { it.authorized }.collect { it.resource }.toSet()
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
        Set<Decision> decisions = authContext.evaluate(
                [resource] as Set,
                actions as Set,
                EnvironmentalContext.RUNDECK_APP_ENV
        )


        return !(decisions.find { !it.authorized })
    }

    @Override
    boolean authorizeApplicationResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            List<String> actions
    ) {
        return actions.any {
            authorizeApplicationResourceAll(authContext, resource, [it])
        }
    }

    @Override
    boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action) {

        if (null == authContext) {
            throw new IllegalArgumentException("null authContext")
        }
        def decision = authContext.evaluate(
                AuthorizationUtil.resourceType(resourceType),
                action,
                EnvironmentalContext.RUNDECK_APP_ENV
        )

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
        Set<Decision> decisions = authContext.evaluate(resources, actions, EnvironmentalContext.forProject(project))

        return decisions
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
        def decision = authContext.evaluate(resource, action, EnvironmentalContext.forProject(project))

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
        def decisions = authContext.evaluate(
                [resource] as Set,
                actions as Set,
                EnvironmentalContext.forProject(project)
        )

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
        def decisions = authContext.evaluate(
                [resource] as Set,
                actions as Set,
                EnvironmentalContext.forProject(project)
        )

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
}
