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

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;


/**
 * Represents a project in the framework. A project is a repository of installed managed entities
 * organized by their type.
 * <br>
 */
public class FrameworkProject extends FrameworkResource implements IRundeckProject {
    public static final String PROP_FILENAME = "project.properties";
    public static final String ETC_DIR_NAME = "etc";
    public static final String NODES_XML = "resources.xml";
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
    private IFilesystemFramework filesystemFramework;
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
            final IFilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr resourceMgr,
            final IRundeckProjectConfig projectConfig,
            final IRundeckProjectConfigModifier projectConfigModifier
    )
    {

        super(name, basedir);
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
     * @param getResourceFormatGeneratorService
     * @param getResourceModelSourceService
     * @param name                              project name
     * @param projectsDir                       projects dir
     * @param resourceMgr                       resourcemanager
     *
     * @return Create a new Project object at the specified projects.directory
     */
    public static FrameworkProject create(
            final String name,
            final File projectsDir,
            final IFilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr resourceMgr,
            final Supplier<ResourceFormatGeneratorService> getResourceFormatGeneratorService,
            final Supplier<ResourceModelSourceService> getResourceModelSourceService
    )
    {
        return FrameworkFactory.createFrameworkProject(
                name,
                new File(projectsDir, name),
                filesystemFramework,
                resourceMgr,
                FrameworkFactory.createNodesFactory(
                        filesystemFramework,
                        getResourceFormatGeneratorService,
                        getResourceModelSourceService
                ),
                null
        );
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
        String canonicalPath = getBaseDir().getCanonicalPath();
        File result = new File(getBaseDir(), path);
        String resultPath = result.getCanonicalPath();
        if (!resultPath.startsWith(canonicalPath)) {
            throw new IOException(String.format("Path is outside of destination directory: %s", result));
        }
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
