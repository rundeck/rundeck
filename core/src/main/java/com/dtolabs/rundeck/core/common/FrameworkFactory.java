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

import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Created by greg on 2/19/15.
 */
public class FrameworkFactory {
    /**
     * Returns an instance of Framework object.  Loads the framework.projects.dir property value, or defaults to
     * basedir/projects
     *
     * @param rdeck_base_dir path name to the rdeck_base
     *
     * @param serviceSupport
     * @return a Framework instance
     */
    public static Framework createForFilesystem(final String rdeck_base_dir, final IFrameworkServices serviceSupport) {
        File baseDir = new File(rdeck_base_dir);

        if (!baseDir.exists()) {
            throw new IllegalArgumentException("rdeck_base directory does not exist. " + rdeck_base_dir);
        }
        FilesystemFramework filesystemFramework = createFilesystemFramework(baseDir);


        //framework lookup property file
        File propertyFile2 = FilesystemFramework.getPropertyFile(FilesystemFramework.getConfigDir(baseDir));
        PropertyLookup lookup1 = PropertyLookup.createDeferred(propertyFile2);
        lookup1.expand();
        FrameworkProjectMgr projectManager = createProjectManager(
                filesystemFramework

        );
        Framework framework = createFramework(lookup1, filesystemFramework, projectManager, serviceSupport);

        IProjectNodesFactory nodesFactory = createNodesFactory(
                filesystemFramework,
                framework::getResourceFormatGeneratorService,
                framework::getResourceModelSourceService
        );
        projectManager.setNodesFactory(nodesFactory);
        return framework;
    }

    public static IProjectNodesFactory createNodesFactory(
            final IFilesystemFramework filesystemFramework,
            Supplier<ResourceFormatGeneratorService> formatGeneratorServiceSupplier,
            Supplier<ResourceModelSourceService> resourceModelSourceServiceSupplier
    )
    {
        return new IProjectNodesFactory() {
            @Override
            public IProjectNodes getNodes(final String name) {

                return new ProjectNodeSupport(
                        loadFrameworkProjectConfig(
                                name,
                                new File(filesystemFramework.getFrameworkProjectsBaseDir(), name),
                                filesystemFramework,
                                null
                        ),
                        formatGeneratorServiceSupplier.get(),
                        resourceModelSourceServiceSupplier.get()
                );
            }

            @Override
            public void refreshProjectNodes(final String name) {
                //noop
            }
        };
    }

    public static FilesystemFramework createFilesystemFramework(final File baseDir) {
        //framework lookup property file
        File propertyFile = FilesystemFramework.getPropertyFile(FilesystemFramework.getConfigDir(baseDir));
        //project manager for filesystem

        File projectsBase = determineProjectsBasedir(baseDir, propertyFile);

        return new FilesystemFramework(baseDir, projectsBase);
    }

    private static File determineProjectsBasedir(final File baseDir, final File propertyFile) {
        String projectsBaseDir = null;
        if (propertyFile.exists()) {
            PropertyRetriever propertyRetriever = FilesystemFramework.createPropertyRetriever(baseDir);
            projectsBaseDir = propertyRetriever.getProperty("framework.projects.dir");
        }
        if (null == projectsBaseDir) {
            projectsBaseDir = FilesystemFramework.getProjectsBaseDir(baseDir);
        }
        File projectsBase = new File(projectsBaseDir);
        if (!projectsBase.exists() && !projectsBase.mkdirs()) {
            throw new IllegalArgumentException("project base directory could not be created. " + projectsBaseDir);
        }
        return projectsBase;
    }

    /**
     * Create framework
     * @param lookup1 properties
     * @param filesystemFramework filessystem
     * @param projectManager project
     * @param services preloaded services
     * @param frameworkServices services support
     * @return framework
     */
    public static Framework createFramework(
            final IPropertyLookup lookup1,
            final FilesystemFramework filesystemFramework,
            final ProjectManager projectManager,
            Map<String,FrameworkSupportService> services,
            IFrameworkServices frameworkServices
    )
    {


        NodeSupport iFrameworkNodes = new NodeSupport();
        iFrameworkNodes.setLookup(lookup1);

        //framework

        Framework framework = new Framework(
                filesystemFramework,
                projectManager,
                lookup1,
                frameworkServices,
                iFrameworkNodes
        );
        filesystemFramework.setFramework(framework);
        if(null!=services) {
            //load predefined services
            for (String s : services.keySet()) {
                frameworkServices.setService(s, services.get(s));
            }
        }

        return framework;
    }
    private static Framework createFramework(
            final PropertyLookup lookup1,
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr projectManager,
            final IFrameworkServices frameworkServices
    )
    {

        NodeSupport iFrameworkNodes = new NodeSupport();
        iFrameworkNodes.setLookup(lookup1);

        //framework

        Framework framework = new Framework(
                filesystemFramework,
                projectManager,
                lookup1,
                frameworkServices,
                iFrameworkNodes
        );
        filesystemFramework.setFramework(framework);
        frameworkServices.initialize(framework);

        return framework;
    }

    public static FrameworkProjectMgr createProjectManager(
            final File baseDir,
            FilesystemFramework filesystemFramework,
            IProjectNodesFactory nodesFactory
    ) {
        return new FrameworkProjectMgr("name", baseDir, filesystemFramework,nodesFactory);
    }
    public static FrameworkProjectMgr createProjectManager(
            FilesystemFramework filesystemFramework,
            IProjectNodesFactory nodesFactory
    ) {
        return new FrameworkProjectMgr(
                "name",
                filesystemFramework.getFrameworkProjectsBaseDir(),
                filesystemFramework,
                nodesFactory
        );
    }

    public static FrameworkProjectMgr createProjectManager(
            FilesystemFramework filesystemFramework
    )
    {
        return new FrameworkProjectMgr(
                "name",
                filesystemFramework.getFrameworkProjectsBaseDir(),
                filesystemFramework
        );
    }

    /**
     * Tells the nodesFactory to refresh nodes after any config change
     */
    static class NodeResetConfigModifier implements IRundeckProjectConfigModifier{
        IProjectNodesFactory nodesFactory;
        IRundeckProjectConfigModifier modifier;
        String name;

        public NodeResetConfigModifier(
                final IProjectNodesFactory nodesFactory,
                final IRundeckProjectConfigModifier modifier,
                final String name
        )
        {
            this.nodesFactory = nodesFactory;
            this.modifier = modifier;
            this.name = name;
        }

        @Override
        public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
            modifier.mergeProjectProperties(properties, removePrefixes);
            nodesFactory.refreshProjectNodes(name);
        }

        @Override
        public void setProjectProperties(final Properties properties) {
            modifier.setProjectProperties(properties);
            nodesFactory.refreshProjectNodes(name);
        }

        @Override
        public void generateProjectPropertiesFile(
                final boolean overwrite,
                final Properties properties,
                final boolean addDefault
        )
        {
            modifier.generateProjectPropertiesFile(overwrite,properties,addDefault);
            nodesFactory.refreshProjectNodes(name);
        }
    }
    /**
     *
     * @param projectName name
     * @param baseDir base dir
     * @param filesystemFramework filesys
     * @param mgr project manager
     * @param properties properties
     * @return new project
     */
    public static FrameworkProject createFrameworkProject(
            String projectName,
            File baseDir,
            final IFilesystemFramework filesystemFramework,
            IFrameworkProjectMgr mgr,
            IProjectNodesFactory nodesFactory,
            Properties properties
    )
    {
        FrameworkProjectConfig projectConfig = loadFrameworkProjectConfig(
                projectName,
                baseDir,
                filesystemFramework,
                properties
        );
        FrameworkProject frameworkProject = new FrameworkProject(
                projectName,
                baseDir,
                filesystemFramework,
                mgr,
                projectConfig,
                new NodeResetConfigModifier(nodesFactory,projectConfig,projectName)
        );
        frameworkProject.setProjectNodesFactory(nodesFactory);

        return frameworkProject;
    }

    /**
     *
     * @param projectName
     * @param baseDir project base directory
     * @param filesystemFramework
     * @param properties
     * @return
     */
    public static FrameworkProjectConfig loadFrameworkProjectConfig(
            final String projectName,
            final File baseDir,
            final IFilesystemFramework filesystemFramework,
            final Properties properties
    )
    {
        return FrameworkProjectConfig.create(
                projectName,
                FrameworkProject.getProjectPropertyFile(baseDir),
                properties,
                filesystemFramework
        );
    }





}
