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

import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException;
import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents a project in the framework. A project is a repository of installed managed entities
 * organized by their type.
 * <p/>
 */
public class FrameworkProject extends FrameworkResourceParent {
    public static final String PROP_FILENAME = "project.properties";
    public static final String ETC_DIR_NAME = "etc";
    public static final String NODES_XML = "resources.xml";
    public static final String PROJECT_RESOURCES_URL_PROPERTY = "project.resources.url";
    public static final String PROJECT_RESOURCES_FILE_PROPERTY = "project.resources.file";
    public static final String PROJECT_RESOURCES_FILEFORMAT_PROPERTY = "project.resources.file.format";
    public static final String PROJECT_RESOURCES_ALLOWED_URL_PREFIX = "project.resources.allowedURL.";
    public static final String FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX = "framework.resources.allowedURL.";
    public static final String RESOURCES_SOURCE_PROP_PREFIX = "resources.source";
    /**
     * Reference to deployments base directory
     */
    private final File resourcesBaseDir;

    /**
     * Reference to the project's config dir
     */
    private final File etcDir;

    private final IFrameworkProjectMgr projectResourceMgr;

    private final File propertyFile;

    /**
     * reference to PropertyLookup object providing access to project.properties
     */
    private PropertyLookup lookup;
    private List<ResourceModelSource> nodesSourceList;

    /**
     * Constructor
     *
     * @param name    Name of the project
     * @param basedir the base directory for the Depot
     */
    public FrameworkProject(final String name, final File basedir, final IFrameworkProjectMgr resourceMgr) {
        this(name, basedir, resourceMgr, null);
    }
    /**
     * Constructor
     *
     * @param name    Name of the project
     * @param basedir the base directory for the Depot
     */
    public FrameworkProject(final String name, final File basedir, final IFrameworkProjectMgr resourceMgr, final Properties properties) {
        super(name, basedir, resourceMgr);
        projectResourceMgr = resourceMgr;
        resourcesBaseDir = new File(getBaseDir(), "resources");
        etcDir = getProjectEtcDir(getBaseDir());
        if (!etcDir.exists()) {
            if (!etcDir.mkdirs()) {
                throw new FrameworkResourceException("error while creating project structure. " +
                        "failed creating directory: " + etcDir.getAbsolutePath(), this );
            }
        }

        propertyFile = getProjectPropertyFile(getBaseDir());
        if ( !propertyFile.exists()) {
            generateProjectPropertiesFile(false, properties);
        }
        loadProperties();

        nodesSourceList = new ArrayList<ResourceModelSource>();

        initialize();
    }

    /**
     * Get the etc dir from the basedir
     * @param baseDir
     * @return
     */
    private static File getProjectEtcDir(File baseDir) {
        return new File(baseDir, ETC_DIR_NAME);
    }

    /**
     * Get the project property file from the basedir
     * @param baseDir
     * @return
     */
    private static File getProjectPropertyFile(File baseDir) {
        return new File(getProjectEtcDir(baseDir), PROP_FILENAME);
    }

    private long propertiesLastReload=0L;
    private synchronized void checkReloadProperties(){
        if (needsPropertiesReload()) {
            loadProperties();
        }
    }

    private boolean needsPropertiesReload(){
        final File fwkProjectPropertyFile = new File(projectResourceMgr.getFramework().getConfigDir(), PROP_FILENAME);
        final long fwkPropsLastModified = fwkProjectPropertyFile.lastModified();
        if(propertyFile.exists()){
            return propertyFile.lastModified()>propertiesLastReload || fwkPropsLastModified>propertiesLastReload;
        }else{
            return fwkPropsLastModified > propertiesLastReload;
        }
    }
    private synchronized void loadProperties() {
        //generic framework properties for a project
        final File fwkProjectPropertyFile = new File(projectResourceMgr.getFramework().getConfigDir(), PROP_FILENAME);

        lookup = createProjectPropertyLookup(projectResourceMgr.getFramework().getBaseDir(), getName());

        if (propertyFile.exists()) {
            getLogger().debug("loading existing project.properties: " + propertyFile.getAbsolutePath());
            final long fwkPropsLastModified = fwkProjectPropertyFile.lastModified();
            final long propsLastMod = propertyFile.lastModified();
            propertiesLastReload = propsLastMod > fwkPropsLastModified ? propsLastMod : fwkPropsLastModified;
        } else {
            getLogger().debug("loading instance-level project.properties: " + propertyFile.getAbsolutePath());
            propertiesLastReload = fwkProjectPropertyFile.lastModified();
        }
    }

    /**
     * Create PropertyLookup for a project from the framework basedir
     * @param baseDir the framework basedir
     * @param projectName
     * @return
     */
    private static PropertyLookup createProjectPropertyLookup(File baseDir, String projectName) {
        PropertyLookup lookup;
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);

        //generic framework properties for a project
        final File fwkProjectPropertyFile = Framework.getPropertyFile(Framework.getConfigDir(baseDir));
        final Properties nodeWideDepotProps = PropertyLookup.fetchProperties(fwkProjectPropertyFile);
        nodeWideDepotProps.putAll(ownProps);

        final File propertyFile = getProjectPropertyFile(new File(Framework.getProjectsBaseDir(baseDir), projectName));

        if (propertyFile.exists()) {
            lookup = PropertyLookup.create(propertyFile,
                    nodeWideDepotProps, Framework.createPropertyLookupFromBasedir(baseDir));
        } else {
            lookup = PropertyLookup.create(fwkProjectPropertyFile,
                    ownProps, Framework.createPropertyLookupFromBasedir(baseDir));
        }
        lookup.expand();
        return lookup;
    }

    /**
     * Create a property retriever for a project given the framework basedir
     * @param baseDir the framework basedir
     * @param projectName
     * @return
     */
    public static PropertyRetriever createProjectPropertyRetriever(File baseDir, String projectName) {
        return createProjectPropertyLookup(baseDir, projectName).safe();
    }

    private ArrayList<Exception> nodesSourceExceptions;
    private long nodesSourcesLastReload = 0L;
    private void loadResourceModelSources() {
        nodesSourceExceptions = new ArrayList<Exception>();
        //generate Configuration for file source
        if (hasProperty(PROJECT_RESOURCES_FILE_PROPERTY)) {
            try {
                final Properties config = createFileSourceConfiguration();
                logger.info("Source (project.resources.file): loading with properties: " + config);
                nodesSourceList.add(loadResourceModelSource("file", config));
            } catch (ExecutionServiceException e) {
                logger.error("Failed to load file resource model source: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
        }
        if(hasProperty(PROJECT_RESOURCES_URL_PROPERTY)) {
            try{
                final Properties config = createURLSourceConfiguration();
                logger.info("Source (project.resources.url): loading with properties: " + config);
                nodesSourceList.add(loadResourceModelSource("url", config));
            } catch (ExecutionServiceException e) {
                logger.error("Failed to load file resource model source: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
        }

        final List<Map> list = listResourceModelConfigurations();
        int i=1;
        for (final Map map : list) {
            final String providerType = (String) map.get("type");
            final Properties props = (Properties) map.get("props");

            logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + props);
            try {
                nodesSourceList.add(loadResourceModelSource(providerType, props));
            } catch (ExecutionServiceException e) {
                logger.error("Failed loading resource model source #" + i + ", skipping: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
            i++;
        }

        nodesSourcesLastReload = getPropertyFile().lastModified();
    }
    /**
     * list the configurations of resource model providers.  Returns a list of maps containing:
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     */
    public synchronized List<Map> listResourceModelConfigurations(){
        final ArrayList<Map> list = new ArrayList<Map>();
        int i = 1;
        boolean done = false;
        while (!done) {
            final String prefix = RESOURCES_SOURCE_PROP_PREFIX + "." + i;
            if (hasProperty(prefix + ".type")) {
                final String providerType = getProperty(prefix + ".type");
                final Properties props = new Properties();
                props.setProperty("project", getName());
                final int len = (prefix + ".config.").length();
                for (final Object o : lookup.getPropertiesMap().keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix + ".config.")) {
                        props.setProperty(key.substring(len), getProperty(key));
                    }
                }
                logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + props);
                final HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("type", providerType);
                map.put("props", props);
                list.add(map);
            } else {
                done = true;
            }
            i++;
        }
        return list;
    }

    private Properties createURLSourceConfiguration() {
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();
        build.url(getProperty(PROJECT_RESOURCES_URL_PROPERTY));
        build.project(getName());

        return build.getProperties();
    }

    private synchronized Collection<ResourceModelSource> getResourceModelSources() {
        //determine if sources need to be reloaded
        final long lastMod = getPropertyFile().lastModified();
        if(lastMod> nodesSourcesLastReload){
            nodesSourceList = new ArrayList<ResourceModelSource>();
            loadResourceModelSources();
        }
        return nodesSourceList;
    }

    private ResourceModelSource loadResourceModelSource(String type, Properties configuration) throws ExecutionServiceException {

        final ResourceModelSourceService nodesSourceService =
            getFrameworkProjectMgr().getFramework().getResourceModelSourceService();
        return nodesSourceService.getSourceForConfiguration(type, configuration);
    }

    private Properties createFileSourceConfiguration() {
        final FileResourceModelSource.Configuration build = FileResourceModelSource.Configuration.build();
        build.file(getProperty(PROJECT_RESOURCES_FILE_PROPERTY));
        if(hasProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY)){
            build.format(getProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY));
        }
        build.project(getName());
        build.generateFileAutomatically(true);
        build.includeServerNode(true);

        return build.getProperties();
    }
    


    /**
     * Create a new Depot object at the specified projects.directory
     * @param name
     * @param projectsDir
     * @param resourceMgr
     * @return
     */
    public static FrameworkProject create(final String name, final File projectsDir, final IFrameworkProjectMgr resourceMgr) {
        return new FrameworkProject(name, new File(projectsDir, name), resourceMgr);
    }
    /**
     * Create a new Depot object at the specified projects.directory
     * @param name
     * @param projectsDir
     * @param resourceMgr
     * @return
     */
    public static FrameworkProject create(final String name, final File projectsDir, final IFrameworkProjectMgr resourceMgr, final Properties properties) {
        return new FrameworkProject(name, new File(projectsDir, name), resourceMgr, properties);
    }


    public IFrameworkResource loadChild(String name) {
        throw new NoSuchResourceException("project named " + name + " doesn't exist", this);
    }

    public boolean childCouldBeLoaded(String name) {

        final File file = new File(resourcesBaseDir, name);
        return file.exists() && file.isDirectory();
    }

    public Collection listChildNames() {
        HashSet childnames = new HashSet();
        if(resourcesBaseDir.isDirectory()){
            final String[] list = resourcesBaseDir.list();
            if (null != list) {
                for (int i = 0; i < list.length; i++) {
                    final File dir = new File(resourcesBaseDir, list[i]);
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
     * @param resourceType
     * @return
     */
    public IFrameworkResource createChild(final String resourceType) {
        throw new UnsupportedOperationException("createChild");
    }


    public File getPropertyFile() {
        return propertyFile;
    }

    public IFrameworkProjectMgr getFrameworkProjectMgr() {
        return projectResourceMgr;
    }

    public static boolean exists(final String project, final IFrameworkProjectMgr projectResourceMgr) {
        return projectResourceMgr.existsFrameworkProject(project);
    }

    public boolean existsFrameworkType(final String name) {
        return existsChild(name);
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
     * Return specific nodes resources file path for the project, based on the framework.nodes.file.name property
     * @return
     */
    public String getNodesResourceFilePath() {
        if(hasProperty(PROJECT_RESOURCES_FILE_PROPERTY)) {
            return new File(getProperty(PROJECT_RESOURCES_FILE_PROPERTY)).getAbsolutePath();
        }
        final Framework framework = projectResourceMgr.getFramework();
        final String s;
        if(framework.hasProperty(Framework.NODES_RESOURCES_FILE_PROP)){
            return new File(getEtcDir(), framework.getProperty(Framework.NODES_RESOURCES_FILE_PROP)).getAbsolutePath();
        }else{
            return new File(getEtcDir(), NODES_XML).getAbsolutePath();
        }
    }
    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link INodeSet}
     */
    public INodeSet getNodeSet() throws NodeFileParserException {
        //iterate through sources, and add nodes
        final AdditiveListNodeSet list = new AdditiveListNodeSet();
        nodesSourceExceptions = new ArrayList<Exception>();
        for (final ResourceModelSource nodesSource : getResourceModelSources()) {
            try {
                list.addNodeSet(nodesSource.getNodes());
            } catch (ResourceModelSourceException e) {
                logger.error("Cannot get nodes from ["+nodesSource.toString()+"]: "+e.getMessage(), e);
                nodesSourceExceptions.add(new ResourceModelSourceException(
                    "Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e));
            }
        }
        return list;

    }

    /**
     * Conditionally update the nodes resources file if a URL source is defined for it and return
     * true if the update process was invoked and succeeded
     *
     * @return true if the update succeeded, false if it was not performed
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    public boolean updateNodesResourceFile() throws UpdateUtils.UpdateException {
        if (shouldUpdateNodesResourceFile()) {
            updateNodesResourceFileFromUrl(getProperty(PROJECT_RESOURCES_URL_PROPERTY), null, null);
            return true;
        }
        return false;
    }

    /**
     * Update the nodes resources file from a specific URL, with BASIC authentication as provided or
     * as defined in the URL's userInfo section.
     * @param providerURL URL to retrieve resources file definition
     * @param username username or null
     * @param password or null
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs during the update process
     */
    public void updateNodesResourceFileFromUrl(final String providerURL, final String username,
                                              final String password) throws UpdateUtils.UpdateException {
        if(!validateResourceProviderURL(providerURL)){
            throw new UpdateUtils.UpdateException("providerURL is not allowed: " + providerURL);
        }
        UpdateUtils.updateFileFromUrl(providerURL, getNodesResourceFilePath(), username, password,
            URLFileUpdater.factory());
        logger.debug("Updated nodes resources file: " + getNodesResourceFilePath());
    }

    /**
     * Return true if the URL is valid, and allowed by configuration
     */
    boolean validateResourceProviderURL(final String providerURL) throws UpdateUtils.UpdateException {
        final URL url;
        try {
            url= new URL(providerURL);
        } catch (MalformedURLException e) {
            throw new UpdateUtils.UpdateException("Invalid URL: " + providerURL, e);
        }
        //assert allowed URL scheme
        if(!("file".equals(url.getProtocol()) || "http".equals(url.getProtocol()) || "https".equals(url.getProtocol()))) {
            throw new UpdateUtils.UpdateException("URL protocol not allowed: " + url.getProtocol());
        }

        return isAllowedProviderURL(providerURL);
    }

    /**
     * Return true in these cases:
     *  1. project.properties allows URL and framework.properties allows URL.
     *  2. project.properties allows URL and no regexes are set in framework.properties
     *  3. project.properties no regexes are set, and framework.properites allows URL.
     */
    boolean isAllowedProviderURL(final String providerURL) {
        checkReloadProperties();
        //whitelist the configured providerURL
        if (hasProperty(PROJECT_RESOURCES_URL_PROPERTY) && getProperty(PROJECT_RESOURCES_URL_PROPERTY).equals(
            providerURL)) {
            return true;
        }
        //check regex properties for project props
        int i = 0;
        boolean projpass = false;
        boolean setproj = false;
        while (hasProperty(PROJECT_RESOURCES_ALLOWED_URL_PREFIX + i)) {
            setproj = true;
            final String regex = getProperty(PROJECT_RESOURCES_ALLOWED_URL_PREFIX + i);
            final Pattern pat = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pat.matcher(providerURL);
            if (matcher.matches()) {
                logger.debug(
                    "ProviderURL allowed by project property \"project.resources.allowedURL." + i + "\": " + regex);
                projpass = true;
                break;
            }
            i++;
        }
        if (!projpass && setproj) {
            //was checked but failed match
            return false;
        }
        assert projpass ^ !setproj;
        //check framework props
        i = 0;

        final Framework framework = getFrameworkProjectMgr().getFramework();
        final boolean setframework = framework.hasProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i);
        if (!setframework && projpass) {
            //unset in framework.props, allowed by project.props
            return true;
        }
        if(!setframework && !setproj){
            //unset in both
            return false;
        }
        while (framework.hasProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i)) {
            final String regex = framework.getProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i);
            final Pattern pat = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pat.matcher(providerURL);
            if (matcher.matches()) {
                logger.debug(
                    "ProviderURL allowed by framework property \"framework.resources.allowedURL." + i + "\": " + regex);
                //allowed by framework.props, and unset or allowed by project.props,
                return true;
            }
            i++;
        }
        if (projpass) {
            logger.warn("providerURL was allowed by project.properties, but is not allowed by framework.properties: "
                        + providerURL);
        }
        return false;
    }

    /**
     * Update the resources file from a source file
     *
     * @param source the source file
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    public void updateNodesResourceFile(final File source) throws UpdateUtils.UpdateException {
        UpdateUtils.updateFileFromFile(source, getNodesResourceFilePath());
        logger.debug("Updated nodes resources file: " + getNodesResourceFilePath());
    }

    /**
     * Update the resources file given an input Nodes set
     *
     * @param source the source nodes
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file or generate
     * nodes
     *
     */
    public void updateNodesResourceFile(final INodeSet nodeset) throws UpdateUtils.UpdateException {
        final String nodesResourceFilePath = getNodesResourceFilePath();
        final ResourceFormatGenerator generator;
        File destfile = new File(getNodesResourceFilePath());
        try {
            generator =
                getFrameworkProjectMgr().getFramework().getResourceFormatGeneratorService()
                    .getGeneratorForFileExtension(destfile);
        } catch (UnsupportedFormatException e) {
            throw new UpdateUtils.UpdateException(
                "Unable to determine file format for file: " + nodesResourceFilePath,e);
        }
        File resfile = null;
        try {
            resfile = File.createTempFile("resource-temp", destfile.getName());
            resfile.deleteOnExit();
        } catch (IOException e) {
            throw new UpdateUtils.UpdateException("Unable to create temp file: " + e.getMessage(), e);
        }
        //serialize nodes and replace the nodes resource file

        try {
            final FileOutputStream stream = new FileOutputStream(resfile);
            try {
                generator.generateDocument(nodeset, stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            throw new UpdateUtils.UpdateException("Unable to generate resources file: " + e.getMessage(), e);
        } catch (ResourceFormatGeneratorException e) {
            throw new UpdateUtils.UpdateException("Unable to generate resources file: " + e.getMessage(), e);
        }

        updateNodesResourceFile(resfile);
        if(!resfile.delete()) {
            getLogger().warn("failed to remove temp file: " + resfile);
        }
        getLogger().debug("generated resources file: " + resfile.getAbsolutePath());
    }

    /**
     * Return true if the resources file should be pulled from the server If he node is the server and workbench
     * integration is enabled then the file should not be updated.
     *
     * @return
     */
    private boolean shouldUpdateNodesResourceFile() {
        return hasProperty(PROJECT_RESOURCES_URL_PROPERTY);
    }


    /**
     * Return the property value by name
     *
     * @param name
     * @return
     */
    public synchronized String getProperty(final String name) {
        checkReloadProperties();
        return lookup.getProperty(name);
    }


    public synchronized boolean hasProperty(final String key) {
        checkReloadProperties();
        return lookup.hasProperty(key);
    }
    public Map getProperties() {
        return lookup.getPropertiesMap();
    }


    /**
     * Return a PropertyRetriever interface for project-scoped properties
     */
    public synchronized PropertyRetriever getPropertyRetriever() {
        checkReloadProperties();
        return lookup.safe();
    }


   /**
     * Creates the file structure for a project
     *
     * @param projectDir     The project base directory
    * @param moduleDir     The project module directory    *
     * @param createModLib Create a project module library
     * @throws IOException
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

    /**
     * Create project.properties file based on $RDECK_BASE/etc/project.properties
     * @param overwrite Overwrite existing properties file
     */
    protected void generateProjectPropertiesFile(final boolean overwrite) {
        generateProjectPropertiesFile(overwrite, null);
    }

    /**
     * Create project.properties file based on $RDECK_BASE/etc/project.properties
     *
     * @param overwrite Overwrite existing properties file
     */
    protected void generateProjectPropertiesFile(final boolean overwrite, final Properties properties) {
        generateProjectPropertiesFile(overwrite, properties, false, null);
    }

    /**
     * Create project.properties file based on $RDECK_BASE/etc/project.properties
     *
     * @param overwrite Overwrite existing properties file
     * @param properties properties to use
     * @param merge if true, merge existing properties that are not replaced
     * @param removePrefixes set of property prefixes to remove from original
     */
    protected void generateProjectPropertiesFile(final boolean overwrite, final Properties properties,
                                                 final boolean merge, final Set<String> removePrefixes) {
        final File destfile = getPropertyFile();
        if (destfile.exists() && !overwrite) {
            return;
        }
        final Properties newProps = new Properties();
        newProps.setProperty("project.name", getName());
        newProps.setProperty("project.resources.file", new File(getEtcDir(), "resources.xml").getAbsolutePath());
        if(merge) {
            final Properties orig = new Properties();

            if(destfile.exists()){
                try {
                    final FileInputStream fileInputStream = new FileInputStream(destfile);
                    try {
                        orig.load(fileInputStream);
                    } finally {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //add all original properties that are not in the incoming  properties, and are not
            //matched by one of the remove prefixes
            entry: for (final Object o : orig.entrySet()) {
                Map.Entry entry=(Map.Entry) o;
                //determine if
                final String key = (String) entry.getKey();
                for (final String replacePrefix : removePrefixes) {
                    if(key.startsWith(replacePrefix)){
                        //skip this key
                        continue entry;
                    }
                }
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        //overwrite original with the input properties
        if (null != properties) {
            newProps.putAll(properties);
        }

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(destfile);
            try {
                newProps.store(fileOutputStream, "Project " + getName() + " configuration, generated");
            } finally {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        getLogger().debug("generated project.properties: " + destfile.getAbsolutePath());
    }
    /**
     * Update the project properties file by setting updating the given properties, and removing
     * any properties that have a prefix in the removePrefixes set
     * @param properties new properties to put in the file
     * @param removePrefixes prefixes of properties to remove from the file
     */
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        generateProjectPropertiesFile(true, properties, true, removePrefixes);
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
     * Return the set of exceptions produced by the last attempt to invoke all node providers
     */
    public ArrayList<Exception> getResourceModelSourceExceptions() {
        return nodesSourceExceptions;
    }
}
