package com.dtolabs.rundeck.core.common;

import com.dtolabs.client.services.RundeckAPICentralDispatcher;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
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
        File propertyFile = FilesystemFramework.getPropertyFile(FilesystemFramework.getConfigDir(baseDir));
        String projectsBaseDir = null;
        if (propertyFile.exists()) {
            PropertyRetriever propertyRetriever = FilesystemFramework.createPropertyRetriever(baseDir);
            projectsBaseDir = propertyRetriever.getProperty("framework.projects.dir");
        }
        if (null == projectsBaseDir) {
            projectsBaseDir = FilesystemFramework.getProjectsBaseDir(baseDir);
        }

        if (!baseDir.exists()) {
            throw new IllegalArgumentException("rdeck_base directory does not exist. " + rdeck_base_dir);
        }

        File projectsBase = new File(projectsBaseDir);
        if (!projectsBase.exists() && !projectsBase.mkdirs()) {
            throw new IllegalArgumentException("project base directory could not be created. " + projectsBaseDir);
        }

        FilesystemFramework filesystemFramework = new FilesystemFramework(baseDir, projectsBase);
        FrameworkProjectMgr frameworkProjectMgr = createProjectManager(
                Framework.PROJECTMGR_NAME,
                projectsBase,
                filesystemFramework
        );

        PropertyLookup lookup1 = PropertyLookup.createDeferred(propertyFile);
        lookup1.expand();

        Framework framework = new Framework(filesystemFramework, frameworkProjectMgr, lookup1);
        return framework;
    }

    public static FrameworkProjectMgr createProjectManager(final String name, final File baseDir, FilesystemFramework filesystemFramework) {
        return new FrameworkProjectMgr(name, baseDir, filesystemFramework);
    }

    /**
     *
     * @param projectName
     * @param baseDir
     * @param filesystemFramework
     * @param mgr
     * @param properties
     * @return
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
