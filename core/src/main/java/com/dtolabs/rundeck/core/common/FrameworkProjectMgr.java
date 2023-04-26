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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Filesystem based project manager
 */
public class FrameworkProjectMgr extends FrameworkResource implements IFrameworkProjectMgr {
    static final String PROJECTMGR_NAME = "frameworkProjectMgr";

    public static final Logger log = LoggerFactory.getLogger(FrameworkProjectMgr.class);

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
        super(name, baseDir);
        this.filesystemFramework = filesystemFramework;
        this.nodesFactory=nodesFactory;
    }

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
            final FilesystemFramework filesystemFramework
    )
    {
        super(name, baseDir);
        this.filesystemFramework = filesystemFramework;
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
            removeSubDir(projectName);
            projectCache.remove(projectName);
        }
    }

    /**
     * @return a collection of Project objects
     */
    public Collection<IRundeckProject> listFrameworkProjects() {
        return listChildren();
    }

    private Collection<IRundeckProject> listChildren() {
        return listChildDirs().stream()
                              .map(file -> createFrameworkProjectInt(file.getName()))
                              .collect(Collectors.toList());
    }

    @Override
    public Collection<String> listFrameworkProjectNames() {
        return new TreeSet<>(listChildNames());
    }

    @Override
    public int countFrameworkProjects() {
        return listChildNames().size();
    }

    @Override
    public void disableFrameworkProject(String projectName) {
        throw new UnsupportedOperationException("Operation not supported for file-based projects");
    }

    @Override
    public void enableFrameworkProject(String projectName) {
        throw new UnsupportedOperationException("Operation not supported for file-based projects");
    }

    @Override
    public boolean isFrameworkProjectDisabled(String projectName) {
        return false;
    }

    public List<String> listChildNames() {
        return listSubdirs().stream().filter(this::isValidProjectDir).map(File::getName).collect(Collectors.toList());
    }

    private List<File> listChildDirs() {
        return listSubdirs().stream().filter(this::isValidProjectDir).collect(Collectors.toList());
    }

    /**
     * @return an existing Project object and returns it
     *
     * @param name The name of the project
     */
    public FrameworkProject getFrameworkProject(final String name) {
        FrameworkProject frameworkProject = loadChild(name);
        if (null != frameworkProject) {
            return frameworkProject;
        }
        throw new NoSuchResourceException("Project does not exist: " + name, this);
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
        if (null == project) {
            throw new NullPointerException("project paramater was null");
        }
        return existsChild(project);
    }

    private boolean existsChild(final String project) {
        return existsSubdir(project) && isValidProjectDir(new File(getBaseDir(), project));
    }

    private boolean isValidProjectDir(final File dir) {
        return new File(dir, "etc/project.properties").isFile();
    }

    public boolean childCouldBeLoaded(final String name) {
        return existsChild(name);
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


    public FrameworkProject loadChild(String name) {
        if (childCouldBeLoaded(name) ) {
            return createFrameworkProjectInt(name);
        }else{
            return null;
        }
    }


    public IProjectNodesFactory getNodesFactory() {
        return nodesFactory;
    }

    public void setNodesFactory(IProjectNodesFactory nodesFactory) {
        this.nodesFactory = nodesFactory;
    }
}
