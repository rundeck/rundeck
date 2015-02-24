package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Manage node source loading for a project
 */
public class ProjectNodeSupport implements IProjectNodes {
    private static final Logger logger = Logger.getLogger(ProjectNodeSupport.class);
    public static final String PROJECT_RESOURCES_URL_PROPERTY = "project.resources.url";
    public static final String PROJECT_RESOURCES_FILE_PROPERTY = "project.resources.file";
    public static final String PROJECT_RESOURCES_FILEFORMAT_PROPERTY = "project.resources.file.format";
    public static final String RESOURCES_SOURCE_PROP_PREFIX = "resources.source";
    public static final String PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES = "project.resources.mergeNodeAttributes";

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

        final List<Map> list = listResourceModelConfigurations();
        int i = 1;
        for (final Map map : list) {
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
    public synchronized List<Map> listResourceModelConfigurations(){
        Map propertiesMap = project.getProperties();
        Properties properties = new Properties();
        properties.putAll(propertiesMap);
        return listResourceModelConfigurations(project.getName(), properties);
    }

    public static List<Map> listResourceModelConfigurations(final String projectName, final Properties props) {
        final ArrayList<Map> list = new ArrayList<Map>();
        int i = 1;
        boolean done = false;
        while (!done) {
            final String prefix = RESOURCES_SOURCE_PROP_PREFIX + "." + i;
            if (props.containsKey(prefix + ".type")) {
                final String providerType = props.getProperty(prefix + ".type");
                final Properties configProps = new Properties();
                configProps.setProperty("project", projectName);
                final int len = (prefix + ".config.").length();
                for (final Object o : props.keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix + ".config.")) {
                        configProps.setProperty(key.substring(len), props.getProperty(key));
                    }
                }
//                logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + configProps);
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

}
