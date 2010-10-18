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

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import com.dtolabs.shared.resources.ResourceXMLGenerator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.filters.ExpandProperties;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Property;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;


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
    private final PropertyLookup lookup;

    /**
     * Constructor
     *
     * @param name    Name of the project
     * @param basedir the base directory for the Depot
     */
    public FrameworkProject(final String name, final File basedir, final IFrameworkProjectMgr resourceMgr) {
        super(name, basedir, resourceMgr);
        projectResourceMgr = resourceMgr;
        resourcesBaseDir = new File(getBaseDir(), "resources");
        etcDir = new File(getBaseDir(), ETC_DIR_NAME);
        if (!etcDir.exists()) {
            if (!etcDir.mkdirs()) {
                throw new FrameworkResourceException("error while creating project structure. " +
                        "failed creating directory: " + etcDir.getAbsolutePath(), this );
            }
        }

        if ( ! (new File(getEtcDir(), PROP_FILENAME).exists()) ) {
            generateProjectPropertiesFile(false);
        }
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", name);
        final File fwkDepotPropertyFile = new File(projectResourceMgr.getFramework().getConfigDir(), PROP_FILENAME);
        final Properties nodeWideDepotProps = PropertyLookup.fetchProperties(fwkDepotPropertyFile);
        nodeWideDepotProps.putAll(ownProps);
        propertyFile = new File(getEtcDir(), PROP_FILENAME);
        if (propertyFile.exists()) {
            lookup = PropertyLookup.create(propertyFile,
                    nodeWideDepotProps, projectResourceMgr.getFramework().getPropertyLookup());
            getLogger().debug("loading existing project.properties: " + propertyFile.getAbsolutePath());

        } else {
            lookup = PropertyLookup.create(fwkDepotPropertyFile,
                    ownProps, projectResourceMgr.getFramework().getPropertyLookup());
            getLogger().debug("loading instance-level project.properties: " + propertyFile.getAbsolutePath());

        }
        lookup.expand();

        final String resfilepath = getNodesResourceFilePath();
        File resfile= new File(resfilepath);
        if(!resfile.isFile()){
            generateResourcesFile(resfile);
        }
        initialize();
    }

    private void generateResourcesFile(final File resfile) {

        final Framework framework = projectResourceMgr.getFramework();
        final NodeEntryImpl node = framework.createFrameworkNode();
        node.setFrameworkProject(getName());
        final ResourceXMLGenerator xmlGenerator = new ResourceXMLGenerator(resfile);
        xmlGenerator.addNode(node);
        try {
            xmlGenerator.generate();
        } catch (IOException e) {
            getLogger().error("Unable to generate resources file: " + e.getMessage(), e);
        }

        getLogger().debug("generated resources file: " + resfile.getAbsolutePath());
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
        if(framework.existsProperty(Framework.NODES_RESOURCES_FILE_PROP)){
            return new File(getEtcDir(), framework.getProperty(Framework.NODES_RESOURCES_FILE_PROP)).getAbsolutePath();
        }else{
            return new File(getEtcDir(), NODES_XML).getAbsolutePath();
        }
    }
    /**
     * Returns a {@link Nodes} object conatining the resources.properties config data
     *
     * @return an instance of {@link Nodes}
     */
    public Nodes getNodes() throws NodeFileParserException {
        String path = getNodesResourceFilePath();
        return getNodes(new File(path));
    }

    private HashMap<File, Long> nodesFileTimes = new HashMap<File, Long>();
    private HashMap<File, Nodes> nodesCache = new HashMap<File, Nodes>();

    /**
     * Returns a {@link Nodes} object conatining the nodes config data.
     *
     * @param nodesFile the source file
     * @return an instance of {@link Nodes}
     */
    public Nodes getNodes(final File nodesFile) throws NodeFileParserException {
        final Nodes.Format format;
        if (nodesFile.getName().endsWith(".xml")) {
            format = Nodes.Format.projectxml;
        }else {
            throw new NodeFileParserException("Unable to determine file format for file: " + nodesFile.getAbsolutePath());
        }
        final Long modtime = nodesFile.lastModified();
        if ( null == nodesCache.get(nodesFile) || !modtime.equals(nodesFileTimes.get(nodesFile)) ) {
            final Nodes nodes = Nodes.create(this, nodesFile, format);
            nodesFileTimes.put(nodesFile, modtime);
            nodesCache.put(nodesFile, nodes);
            return nodes;
        } else {
            return nodesCache.get(nodesFile);
        }

    }

    /**
     * Conditionally update the nodes resources file if a URL source is defined for it 
     *
     * @throws UpdateUtils.UpdateException
     *
     */
    public void updateNodesResourceFile() throws UpdateUtils.UpdateException {
        if (shouldUpdateNodesResourceFile()) {
            UpdateUtils.updateFileFromUrl(getProperty(PROJECT_RESOURCES_URL_PROPERTY), getNodesResourceFilePath());
            logger.debug("Updated nodes resources file: " + getNodesResourceFilePath());
        }
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
    public String getProperty(final String name) {
        return lookup.getProperty(name);
    }


    public boolean hasProperty(final String key) {
        return lookup.hasProperty(key);
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
        Copy copyTask = new Copy();
        Project antProject = new Project();
        antProject.setProperty("project.name", getName());
        Property propTask = new Property();
        propTask.setProject(antProject);
        propTask.setFile(new File(Constants.getFrameworkProperties(
                getFrameworkProjectMgr().getFramework().getBaseDir().getAbsolutePath())));
        propTask.execute();
        copyTask.setProject(antProject);
        copyTask.setOverwrite(overwrite);
        final File destfile = new File(getEtcDir(), PROP_FILENAME);
        copyTask.setTofile(destfile);
        copyTask.setFile(new File(getFrameworkProjectMgr().getFramework().getConfigDir(), PROP_FILENAME));
        copyTask.setFiltering(true);
        copyTask.createFilterChain().add(new ExpandProperties());

        // execute the task
        copyTask.execute();
        getLogger().debug("generated project.properties: "+ destfile.getAbsolutePath());
    }

    /**
     * Checks if project is installed by checking if it's basedir directory exists.
     * @param d Depot object to check.
     * @return true if project exists
     */
    public static boolean isInstalled(final FrameworkProject d) {
        return d.getBaseDir().exists();
    }

}
