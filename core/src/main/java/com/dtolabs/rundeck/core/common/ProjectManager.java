/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.core.common;

import java.util.Collection;
import java.util.Properties;

/**
 * Managers project
 */
public interface ProjectManager {

    /**
     * List all current {@link FrameworkProject} objects
     *
     * @return a Collection of {@link FrameworkProject} instances
     */
    Collection<IRundeckProject> listFrameworkProjects();

    /**
     * List the project names
     * @return
     */
    Collection<String> listFrameworkProjectNames();

    /**
     * Get the specified existing project
     *
     * @param name Depot name
     * @return {@link IRundeckProject} instance
     */
    IRundeckProject getFrameworkProject(String name);

    IRundeckProjectConfig loadProjectConfig(final String project);

    /**
     * Checks if project by that name exists
     *
     * @param project project name
     * @return true if that project exists. false otherwise
     */
    boolean existsFrameworkProject(String project);

    /**
     * Create a new project. This also creates its structure
     *
     * @param projectName Name of project
     * @return newly created {@link FrameworkProject}
     */
    IRundeckProject createFrameworkProject(String projectName);

    /**
     * @return Create a new project if it doesn't exist, otherwise returns existing project
     *
     * @param projectName Name of the project
     * @param properties additional properties to include in the project's properties file
     */
    IRundeckProject createFrameworkProject(String projectName, Properties properties);

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
    IRundeckProject createFrameworkProjectStrict(String projectName, Properties properties);
}
