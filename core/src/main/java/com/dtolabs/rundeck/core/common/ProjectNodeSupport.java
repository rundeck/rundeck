package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.resources.format.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manage node source loading for a project
 */
public class ProjectNodeSupport implements IProjectNodes {
    private static final Logger logger = Logger.getLogger(ProjectNodeSupport.class);
    public static final String NODES_XML = "resources.xml";
    public static final String PROJECT_RESOURCES_URL_PROPERTY = "project.resources.url";
    public static final String PROJECT_RESOURCES_FILE_PROPERTY = "project.resources.file";
    public static final String PROJECT_RESOURCES_FILEFORMAT_PROPERTY = "project.resources.file.format";
    public static final String RESOURCES_SOURCE_PROP_PREFIX = "resources.source";
    public static final String PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES = "project.resources.mergeNodeAttributes";
    public static final String PROJECT_RESOURCES_ALLOWED_URL_PREFIX = "project.resources.allowedURL.";
    public static final String FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX = "framework.resources.allowedURL.";

    private IRundeckProject project;
    private ArrayList<Exception> nodesSourceExceptions;
    private long nodesSourcesLastReload = -1L;
    private List<ResourceModelSource> nodesSourceList;
    private ResourceFormatGeneratorService resourceFormatGeneratorService;
    private ResourceModelSourceService resourceModelSourceService;

    public ProjectNodeSupport(
            final IRundeckProject project,
            final ResourceFormatGeneratorService resourceFormatGeneratorService,
            final ResourceModelSourceService resourceModelSourceService
    )
    {
        this.project = project;
        this.resourceFormatGeneratorService = resourceFormatGeneratorService;
        this.resourceModelSourceService = resourceModelSourceService;
    }

    static Set<String> uncachedResourceTypes = new HashSet<String>();

    static {
        uncachedResourceTypes.add("file");
        uncachedResourceTypes.add("directory");
    }

    private boolean shouldCacheForType(String type) {
        return !uncachedResourceTypes.contains(type);
    }
    /**
     * Create a {@link NodeSetMerge} based on project configuration, it defaults to merge all node attributes unless "project.resources.mergeNodeAttributes" is false
     *
     * @return a NodeSetMerge
     */
    private NodeSetMerge getNodeSetMerge() {
        if (project.hasProperty(PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES) && "false".equals(
                project.getProperty
                        (PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES))) {
            return new AdditiveListNodeSet();
        }
        return new MergedAttributesNodeSet();
    }

    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link INodeSet}
     */
    @Override
    public INodeSet getNodeSet() {
        //iterate through sources, and add nodes
        final NodeSetMerge list = getNodeSetMerge();
        nodesSourceExceptions = new ArrayList<Exception>();
        for (final ResourceModelSource nodesSource : getResourceModelSources()) {
            try {
                INodeSet nodes = nodesSource.getNodes();
                if (null == nodes) {
                    logger.warn("Empty nodes result from [" + nodesSource.toString() + "]");
                } else {
                    list.addNodeSet(nodes);
                }
            } catch (ResourceModelSourceException e) {
                logger.error("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e);
                nodesSourceExceptions.add(
                        new ResourceModelSourceException(
                                "Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e
                        )
                );
            } catch (RuntimeException e) {
                logger.error("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e);
                nodesSourceExceptions.add(
                        new ResourceModelSourceException(
                                "Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e
                        )
                );
            } catch (Throwable e) {
                logger.error("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e);
                nodesSourceExceptions.add(
                        new ResourceModelSourceException(
                                "Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage()
                        )
                );
            }
        }
        return list;

    }

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    @Override
    public ArrayList<Exception> getResourceModelSourceExceptions() {
        return nodesSourceExceptions;
    }

    private synchronized Collection<ResourceModelSource> getResourceModelSources() {
        //determine if sources need to be reloaded
        final long lastMod = project.getConfigLastModifiedTime()!=null?project.getConfigLastModifiedTime().getTime():0;
        if (lastMod > nodesSourcesLastReload) {
            nodesSourceList = new ArrayList<ResourceModelSource>();
            loadResourceModelSources();
        }
        return nodesSourceList;
    }

    private void loadResourceModelSources() {
        nodesSourceExceptions = new ArrayList<Exception>();
        //generate Configuration for file source
        if (project.hasProperty(PROJECT_RESOURCES_FILE_PROPERTY)) {
            try {
                final Properties config = createFileSourceConfiguration();
                logger.info("Source (project.resources.file): loading with properties: " + config);
                nodesSourceList.add(loadResourceModelSource("file", config, shouldCacheForType("file"), "file.file"));
            } catch (ExecutionServiceException e) {
                logger.error("Failed to load file resource model source: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
        }
        if (project.hasProperty(PROJECT_RESOURCES_URL_PROPERTY)) {
            try {
                final Properties config = createURLSourceConfiguration();
                logger.info("Source (project.resources.url): loading with properties: " + config);
                nodesSourceList.add(loadResourceModelSource("url", config, shouldCacheForType("url"), "file.url"));
            } catch (ExecutionServiceException e) {
                logger.error("Failed to load file resource model source: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
        }

        final List<Map<String, Object>> list = listResourceModelConfigurations();
        int i = 1;
        for (final Map<String, Object> map : list) {
            final String providerType = (String) map.get("type");
            final Properties props = (Properties) map.get("props");

            logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + props);
            try {
                nodesSourceList.add(
                        loadResourceModelSource(
                                providerType, props, shouldCacheForType(providerType),
                                i + "." + providerType
                        )
                );
            } catch (ExecutionServiceException e) {
                logger.error("Failed loading resource model source #" + i + ", skipping: " + e.getMessage(), e);
                nodesSourceExceptions.add(e);
            }
            i++;
        }

        Date configLastModifiedTime = project.getConfigLastModifiedTime();
        nodesSourcesLastReload = configLastModifiedTime!=null?configLastModifiedTime.getTime():-1;
    }

    private Properties createURLSourceConfiguration() {
        final URLResourceModelSource.Configuration build = URLResourceModelSource.Configuration.build();
        build.url(project.getProperty(PROJECT_RESOURCES_URL_PROPERTY));
        build.project(project.getName());

        return build.getProperties();
    }

    private File getResourceModelSourceFileCacheForType(String ident) {
        String varDir = project.getProperty("framework.var.dir");
        File file = new File(varDir, "resourceModelSourceCache/" + project.getName() + "/" + ident + ".xml");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            logger.warn("Failed to create cache dirs for source file cache");
        }
        return file;
    }


    private ResourceModelSource createCachingSource(
            ResourceModelSource sourceForConfiguration,
            String ident,
            String descr
    )
    {
        final File file = getResourceModelSourceFileCacheForType(ident);
        final ResourceModelSourceService nodesSourceService = getResourceModelSourceService();
        final ResourceFormatGeneratorService resourceFormatGeneratorService = getResourceFormatGeneratorService();
        final Properties fileSourceConfig = generateFileSourceConfigurationProperties(
                file.getAbsolutePath(),
                ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE, true, false
        );
        try {
            ResourceModelSource fileSource = nodesSourceService.getSourceForConfiguration("file", fileSourceConfig);

            ResourceFormatGenerator generatorForFormat = resourceFormatGeneratorService.getGeneratorForFormat
                    (ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE);

            String ident1 = "[ResourceModelSource: " + descr + ", project: " + project.getName() + "]";
            return new CachingResourceModelSource(
                    sourceForConfiguration,
                    ident1,
                    new LoggingResourceModelSourceCache(
                            new FileResourceModelSourceCache(file, generatorForFormat, fileSource),
                            ident1
                    )
            );
        } catch (UnsupportedFormatException e) {
            e.printStackTrace();
        } catch (ExecutionServiceException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return resourceFormatGeneratorService;
    }

    private ResourceModelSourceService getResourceModelSourceService() {
        return resourceModelSourceService;
    }

    private ResourceModelSource loadResourceModelSource(
            String type, Properties configuration, boolean useCache,
            String ident
    ) throws ExecutionServiceException
    {

        final ResourceModelSourceService nodesSourceService =
                getResourceModelSourceService();
        configuration.put("project",project.getName());
        ResourceModelSource sourceForConfiguration = nodesSourceService.getSourceForConfiguration(type, configuration);

        if (useCache) {
            ResourceModelSourceFactory provider = nodesSourceService.providerOfType(type);
            String name = ident;
            if (provider instanceof Describable) {
                Describable desc = (Describable) provider;
                Description description = desc.getDescription();
                name = ident + " (" + description.getTitle() + ")";
            }
            return createCachingSource(sourceForConfiguration, ident, name);
        } else {
            return sourceForConfiguration;
        }
    }

    private Properties generateFileSourceConfigurationProperties(
            String filepath, String format, boolean generate,
            boolean includeServerNode
    )
    {
        final FileResourceModelSource.Configuration build = FileResourceModelSource.Configuration.build();
        build.file(filepath);
        if (null != format) {
            build.format(format);
        }
        build.project(project.getName());
        build.generateFileAutomatically(generate);
        build.includeServerNode(includeServerNode);

        return build.getProperties();
    }


    private Properties createFileSourceConfiguration() {
        String format = null;
        if (project.hasProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY)) {
            format = project.getProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY);
        }
        return generateFileSourceConfigurationProperties(
                project.getProperty(PROJECT_RESOURCES_FILE_PROPERTY), format, true,
                true
        );
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
        Map propertiesMap = project.getProperties();
        Properties properties = new Properties();
        properties.putAll(propertiesMap);
        return listResourceModelConfigurations(properties);
    }

    /**
     * Return a list of resource model configuration
     * @param props properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public static List<Map<String, Object>> listResourceModelConfigurations(final Properties props) {
        final ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int i = 1;
        boolean done = false;
        while (!done) {
            final String prefix = RESOURCES_SOURCE_PROP_PREFIX + "." + i;
            if (props.containsKey(prefix + ".type")) {
                final String providerType = props.getProperty(prefix + ".type");
                final Properties configProps = new Properties();
                final int len = (prefix + ".config.").length();
                for (final Object o : props.keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix + ".config.")) {
                        configProps.setProperty(key.substring(len), props.getProperty(key));
                    }
                }
                final HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("type", providerType);
                map.put("props", configProps);
                list.add(map);
            } else {
                done = true;
            }
            i++;
        }
        return list;
    }

    /**
     * @return specific nodes resources file path for the project, based on the framework.nodes.file.name property
     */
    public static String getNodesResourceFilePath(IRundeckProject project, Framework framework) {
        if(project.hasProperty(ProjectNodeSupport.PROJECT_RESOURCES_FILE_PROPERTY)) {
            return new File(project.getProperty(ProjectNodeSupport.PROJECT_RESOURCES_FILE_PROPERTY)).getAbsolutePath();
        }
        if(null!=framework) {
            File etcDir = new File(framework.getFrameworkProjectsBaseDir(), project.getName() + "/etc/");
            if (framework.hasProperty(Framework.NODES_RESOURCES_FILE_PROP)) {
                return new File(etcDir, framework.getProperty(Framework.NODES_RESOURCES_FILE_PROP)).getAbsolutePath();
            } else {
                return new File(etcDir, ProjectNodeSupport.NODES_XML).getAbsolutePath();
            }
        }else{
            return null;
        }
    }
    /**
     * Return true if the resources file should be pulled from the server If he node is the server and workbench
     * integration is enabled then the file should not be updated.
     *
     */
    private boolean shouldUpdateNodesResourceFile() {
        return project.hasProperty(PROJECT_RESOURCES_URL_PROPERTY);
    }
    /**
     * Conditionally update the nodes resources file if a URL source is defined for it and return
     * true if the update process was invoked and succeeded
     *
     * @param nodesResourcesFilePath destination file path
     * @return true if the update succeeded, false if it was not performed
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    @Override
    public boolean updateNodesResourceFile(final String nodesResourcesFilePath) throws UpdateUtils.UpdateException {
        if (shouldUpdateNodesResourceFile()) {
            updateNodesResourceFileFromUrl(project.getProperty(PROJECT_RESOURCES_URL_PROPERTY), null, null,nodesResourcesFilePath);
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
     * @param nodesResourceFilePath path of the destination file
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs during the update process
     */
    @Override
    public void updateNodesResourceFileFromUrl(
            final String providerURL, final String username,
            final String password, final String nodesResourceFilePath
    ) throws UpdateUtils.UpdateException {
        if(!validateResourceProviderURL(providerURL)){
            throw new UpdateUtils.UpdateException("providerURL is not allowed: " + providerURL);
        }

        UpdateUtils.updateFileFromUrl(providerURL, nodesResourceFilePath, username, password,
                                      URLFileUpdater.factory());
        logger.debug("Updated nodes resources file: " + nodesResourceFilePath);
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
        //whitelist the configured providerURL
        if (project.hasProperty(PROJECT_RESOURCES_URL_PROPERTY) && project.getProperty(PROJECT_RESOURCES_URL_PROPERTY).equals(
                providerURL)) {
            return true;
        }
        //check regex properties for project props
        int i = 0;
        boolean projpass = false;
        boolean setproj = false;
        while (project.hasProperty(PROJECT_RESOURCES_ALLOWED_URL_PREFIX + i)) {
            setproj = true;
            final String regex = project.getProperty(PROJECT_RESOURCES_ALLOWED_URL_PREFIX + i);
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
        //check framework props
        i = 0;

        final boolean setframework = project.hasProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i);
        if (!setframework && projpass) {
            //unset in framework.props, allowed by project.props
            return true;
        }
        if(!setframework && !setproj){
            //unset in both
            return false;
        }
        while (project.hasProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i)) {
            final String regex = project.getProperty(FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX + i);
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
     * Update the resources file given an input Nodes set
     *
     * @param nodeset nodes
     *
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file or generate
     *                                     nodes
     */
    @Override
    public void updateNodesResourceFile(final INodeSet nodeset, final String nodesResourceFilePath)
            throws UpdateUtils.UpdateException
    {
        final ResourceFormatGenerator generator;
        File destfile = new File(nodesResourceFilePath);
        try {
            generator =
                    resourceFormatGeneratorService
                            .getGeneratorForFileExtension(destfile);
        } catch (UnsupportedFormatException e) {
            throw new UpdateUtils.UpdateException(
                    "Unable to determine file format for file: " + nodesResourceFilePath, e
            );
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

        updateNodesResourceFile(resfile,nodesResourceFilePath);
        if (!resfile.delete()) {
            logger.warn("failed to remove temp file: " + resfile);
        }
        logger.debug("generated resources file: " + resfile.getAbsolutePath());
    }

    /**
     * Update the resources file from a source file
     *
     * @param source the source file
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     *
     */
    public void updateNodesResourceFile(final File source, final String nodesResourceFilePath) throws UpdateUtils.UpdateException {
        UpdateUtils.updateFileFromFile(source, nodesResourceFilePath);
        logger.debug("Updated nodes resources file: " + nodesResourceFilePath);
    }



}
