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

import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Filesystem based project manager
 */
public class FrameworkProjectMgr extends FrameworkResourceParent implements IFrameworkProjectMgr {
    static final String PROJECTMGR_NAME = "frameworkProjectMgr";

    public static final Logger log = Logger.getLogger(FrameworkProjectMgr.class);

    private final FilesystemFramework filesystemFramework;
    private IProjectNodesFactory nodesFactory;


    /**
     * Base constructor
     *
     * @param name                Name of manager. informational purposes
     * @param baseDir             Basedir where child resources live
     * @param filesystemFramework Framework instance
     */
    FrameworkProjectMgr(
            final String name,
            final File baseDir,
            final FilesystemFramework filesystemFramework,
            final IProjectNodesFactory nodesFactory
    )
    {
        super(name, baseDir, null);
        this.filesystemFramework = filesystemFramework;
        this.nodesFactory=nodesFactory;
    }

    static FrameworkProjectMgr create(
            final String name,
            final File baseDir,
            final Framework framework,
            final IProjectNodesFactory nodesFactory
    )
    {
        return FrameworkFactory.createProjectManager(baseDir, framework.getFilesystemFramework(), nodesFactory);
    }


    /**
     *
     * @param projectName Name of the project
     */
    public IRundeckProject createFrameworkProject(final String projectName) {

        return createFrameworkProjectInt(projectName);
    }
    /**
     *
     * @param projectName Name of the project
     */
    public FrameworkProject createFSFrameworkProject(final String projectName) {

        return createFrameworkProjectInt(projectName);
    }

    public FilesystemFramework getFilesystemFramework() {
        return filesystemFramework;
    }

    public IRundeckProject createFrameworkProject(final String projectName, final Properties properties) {

        return createFrameworkProjectInt(projectName,properties,false);
    }

    /**
     * Create a new project if it doesn't, otherwise throw exception
     * @param projectName name of project
     * @param properties config properties
     * @return new project
     * @throws IllegalArgumentException if the project already exists
     */
    @Override
    public IRundeckProject createFrameworkProjectStrict(final String projectName, final Properties properties) {

        return createFrameworkProjectInt(projectName,properties,true);
    }

    final HashMap<String,FrameworkProject> projectCache= new HashMap<String, FrameworkProject>();
    /**
     * @return Create a project object without adding to child map
     * @param projectName name of project
     */
    private FrameworkProject createFrameworkProjectInt(final String projectName) {
        return createFrameworkProjectInt(projectName, null,false);
    }

    /**
     * @return Create a project object without adding to child map
     * @param projectName name
     * @param properties properties
     * @param strict true for strict exists check
     * @throws java.lang.IllegalArgumentException if strict is true and the project already exists
     */
    private FrameworkProject createFrameworkProjectInt(final String projectName,final Properties properties,
            boolean strict) {
        final FrameworkProject project;
        if (strict && existsFrameworkProject(projectName)) {
            throw new IllegalArgumentException("project exists: " + projectName);
        }
        synchronized (projectCache) {
            if (null != projectCache.get(projectName)) {
                return projectCache.get(projectName);
            }
            // check if the FrameworkProject has its own module library
            project = FrameworkFactory.createFrameworkProject(
                    projectName,
                    new File(getBaseDir(), projectName),
                    filesystemFramework,
                    this,
                    nodesFactory,
                    properties
            );

            projectCache.put(projectName, project);
        }
        return project;
    }

    /**
     * Remove a project definition
     * @param projectName name of the project
     */
    @Override
    public void removeFrameworkProject(final String projectName){
        synchronized (projectCache) {
            super.remove(projectName);
            projectCache.remove(projectName);
        }
    }

    /**
     * @return a collection of Project objects
     */
    public Collection<IRundeckProject> listFrameworkProjects() {
        return listChildren();
    }

    @Override
    public Collection<String> listFrameworkProjectNames() {
        return new TreeSet<>(listChildNames());
    }

    /**
     * @return an existing Project object and returns it
     *
     * @param name The name of the project
     */
    public FrameworkProject getFrameworkProject(final String name) {
        try {
            return (FrameworkProject)getChild(name);
        } catch (NoSuchResourceException e) {
            throw new NoSuchResourceException("Project does not exist: " + name, this);
        }
    }

    @Override
    public IRundeckProjectConfig loadProjectConfig(final String projectName) {
        return FrameworkFactory.loadFrameworkProjectConfig(
                projectName,
                new File(getBaseDir(), projectName),
                filesystemFramework,
                null
        );
    }

    /**
     * @return true if project exists in framework.
     *
     * @param project The name of the project
     */
    public boolean existsFrameworkProject(final String project) {
        if (null == project) throw new IllegalArgumentException("project paramater was null");
        return existsChild(project);
    }

    @Override
    public boolean childCouldBeLoaded(final String name) {
        return super.childCouldBeLoaded(name) && new File(getBaseDir(), name + "/etc/project.properties").exists();
    }

    public String toString() {
        return "FrameworkProjectMgr{" +
                "name=" + getName() +
                ", baseDir=" + getBaseDir() +
                "}";
    }


    public Properties getProperties() {
        return new Properties();
    }

    public IFrameworkResource createChild(final String projectName) {
        return createFSFrameworkProject(projectName);
    }

    public IFrameworkResource loadChild(String name) {
        if (childCouldBeLoaded(name) ) {
            return createFrameworkProjectInt(name);
        }else{
            return null;
        }
    }


}
