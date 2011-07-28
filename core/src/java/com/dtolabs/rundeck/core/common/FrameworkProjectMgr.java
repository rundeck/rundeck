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
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * DepotMgr is a framework resource that provides interfaces for looking up other resources such
 * as {@link FrameworkType} {@link FrameworkResourceInstance}, etc.
 */
public class FrameworkProjectMgr extends FrameworkResourceParent implements IFrameworkProjectMgr {

    public static final Logger log = Logger.getLogger(FrameworkProjectMgr.class);

    private final Framework framework;

    private final IPropertyLookup lookup;

    /**
     * Factory method.
     *
     * @param name      Name of manager. informational purposes
     * @param baseDir   Basedir where child resources live
     * @param framework Framework instance
     */
    public static FrameworkProjectMgr create(final String name, final File baseDir, final Framework framework) {
        return new FrameworkProjectMgr(name, baseDir, framework);
    }


    /**
     * Base constructor
     *
     * @param name       Name of manager. informational purposes
     * @param baseDir    Basedir where child resources live
     * @param framework  Framework instance
     * @param initialize If true, calls {@link #initialize()} method.
     */
    private FrameworkProjectMgr(final String name, final File baseDir, final Framework framework) {
        super(name, baseDir, framework);
        this.framework = framework;
        lookup = PropertyLookup.create(framework.getPropertyLookup());
    }






    /**
     * Add a new project to the map. Checks if project has its own module library and creates
     * a ModuleLookup object accordingly.
     *
     * @param projectName Name of the project
     */
    public FrameworkProject createFrameworkProject(final String projectName) {
        final FrameworkProject project = createFrameworkProjectInt(projectName);

        return project;
    }
    /**
     * Add a new project to the map. Checks if project has its own module library and creates
     * a ModuleLookup object accordingly.
     *
     * @param projectName Name of the project
     */
    public FrameworkProject createFrameworkProject(final String projectName, final Properties properties) {
        final FrameworkProject project = createFrameworkProjectInt(projectName,properties);

        return project;
    }

    final HashMap<String,FrameworkProject> projectCache= new HashMap<String, FrameworkProject>();
    /**
     * Create a project object without adding to child map
     * @param projectName
     * @return
     */
    private FrameworkProject createFrameworkProjectInt(final String projectName) {
        return createFrameworkProjectInt(projectName, null);
    }
    /**
     * Create a project object without adding to child map
     * @param projectName
     * @return
     */
    private FrameworkProject createFrameworkProjectInt(final String projectName,final Properties properties) {
        final FrameworkProject project;
        synchronized (projectCache) {
            if (null != projectCache.get(projectName)) {
                return projectCache.get(projectName);
            }
            final File projectDir = new File(getBaseDir(), projectName);
            // check if the FrameworkProject has its own module library
            project= FrameworkProject.create(projectName, getBaseDir(), this, properties);
            projectCache.put(projectName, project);
        }
        return project;
    }

    /**
     * returns a collection of Depot objects
     *
     * @return
     */
    public Collection listFrameworkProjects() {
        return listChildren();
    }

    /**
     * Looks for name as an existing Depot object and returns it
     *
     * @param name The name of the project
     * @return
     */
    public FrameworkProject getFrameworkProject(final String name) {
        try {
            return (FrameworkProject)getChild(name);
        } catch (NoSuchResourceException e) {
            throw new NoSuchResourceException("Project does not exist: " + name, this);
        }
    }

    /**
     * Determines if Depot exists in framework.
     *
     * @param project The name of the project
     * @return
     */
    public boolean existsFrameworkProject(final String project) {
        if (null == project) throw new IllegalArgumentException("project paramater was null");
        return existsChild(project);
    }


    /**
     * Prints internal state info for debugging purposes
     *
     * @return
     */
    public String toString() {
        return "DepotMgr{" +
                "name=" + getName() +
                ", baseDir=" + getBaseDir() +
                "}";
    }


    public Properties getProperties() {
        return new Properties();
    }

    public File getPropertyFile() {
        throw new UnsupportedOperationException("FrameworkProjectMgr should not have its own property file");
    }


    public IFrameworkResource createChild(final String projectName) {
        return createFrameworkProject(projectName);
    }

    public IFrameworkResource loadChild(String name) {
        if (childCouldBeLoaded(name) ) {
            return createFrameworkProjectInt(name);
        }else{
            return null;
        }
    }


    public Framework getFramework() {
        return framework;
    }

    /**
     * Checks if objects must be registered in the resources.properties file
     *
     * @param projectName Name of project to check
     * @return true if registration is required
     */
    public boolean isConfiguredObjectDeploymentsCheck(final String projectName) {
        if (existsFrameworkProject(projectName)) {
            final FrameworkProject d = getFrameworkProject(projectName);
            return d.hasProperty("project.resources.check");
        } else {
            throw new FrameworkResourceException("project not found: " + projectName, this);
        }
    }




    /**
     * get property value
     *
     * @param key the name of the property
     * @return property value
     */
    public String getProperty(final String key) {
        return lookup.getProperty(key);
    }

    /**
     * checks if property value exists
     *
     * @param key name of the property
     * @return true if it exists; false otherwise
     */
    public boolean hasProperty(final String key) {
        return lookup.hasProperty(key);
    }

    /**
     * Retrieves map of property data
     *
     * @return {@link java.util.Map} containing property key/value pair
     * @throws com.dtolabs.rundeck.core.utils.PropertyLookupException
     *          thrown if loaderror
     */
    public Map getPropertiesMap() {
        return lookup.getPropertiesMap();
    }
}
