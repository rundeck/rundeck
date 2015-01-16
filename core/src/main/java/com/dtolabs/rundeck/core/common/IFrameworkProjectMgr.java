/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.Collection;
import java.util.Properties;

/**
 * A set of interfaces for managing a set of Depots
 */
public interface IFrameworkProjectMgr extends IFrameworkResourceParent, IPropertyLookup {

    /**
     * Gets the instance of the framework for this resource mgr
     *
     * @return Framework instance
     */
    Framework getFramework();

    /**
     * List all current {@link FrameworkProject} objects
     *
     * @return a Collection of {@link FrameworkProject} instances
     */
    Collection listFrameworkProjects();

    /**
     * Get the specified existing project
     *
     * @param name Depot name
     * @return {@link FrameworkProject} instance
     */
    FrameworkProject getFrameworkProject(String name);

    /**
     * Checks if project by that name exists
     *
     * @param project project name
     * @return true if that project exists. false otherwise
     */
    boolean existsFrameworkProject(String project);

    /**
     * checks if project is configured to use resources.properties lookups
     *
     * @param projectName name of project
     * @return true if configured for lookups
     */
    boolean isConfiguredObjectDeploymentsCheck(String projectName);


    /**
     * Create a new project. This also creates its structure
     *
     * @param projectName Name of project
     * @return newly created {@link FrameworkProject}
     */
    FrameworkProject createFrameworkProject(String projectName);

    /**
     * @return Create a new project if it doesn't exist, otherwise returns existing project
     *
     * @param projectName Name of the project
     * @param properties additional properties to include in the project's properties file
     */
    FrameworkProject createFrameworkProject(String projectName, Properties properties);

    /**
     * Remove a project definition
     * @param projectName name of the project
     */
    void removeFrameworkProject(String projectName);

    /**
     * Create a new project if it doesn't, otherwise throw exception
     * @param projectName name of project
     * @param properties config properties
     * @return new project
     * @throws IllegalArgumentException if the project already exists
     */
    FrameworkProject createFrameworkProjectStrict(String projectName, Properties properties);
}
