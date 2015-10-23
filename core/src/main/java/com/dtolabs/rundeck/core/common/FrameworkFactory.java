package com.dtolabs.rundeck.core.common;

import com.dtolabs.client.services.DispatcherConfig;
import com.dtolabs.client.services.RundeckAPICentralDispatcher;
import com.dtolabs.rundeck.core.authorization.AclsUtil;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.authorization.providers.Policies;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;
import java.util.Map;
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
                filesystemFramework
        );

        return createFramework(lookup, filesystemFramework, projectManager, null);
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
     * @return framework
     */
    public static Framework createFramework(
            final IPropertyLookup lookup1,
            final FilesystemFramework filesystemFramework,
            final ProjectManager projectManager,
            Map<String,FrameworkSupportService> services
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
        if(null!=services) {
            //load predefined services
            for (String s : services.keySet()) {
                framework.setService(s, services.get(s));
            }
        }
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
    public static FrameworkProjectMgr createProjectManager(
            FilesystemFramework filesystemFramework
    ) {
        return new FrameworkProjectMgr("name", filesystemFramework.getFrameworkProjectsBaseDir(), filesystemFramework);
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
        File aclPath = new File(baseDir, "acls");
        if(!aclPath.exists()) {
            aclPath.mkdirs();
        }
        frameworkProject.setProjectAuthorization(
                AclsUtil.createAuthorization(
                        Policies.load(
                                aclPath,
                                AuthorizationUtil.projectContext(
                                        projectName
                                )
                        )
                )
        );
        return frameworkProject;
    }

    /**
     * Return true if the config has values for each config
     * @param config config
     * @return true if valid values are found
     */
    public static boolean isValid(DispatcherConfig config) {
        return null != config.getUrl() &&
               null != config.getUsername() &&
               null != config.getPassword();
    }
    public static DispatcherConfig createDispatcherConfig(PropertyRetriever props){
        final String url = props.getProperty("framework.server.url");
        final String username = props.getProperty("framework.server.username");
        final String password = props.getProperty("framework.server.password");
        return createDispatcherConfig(url, username, password);
    }

    public static DispatcherConfig createDispatcherConfig(
            final String url,
            final String username,
            final String password
    )
    {
        return new DispatcherConfig() {
            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
    }

    public static CentralDispatcher createDispatcher(PropertyRetriever props) {
        return createDispatcher(createDispatcherConfig(props));
    }

    public static CentralDispatcher createDispatcher(final DispatcherConfig config)
    {
        return new RundeckAPICentralDispatcher(config);
    }
}
