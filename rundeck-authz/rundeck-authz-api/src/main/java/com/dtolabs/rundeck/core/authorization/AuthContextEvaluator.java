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

package com.dtolabs.rundeck.core.authorization;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility interface for checking authorization for common resources using AuthContext
 */
public interface AuthContextEvaluator {

    /**
     * Return the resource definition for a job for use by authorization checks
     * @param name job name
     * @param jobGroup job group
     * @param uuid uuid
     * @return
     */
    Map<String,String> authResourceForJob(String name, String jobGroup, String uuuid);

    /**
     * return true if all of the actions are authorized for the resource type in the application context
     *
     * @param resourceType
     * @param actions
     */
    boolean authorizeApplicationResourceTypeAll(
            AuthContext authContext,
            String resourceType,
            Collection<String> actions
    );

    /**
     * return true if the action is authorized for the resource type in the application context
     *
     * @param resourceType
     * @param action
     */
    boolean authorizeApplicationResourceType(AuthContext authContext, String resourceType, String action);

    /**
     * return true if any of the actions are authorized for the resource in the application context
     *
     * @param resource
     * @param actions
     */
    boolean authorizeApplicationResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            List<String> actions
    );

    /**
     * return true if all of the actions are authorized for the resource in the application context
     *
     * @param resource
     * @param actions
     */
    boolean authorizeApplicationResourceAll(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions
    );

    /**
     * return all authorized resources for the action evaluated in the application context
     *
     * @param resources requested resources to authorize
     * @param actions   set of any actions to authorize
     * @return set of authorized resources
     */
    Set<Map<String, String>> authorizeApplicationResourceSet(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions
    );

    /**
     * return true if the action is authorized for the resource in the application context
     *
     * @param resource
     * @param action
     */
    boolean authorizeApplicationResource(AuthContext authContext, Map<String, String> resource, String action);


    /**
     * Return true if any actions are authorized for the resource in the project context
     *
     * @param resource
     * @param actions
     * @param project
     */
    boolean authorizeProjectResourceAny(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    );

    /**
     * Return true if all actions are authorized for the resource in the project context
     *
     * @param resource
     * @param actions
     * @param project
     */
    boolean authorizeProjectResourceAll(
            AuthContext authContext,
            Map<String, String> resource,
            Collection<String> actions,
            String project
    );

    /**
     * return true if the action is authorized for the resource in the project context
     *
     * @param resource
     * @param action
     * @param project
     */
    boolean authorizeProjectResource(
            AuthContext authContext,
            Map<String, String> resource,
            String action,
            String project
    );

    /**
     * return the decision set for all actions on all resources in the project context
     *
     * @param resources
     * @param actions
     * @param project
     */
    Set<Decision> authorizeProjectResources(
            AuthContext authContext,
            Set<Map<String, String>> resources,
            Set<String> actions,
            String project
    );

    /**
     * Return the resource inition for a project ACL for use by authorization checks
     *
     * @param name the project name
     * @return resource map for authorization check
     */
    Map<String, String> authResourceForProjectAcl(String name);

    /**
     * Return the resource inition for a project for use by authorization checks
     *
     * @param name the project name
     * @return resource map for authorization check
     */
    Map<String, String> authResourceForProject(String name);
}
