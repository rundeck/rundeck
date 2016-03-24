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

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.net.URI;
import java.util.*;


/**
 * Represents a project in the framework. A project is a repository of installed managed entities
 * organized by their type.
 * <br>
 */
public class FrameworkProject extends FrameworkResourceParent implements IRundeckProject {
    public static final String PROP_FILENAME = "project.properties";
    public static final String ETC_DIR_NAME = "etc";
    public static final String NODES_XML = "resources.xml";
    public static final String PROJECT_RESOURCES_URL_PROPERTY = "project.resources.url";
    public static final String PROJECT_RESOURCES_FILE_PROPERTY = "project.resources.file";
    public static final String PROJECT_RESOURCES_FILEFORMAT_PROPERTY = "project.resources.file.format";
    public static final String PROJECT_RESOURCES_ALLOWED_URL_PREFIX = "project.resources.allowedURL.";
    public static final String FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX = "framework.resources.allowedURL.";
    public static final String RESOURCES_SOURCE_PROP_PREFIX = "resources.source";
    public static final String PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES = "project.resources.mergeNodeAttributes";

    /**
     * Creates an authorization environment for a project.
     * @param project project name
     * @return environment to evaluate authorization for a project
     */
    public static Set<Attribute> authorizationEnvironment(final String project) {
        return Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"),
                project));
    }
    /**
     * Reference to deployments base directory
     */
    private final File resourcesBaseDir;

    /**
     * Reference to the project's config dir
     */
    private final File etcDir;

    private final IFrameworkProjectMgr projectResourceMgr;

    /**
     * reference to PropertyLookup object providing access to project.properties
     */
    private PropertyLookup lookup;
    /**
     * Direct projec properties
     */
    private FilesystemFramework filesystemFramework;
    private Framework framework;
    private IProjectNodesFactory projectNodesFactory;
    private Authorization projectAuthorization;
    private IRundeckProjectConfig projectConfig;
    private IRundeckProjectConfigModifier projectConfigModifier;


    /**
     * Constructor
     *
     * @param name        Name of the project
     * @param basedir     the base directory for the Depot
     * @param resourceMgr manager
     * @param projectConfig  config
     */
    public FrameworkProject(
            final String name,
            final File basedir,
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr resourceMgr,
            final IRundeckProjectConfig projectConfig,
            final IRundeckProjectConfigModifier projectConfigModifier
    )
    {

        super(name, basedir, null);
        this.filesystemFramework=filesystemFramework;
        projectResourceMgr = resourceMgr;
        resourcesBaseDir = new File(getBaseDir(), "resources");
        etcDir = getProjectEtcDir(getBaseDir());
        if (!etcDir.exists()) {
            if (!etcDir.mkdirs()) {
                throw new FrameworkResourceException(
                        "error while creating project structure. " +
                        "failed creating directory: " + etcDir.getAbsolutePath(), this
                );
            }
        }

        this.projectConfig=projectConfig;
        this.projectConfigModifier=projectConfigModifier;
        initialize();
    }

    @Override
    public IProjectInfo getInfo() {
        return new IProjectInfo() {
            @Override
            public String getDescription() {
                return hasProperty("project.description")?getProperty("project.description"):null;
            }

            @Override
            public String getReadme() {
                return readFileResourceContents("readme.md");
            }

            @Override
            public String getReadmeHTML() {
                return null;
            }

            @Override
            public String getMotdHTML() {
                return null;
            }

            @Override
            public String getMotd() {
                return readFileResourceContents("motd.md");
            }
        };
    }

    private String readFileResourceContents(final String path) {
        if (!existsFileResource(path)) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            loadFileResource(path, output);
        } catch (IOException e) {
            return null;
        }
        return output.toString();
    }

    /**
     * Get the etc dir from the basedir
     */
    public static File getProjectEtcDir(File baseDir) {
        return new File(baseDir, ETC_DIR_NAME);
    }

    /**
     * Get the project property file from the basedir
     */
    public static File getProjectPropertyFile(File baseDir) {
        return new File(getProjectEtcDir(baseDir), PROP_FILENAME);
    }

    @Override
    public String getProperty(final String name) {
        return projectConfig.getProperty(name);
    }

    @Override
    public boolean hasProperty(final String key) {
        return projectConfig.hasProperty(key);
    }

    @Override
    public Map<String, String> getProperties() {
        return projectConfig.getProperties();
    }

    @Override
    public Map<String, String> getProjectProperties() {
        return projectConfig.getProjectProperties();
    }

    @Override
    public Date getConfigLastModifiedTime() {
        return projectConfig.getConfigLastModifiedTime();
    }

    /**
     * list the configurations of resource model providers.
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    @Override
    public synchronized List<Map<String, Object>> listResourceModelConfigurations(){
        return getProjectNodes().listResourceModelConfigurations();
    }

    /**
     * @param name        project name
     * @param projectsDir projects dir
     * @param resourceMgr resourcemanager
     *
     * @return Create a new Project object at the specified projects.directory
     */
    public static FrameworkProject create(
            final String name,
            final File projectsDir,
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr resourceMgr,
            IProjectNodesFactory nodesFactory
    )
    {
        return FrameworkFactory.createFrameworkProject(name,
                                                       new File(projectsDir, name),
                                                       filesystemFramework,
                                                       resourceMgr,
                                                       nodesFactory,
                                                       null);
    }

    /**
     * @param name        project name
     * @param projectsDir projects dir
     * @param resourceMgr resourcemanager
     *
     * @return Create a new Project object at the specified projects.directory
     */
    public static FrameworkProject create(
            final String name,
            final File projectsDir,
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr resourceMgr
    )
    {
        return FrameworkFactory.createFrameworkProject(
                name,
                new File(projectsDir, name),
                filesystemFramework,
                resourceMgr,
                FrameworkFactory.createNodesFactory(filesystemFramework),
                null
        );
    }


    public IFrameworkResource loadChild(String name) {
        throw new NoSuchResourceException("project named " + name + " doesn't exist", this);
    }

    public boolean childCouldBeLoaded(String name) {

        final File file = new File(resourcesBaseDir, name);
        return file.exists() && file.isDirectory();
    }

    public Collection<String> listChildNames() {
        HashSet<String> childnames = new HashSet<>();
        if(resourcesBaseDir.isDirectory()){
            final String[] list = resourcesBaseDir.list();
            if (null != list) {
                for (final String aList : list) {
                    final File dir = new File(resourcesBaseDir, aList);
                    if (dir.isDirectory()) {
                        childnames.add(dir.getName());
                    }
                }
            }
        }
        return childnames;
    }

    /**
     * Create a new type and store it
     *
     */
    public IFrameworkResource createChild(final String resourceType) {
        throw new UnsupportedOperationException("createChild");
    }



    public IFrameworkProjectMgr getFrameworkProjectMgr() {
        return projectResourceMgr;
    }

    public static boolean exists(final String project, final IFrameworkProjectMgr projectResourceMgr) {
        return projectResourceMgr.existsFrameworkProject(project);
    }

    /**
     * Gets the config dir for this project
     *
     * @return etc directory
     */
    public File getEtcDir() {
        return etcDir;
    }

    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link INodeSet}
     * @throws NodeFileParserException on parse error
     */
    @Override
    public INodeSet getNodeSet() throws NodeFileParserException {
        return getProjectNodes().getNodeSet();
    }

    /**
     * Conditionally update the nodes resources file if a URL source is defined for it and return
     * true if the update process was invoked and succeeded
     *
     * @return true if the update succeeded, false if it was not performed
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    @Override
    public boolean updateNodesResourceFile() throws UpdateUtils.UpdateException {
        return getProjectNodes().updateNodesResourceFile(ProjectNodeSupport.getNodesResourceFilePath(this, framework));
    }

    /**
     * Update the nodes resources file from a specific URL, with BASIC authentication as provided or
     * as defined in the URL's userInfo section.
     * @param providerURL URL to retrieve resources file definition
     * @param username username or null
     * @param password or null
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs during the update process
     */
    @Override
    public void updateNodesResourceFileFromUrl(
            final String providerURL, final String username,
            final String password
    ) throws UpdateUtils.UpdateException
    {
        getProjectNodes().updateNodesResourceFileFromUrl(
                providerURL,
                username,
                password,
                ProjectNodeSupport.getNodesResourceFilePath(this, framework)
        );
    }


    /**
     * Update the resources file given an input Nodes set
     *
     * @param nodeset nodes
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file or generate
     * nodes
     *
     */
    @Override
    public void updateNodesResourceFile(final INodeSet nodeset) throws UpdateUtils.UpdateException {
       getProjectNodes().updateNodesResourceFile(nodeset,ProjectNodeSupport.getNodesResourceFilePath(this, framework));
    }



   /**
     * Creates the file structure for a project
     *
     * @param projectDir     The project base directory
     * @throws IOException on io error
     */
    public static void createFileStructure(final File projectDir) throws IOException {
       /*
       * create an empty project file structure
       */
        if (! projectDir.exists() && ! projectDir.mkdirs()) {
            throw new IOException("failed creating project base dir: " + projectDir.getAbsolutePath());
        }
       /**
        * Create project etc directory for configuration data
        */
        final File etcDir = new File(projectDir, FrameworkProject.ETC_DIR_NAME);
        if (! etcDir.exists() && ! etcDir.mkdirs()) {
            throw new IOException("failed creating project etc dir: " + etcDir.getAbsolutePath());
        }

    }

    @Override
    public boolean existsFileResource(final String path) {
        File result = new File(getBaseDir(), path);
        return result.exists()&& result.isFile();
    }

    @Override
    public boolean existsDirResource(final String path) {
        File result = new File(getBaseDir(), path);
        return result.exists()&& result.isDirectory();
    }

    @Override
    public List<String> listDirPaths(final String path) {
        if (!existsDirResource(path)) {
            return Collections.emptyList();
        }
        File dir = new File(getBaseDir(), path);
        File[] list = dir.listFiles();
        ArrayList<String> result = new ArrayList<>();
        String prefix=path;
        if(path.endsWith("/")) {
            prefix = path.substring(0, path.length() - 1);
        }
        assert list != null;
        for (File s : list) {
            result.add(prefix + "/" + s.getName() + (s.isDirectory() ? "/" : ""));
        }
        return result;
    }

    @Override
    public boolean deleteFileResource(final String path) {
        File result = new File(getBaseDir(), path);
        return !result.exists() || result.delete();
    }

    @Override
    public long storeFileResource(final String path, final InputStream input) throws IOException {
        File result = new File(getBaseDir(), path);
        if(!result.getParentFile().exists()){
            result.getParentFile().mkdirs();
        }
        try(FileOutputStream fos = new FileOutputStream(result)) {
           return Streams.copyStream(input, fos);
        }
    }

    @Override
    public long loadFileResource(final String path, final OutputStream output) throws IOException {
        File result = new File(getBaseDir(), path);
        try(FileInputStream fis = new FileInputStream(result)) {
            return Streams.copyStream(fis, output);
        }
    }

    protected void generateProjectPropertiesFile(boolean overwrite, Properties properties, boolean addDefault){
        projectConfigModifier.generateProjectPropertiesFile(overwrite, properties, addDefault);
    }
    /**
     * Update the project properties file by setting updating the given properties, and removing
     * any properties that have a prefix in the removePrefixes set
     * @param properties new properties to put in the file
     * @param removePrefixes prefixes of properties to remove from the file
     */
    @Override
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        projectConfigModifier.mergeProjectProperties(properties, removePrefixes);
    }
    /**
     * Set the project properties file contents exactly
     * @param properties new properties to use in the file
     */
    @Override
    public void setProjectProperties(final Properties properties) {
        projectConfigModifier.setProjectProperties(properties);
    }


    /**
     * Checks if project is installed by checking if it's basedir directory exists.
     * @param d Depot object to check.
     * @return true if project exists
     */
    public static boolean isInstalled(final FrameworkProject d) {
        return d.getBaseDir().exists();
    }

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    public ArrayList<Exception> getResourceModelSourceExceptions() {
        return getProjectNodes().getResourceModelSourceExceptions();
    }

    public Framework getFramework() {
        return framework;
    }

    public void setFramework(final Framework framework) {
        this.framework = framework;
    }


    @Override
    public IProjectNodes getProjectNodes() {
        return projectNodesFactory.getNodes(getName());
    }

    @Override
    public Authorization getProjectAuthorization() {
        return projectAuthorization;
    }

    public void setProjectAuthorization(Authorization projectAuthorization) {
        this.projectAuthorization = projectAuthorization;
    }

    public void setProjectNodesFactory(IProjectNodesFactory projectNodesFactory) {
        this.projectNodesFactory = projectNodesFactory;
    }
}
