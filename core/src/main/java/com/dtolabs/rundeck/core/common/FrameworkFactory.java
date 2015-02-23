package com.dtolabs.rundeck.core.common;

import com.dtolabs.client.services.RundeckAPICentralDispatcher;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;
import java.util.Properties;

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
     * @return a Framework instance
     */
    public static Framework createForFilesystem(final String rdeck_base_dir) {
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
                filesystemFramework.getFrameworkProjectsBaseDir(),
                filesystemFramework
        );
        return createFramework(lookup1, filesystemFramework, projectManager);
    }

    public static FilesystemFramework createFilesystemFramework(final File baseDir) {
        //framework lookup property file
        File propertyFile = FilesystemFramework.getPropertyFile(FilesystemFramework.getConfigDir(baseDir));
        //project manager for filesystem

        File projectsBase = determineProjectsBasedir(baseDir, propertyFile);

        return new FilesystemFramework(baseDir, projectsBase);
    }

    /**
     * Returns an instance of Framework object.  Loads the framework.projects.dir property value, or defaults to
     * basedir/projects
     *
     * @return a Framework instance
     */
    public static Framework createForFilesystem(
            final IPropertyLookup lookup, final FilesystemFramework filesystemFramework
    ) {


        FrameworkProjectMgr projectManager = createProjectManager(
                filesystemFramework.getFrameworkProjectsBaseDir(),
                filesystemFramework
        );

        return createFramework(lookup, filesystemFramework, projectManager);
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

    public static Framework createFramework(
            final IPropertyLookup lookup1,
            final FilesystemFramework filesystemFramework,
            final ProjectManager projectManager
    )
    {


        ServiceSupport serviceSupport = new ServiceSupport();

        NodeSupport iFrameworkNodes = new NodeSupport();
        iFrameworkNodes.setLookup(lookup1);

        //framework

        Framework framework = new Framework(
                filesystemFramework,
                projectManager,
                lookup1,
                serviceSupport,
                iFrameworkNodes
        );
        serviceSupport.initialize(framework);

        return framework;
    }
    private static Framework createFramework(
            final PropertyLookup lookup1,
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr projectManager
    )
    {


        ServiceSupport serviceSupport = new ServiceSupport();

        NodeSupport iFrameworkNodes = new NodeSupport();
        iFrameworkNodes.setLookup(lookup1);

        //framework

        Framework framework = new Framework(
                filesystemFramework,
                projectManager,
                lookup1,
                serviceSupport,
                iFrameworkNodes
        );
        serviceSupport.initialize(framework);

        return framework;
    }

    public static FrameworkProjectMgr createProjectManager(
            final File baseDir,
            FilesystemFramework filesystemFramework
    ) {
        return new FrameworkProjectMgr("name", baseDir, filesystemFramework);
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
            final FilesystemFramework filesystemFramework,
            IFrameworkProjectMgr mgr,
            Properties properties
    )
    {
        FrameworkProject frameworkProject = new FrameworkProject(
                projectName,
                baseDir,
                filesystemFramework,
                mgr,
                properties
        );
        frameworkProject.setFramework(filesystemFramework.getFramework());
        ProjectNodeSupport projectNodeSupport = new ProjectNodeSupport(frameworkProject,
                                                                       filesystemFramework.getFramework()
                                                                                          .getResourceFormatGeneratorService(),
                                                                       filesystemFramework.getFramework()
                                                                                          .getResourceModelSourceService()
        );
        frameworkProject.setProjectNodes(projectNodeSupport);
        return frameworkProject;
    }

    public static CentralDispatcher createDispatcher(PropertyRetriever props) {
        String url = props.getProperty("framework.server.url");
        String username = props.getProperty("framework.server.username");
        String password = props.getProperty("framework.server.password");
        return new RundeckAPICentralDispatcher(url, username, password);
    }

    public static CentralDispatcher createDispatcher(
            final String url,
            final String username, final String password
    )
    {
        return new RundeckAPICentralDispatcher(url, username, password);
    }
}
