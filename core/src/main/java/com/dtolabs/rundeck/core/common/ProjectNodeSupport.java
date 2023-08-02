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

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.resources.format.*;
import com.dtolabs.rundeck.core.utils.TextUtils;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manage node source loading for a project
 */
public class ProjectNodeSupport implements IProjectNodes, Closeable {
    private static final Logger logger                                  = LoggerFactory.getLogger(ProjectNodeSupport.class);
    public static final  String PROJECT_RESOURCES_URL_PROPERTY          = "project.resources.url";
    public static final  String PROJECT_RESOURCES_FILE_PROPERTY         = "project.resources.file";
    public static final  String RESOURCES_SOURCE_PROP_PREFIX            = "resources.source";
    public static final  String NODE_ENHANCER_PROP_PREFIX               = "nodes.plugin";
    public static final  String PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES = "project.resources.mergeNodeAttributes";

    private IRundeckProjectConfig                                                  projectConfig;
    private final Map<String, Throwable>                                           nodesSourceExceptions;
    private long                                                                   nodesSourcesLastReload = -1L;
    private List<LoadedResourceModelSource>                                        nodesSourceList;
    /**
     * Closeables for releasing plugin loaders when sources are disposed
     */
    private Set<Closeable>
                                                                                   nodeSourceReferences   =
        new HashSet<>();
    private ResourceFormatGeneratorService                                         resourceFormatGeneratorService;
    private ResourceModelSourceService                                             resourceModelSourceService;
    private NodeSourceLoader     nodeSourceLoader;
    private boolean                                                                sourcesOpened;

    /**
     * @param projectConfig
     * @param resourceFormatGeneratorService
     * @param nodeSourceLoader model source provider
     */
    public ProjectNodeSupport(
        final IRundeckProjectConfig projectConfig,
        final ResourceFormatGeneratorService resourceFormatGeneratorService,
        final ResourceModelSourceService resourceModelSourceService,
        final NodeSourceLoader     nodeSourceLoader
    ) {
        this.projectConfig = projectConfig;
        this.resourceFormatGeneratorService = resourceFormatGeneratorService;
        this.resourceModelSourceService = resourceModelSourceService;
        this.nodeSourceLoader = nodeSourceLoader;
        this.nodesSourceExceptions = Collections.synchronizedMap(new HashMap<>());
    }
    /**
     * @param projectConfig
     * @param resourceFormatGeneratorService
     * @deprecated use {@link #ProjectNodeSupport(IRundeckProjectConfig, ResourceFormatGeneratorService, ResourceModelSourceService, Function)}
     */
    public ProjectNodeSupport(
        final IRundeckProjectConfig projectConfig,
        final ResourceFormatGeneratorService resourceFormatGeneratorService,
        final ResourceModelSourceService resourceModelSourceService
    ) {
        this.projectConfig = projectConfig;
        this.resourceFormatGeneratorService = resourceFormatGeneratorService;
        this.resourceModelSourceService = resourceModelSourceService;
        this.nodesSourceExceptions = Collections.synchronizedMap(new HashMap<>());
    }

    //TODO: add flag to ResourceModelSource that allows disabling local file cache
    private static Set<String> uncachedResourceTypes = new HashSet<>();

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
        if (projectConfig.hasProperty(PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES) && "false".equals(
                projectConfig.getProperty
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
        Map<String,Exception> exceptions = Collections.synchronizedMap(new HashMap<>());
        int index=1;

        nodesSourceExceptions.clear();
        for (final ResourceModelSource nodesSource : getResourceModelSourcesInternal()) {
            try {
                INodeSet nodes = nodesSource.getNodes();
                if (null == nodes) {
                    logger.warn("Empty nodes result from [" + nodesSource.toString() + "]");
                } else {
                    list.addNodeSet(nodes);
                }
                if(nodesSource instanceof ResourceModelSourceErrors){
                    ResourceModelSourceErrors nodeerrors = (ResourceModelSourceErrors) nodesSource;
                    List<String> modelSourceErrors = nodeerrors.getModelSourceErrors();
                    if(modelSourceErrors!=null && modelSourceErrors.size()>0){

                        logger.error("Some errors getting nodes from [" +
                                     nodesSource.toString() +
                                     "]: " +
                                     modelSourceErrors);
                        exceptions.put(
                                index + ".source",
                                new ResourceModelSourceException(
                                        TextUtils.join(
                                                modelSourceErrors.toArray(new String[0]),
                                                ';'
                                        )
                                )
                        );
                    }
                }
            } catch (ResourceModelSourceException | RuntimeException e) {
                logger.error("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage());
                logger.debug("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e);
                exceptions.put(
                        index+".source",
                        new ResourceModelSourceException(
                                e.getMessage(), e
                        )
                );
            } catch (Throwable e) {
                logger.error("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage());
                logger.debug("Cannot get nodes from [" + nodesSource.toString() + "]: " + e.getMessage(), e);
                exceptions.put(
                        index+".source",
                        new ResourceModelSourceException(
                                e.getMessage()
                        )
                );
            }
            index++;
        }
        synchronized (nodesSourceExceptions){
            nodesSourceExceptions.putAll(exceptions);
        }
        return list;

    }

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    @Override
    public ArrayList<Throwable> getResourceModelSourceExceptions() {
        synchronized (nodesSourceExceptions) {
            return new ArrayList<>(nodesSourceExceptions.values());
        }
    }
    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    @Override
    public Map<String,Throwable> getResourceModelSourceExceptionsMap() {
        synchronized (nodesSourceExceptions) {
            return Collections.unmodifiableMap(nodesSourceExceptions);
        }
    }

    public List<ReadableProjectNodes> getResourceModelSources() {
        List<LoadedResourceModelSource> resourceModelSources = getResourceModelSourcesInternal();
        return
            resourceModelSources.stream()
                                .map(i -> (ReadableProjectNodes) i)
                                .collect(Collectors.toList());
    }

    private synchronized List<LoadedResourceModelSource> getResourceModelSourcesInternal() {
        //determine if sources need to be reloaded
        final long lastMod = projectConfig.getConfigLastModifiedTime()!=null? projectConfig.getConfigLastModifiedTime().getTime():0;
        if (lastMod > nodesSourcesLastReload) {
            unloadSources();
        }
        if (!sourcesOpened) {
            loadResourceModelSources();
            sourcesOpened = true;
        }
        return nodesSourceList;
    }

    @Data @RequiredArgsConstructor
    public static final class ProjectWriteableNodes implements WriteableProjectNodes {
        final WriteableModelSource writeableSource;
        final int index;
        final String type;
    }

    public Collection<WriteableProjectNodes> getWriteableResourceModelSources() {
        //determine if sources need to be reloaded
        List<LoadedResourceModelSource> resourceModelSources = getResourceModelSourcesInternal();
        return
                resourceModelSources.stream()
                                    .filter(i -> i.getSourceType() == SourceType.READ_WRITE)
                                    .map(i -> new ProjectWriteableNodes(i.getWriteable(), i.getIndex(), i.getType()))
                                    .collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        unloadSources();
    }

    /**
     * Clear the sources list and close all plugin loader references
     */
    private synchronized void unloadSources() {
        nodesSourceList = new ArrayList<>();
        Closeables.closeQuietly(nodeSourceReferences);
        nodeSourceReferences = new HashSet<>();
        sourcesOpened = false;
    }

    private void loadResourceModelSources() {
        Map<String,Exception> exceptions = Collections.synchronizedMap(new HashMap<>());
        Set<String> validSources = new HashSet<>();
        //generate Configuration for file source
        if (projectConfig.hasProperty(PROJECT_RESOURCES_FILE_PROPERTY)) {
            logger.error("Project config: " + PROJECT_RESOURCES_FILE_PROPERTY + " is no longer supported.");
        }
        if (projectConfig.hasProperty(PROJECT_RESOURCES_URL_PROPERTY)) {
            logger.error("Project config: " + PROJECT_RESOURCES_URL_PROPERTY + " is no longer supported.");
        }
        String name = projectConfig.getName();

        final List<Map<String, Object>> list = listResourceModelConfigurations();
        int i = 1;
        for (final Map<String, Object> map : list) {
            final String providerType = (String) map.get("type");
            final Properties props = (Properties) map.get("props");
            final Properties extraProps = (Properties) map.get("extraProps");

            logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + props);
            try {
                nodesSourceList.add(
                        loadResourceModelSource(
                                providerType, props,extraProps, shouldCacheForType(providerType),
                                i + ".source",
                                i
                        )
                );
                validSources.add(i + ".source" );
            } catch (ExecutionServiceException e) {
                logger.error(String.format(
                        "Failed loading resource model source #%d in project %s, skipping: %s",
                        i,
                        name,
                        e.getMessage()
                ), e);
                exceptions.put(i + ".source" ,e);
            }
            i++;
        }
        synchronized (nodesSourceExceptions) {
            nodesSourceExceptions.putAll(exceptions);
            for (String validSource : validSources) {
                nodesSourceExceptions.remove(validSource);
            }
        }

        Date configLastModifiedTime = projectConfig.getConfigLastModifiedTime();
        nodesSourcesLastReload = configLastModifiedTime!=null?configLastModifiedTime.getTime():-1;
    }


    private File getResourceModelSourceFileCacheForType(String ident) {
        String varDir = projectConfig.getProperty("framework.var.dir");
        File file = new File(varDir, "resourceModelSourceCache/" + projectConfig.getName() + "/" + ident + ".xml");
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            logger.warn("Failed to create cache dirs for source file cache");
        }
        return file;
    }

    class StoreExceptionHandler implements ExceptionCatchingResourceModelSource.ExceptionHandler {
        String sourceIdent;

        StoreExceptionHandler(final String sourceIdent) {
            this.sourceIdent = sourceIdent;
        }

        @Override
        public void handleException(final Throwable t, final ResourceModelSource origin) {
            nodesSourceExceptions.put(sourceIdent, t);
        }
    }

    /**
     * @param nodes IProjectNodes
     *
     * @return model source view of the nodes
     */
    public static ResourceModelSource asModelSource(IProjectNodes nodes) {
        return new ProjectNodesSource(nodes);
    }

    /**
     * implements ResourceModelSource by wrapping IProjectNodes
     */
    private static class ProjectNodesSource implements ResourceModelSource {
        IProjectNodes nodes;

        ProjectNodesSource(final IProjectNodes nodes) {
            this.nodes = nodes;
        }

        @Override
        public INodeSet getNodes() {
            return nodes.getNodeSet();
        }
    }

    /**
     * @param origin origin source
     * @param ident  unique identity for this cached source, used in filename
     * @param descr  description of the source, used in logging
     *
     * @return new source
     */
    private ResourceModelSource createCachingSource(
            ResourceModelSource origin,
            String ident,
            String descr
    )
    {
        return createCachingSource(origin, ident, descr, SourceFactory.CacheType.BOTH, true);
    }

    /**
     * @param logging
     * @param origin origin source
     * @param ident  unique identity for this cached source, used in filename
     * @param descr  description of the source, used in logging
     * @param logging if true, log cache access
     *
     * @return new source
     */
    public ResourceModelSource createCachingSource(
            ResourceModelSource origin,
            String ident,
            String descr,
            SourceFactory.CacheType type,
            final boolean logging
    )
    {
        final File file = getResourceModelSourceFileCacheForType(ident);
        final ResourceModelSourceService nodesSourceService = resourceModelSourceService;
        final ResourceFormatGeneratorService resourceFormatGeneratorService = getResourceFormatGeneratorService();
        final Properties fileSourceConfig = generateFileSourceConfigurationProperties(
                file.getAbsolutePath(),
                ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE, false, false
        );
        try {
            ResourceModelSource fileSource = nodesSourceService.getSourceForConfiguration("file", fileSourceConfig);

            ResourceFormatGenerator generatorForFormat = resourceFormatGeneratorService.getGeneratorForFormat
                    (ResourceXMLFormatGenerator.SERVICE_PROVIDER_TYPE);

            String ident1 = "[ResourceModelSource: " + descr + ", project: " + projectConfig.getName() + "]";
            StoreExceptionHandler handler = new StoreExceptionHandler(ident);
            ResourceModelSourceCache cache = new FileResourceModelSourceCache(
                    file,
                    generatorForFormat,
                    fileSource
            );
            if(logging) {
                cache = new LoggingResourceModelSourceCache(cache, ident1);
            }
            return SourceFactory.cachedSource(
                    origin,
                    ident1,
                    handler,
                    cache,
                    type
            );
        } catch (UnsupportedFormatException | ExecutionServiceException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return resourceFormatGeneratorService;
    }


    interface LoadedResourceModelSource extends ResourceModelSource, ReadableProjectNodes {
        int getIndex();

        String getType();
    }

    static class LoadedSource
            extends DelegateResourceModelSource implements LoadedResourceModelSource
    {
        @Getter final int index;
        @Getter final String type;

        LoadedSource(final int index, final String type, final ResourceModelSource delegate) {
            super(delegate);
            this.index = index;
            this.type = type;
        }

        @Override
        public ResourceModelSource getSource() {
            return getDelegate();
        }
    }

    private LoadedResourceModelSource loadResourceModelSource(
            String type,
            Properties configuration,
            Properties extraConfiguration,
            boolean useCache,
            String ident,
            int index
    ) throws ExecutionServiceException
    {

        configuration.put("project", projectConfig.getName());

        CloseableProvider<ResourceModelSource> sourceForConfiguration ;

        if (null == nodeSourceLoader) {
            sourceForConfiguration = resourceModelSourceService.getCloseableSourceForConfiguration(type, configuration);
        } else {
            try {
                NodeSourceLoaderConfig nodeSourceLoaderConfig = nodeSourceLoader.getSourceForConfiguration(projectConfig.getName(),
                                                                             new SourceDefinitionImpl(type,configuration,extraConfiguration,ident,index)
                );
                sourceForConfiguration = nodeSourceLoaderConfig.getCloseableProvider();
            } catch (Throwable e) {
                throw new ExecutionServiceException(e, "Could not create node source: " + e.getMessage());
            }
            if (sourceForConfiguration == null) {
                throw new ExecutionServiceException("Could not create node source: not found");
            }
        }

        nodeSourceReferences.add(sourceForConfiguration);
        if (useCache) {
            return new LoadedSource(
                    index,
                    type,
                    createCachingSource(sourceForConfiguration.getProvider(), ident, ident + " (" + type + ")")
            );
        } else {

            return new LoadedSource(
                    index,
                    type, sourceForConfiguration.getProvider()
            );
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
        build.project(projectConfig.getName());
        build.generateFileAutomatically(generate);
        build.includeServerNode(includeServerNode);

        return build.getProperties();
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
        Map propertiesMap = projectConfig.getProperties();
        Properties properties = new Properties();
        properties.putAll(propertiesMap);
        return listResourceModelConfigurations(properties);
    }

    /**
     * list the configurations of resource model providers.
     *
     * @return a list of PluginConfiguration
     */
    @Override
    public synchronized List<ExtPluginConfiguration> listResourceModelPluginConfigurations() {
        return listPluginConfigurations(
                projectConfig.getProjectProperties(),
                RESOURCES_SOURCE_PROP_PREFIX,
                ServiceNameConstants.ResourceModelSource
        );
    }

    /**
     * list the configurations of node enhancer providers.
     *
     * @return a list of PluginConfiguration
     */
    @Override
    public synchronized List<ExtPluginConfiguration> listNodeEnhancerConfigurations() {
        return listPluginConfigurations(
                projectConfig.getProjectProperties(),
                NODE_ENHANCER_PROP_PREFIX,
                ServiceNameConstants.NodeEnhancer
        );
    }


    /**
     * @return Properties form for the serialized list of model source configurations
     */
    public static Properties serializeResourceModelConfigurations(final List<Map<String, Object>> configs) {
        Properties projProps = new Properties();
        int count = 1;
        for (Map<String, Object> config : configs) {

            String prefix = FrameworkProject.RESOURCES_SOURCE_PROP_PREFIX + "." + count + ".";
            String type = config.get("type").toString();
            Properties props = (Properties) config.get("props");
            projProps.setProperty(prefix + "type", type);
            for (String k : props.stringPropertyNames()) {
                String v = props.getProperty(k);
                projProps.setProperty(prefix + "config." + k, v);
            }
            count++;
        }
        return projProps;
    }

    /**
     * @return Properties form for the serialized list of indexed plugin configurations with a prefix
     */
    public static Properties serializePluginConfigurations(String prefix, final List<PluginConfiguration> configs) {
        Properties projProps = new Properties();
        int count = 1;
        for (PluginConfiguration config : configs) {

            serializeProp(prefix, projProps, count, config);
            count++;
        }
        return projProps;
    }

    public static void serializeProp(
            final String prefix,
            final Properties projProps,
            final int count,
            final PluginConfiguration config
    )
    {
        String propPrefix = prefix + "." + count + ".";
        projProps.setProperty(propPrefix + "type", config.getProvider());
        Map<String, Object> configuration = config.getConfiguration();
        for (String k : configuration.keySet()) {
            Object v = configuration.get(k);
            projProps.setProperty(propPrefix + "config." + k, v.toString());
        }
    }

    /**
     * @param extra if true, include extra data
     * @return Properties form for the serialized list of indexed plugin configurations with a prefix
     */
    public static Properties serializePluginConfigurations(
            String prefix,
            final List<ExtPluginConfiguration> configs,
            boolean extra
    )
    {
        Properties projProps = new Properties();
        int count = 1;
        for (ExtPluginConfiguration config : configs) {
            String propPrefix = prefix + "." + count + ".";
            serializeProp(prefix, projProps, count, config);
            if (extra && config.getExtra() != null && config.getExtra().size() > 0) {
                Properties extraProperties = generateExtraProperties(propPrefix, config.getExtra());
                extraProperties.forEach((key, value) -> {
                    if(!projProps.containsKey(key)){
                        projProps.put(key, value);
                    }
                });
            }
            count++;
        }
        return projProps;
    }

    public static Properties generateExtraProperties(String propPrefix , Map<String, Object> extra){
        Properties extraProps = new Properties();

        for (String s : extra.keySet()) {
            if(extra.get(s) instanceof Map){
                Properties subprops = generateExtraProperties(propPrefix + s +".", (Map)extra.get(s));
                extraProps.putAll(subprops);
            }else{
                extraProps.setProperty(propPrefix + s, extra.get(s).toString());
            }
        }
        return extraProps;
    }

    /**
     * Return a list of resource model configuration
     * @param props properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public static List<Map<String, Object>> listResourceModelConfigurations(final Properties props) {
        final ArrayList<Map<String, Object>> list = new ArrayList<>();
        int i = 1;
        boolean done = false;
        while (!done) {
            final String prefix = RESOURCES_SOURCE_PROP_PREFIX + "." + i;
            if (props.containsKey(prefix + ".type")) {
                final String providerType = props.getProperty(prefix + ".type");
                final Properties configProps = new Properties();
                final Properties extraConfigProps = new Properties();
                final int len = (prefix + ".config.").length();
                for (final Object o : props.keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix + ".config.")) {
                        configProps.setProperty(key.substring(len), props.getProperty(key));
                    }
                }
                final int extraLen = (prefix+".").length();
                for (final Object o : props.keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix) && !key.startsWith(prefix + ".config.") && !key.startsWith(prefix + ".type")) {
                        extraConfigProps.setProperty(key.substring(extraLen), props.getProperty(key));
                    }
                }
                final HashMap<String, Object> map = new HashMap<>();
                map.put("type", providerType);
                map.put("props", configProps);
                map.put("extraProps", extraConfigProps);
                list.add(map);
            } else {
                done = true;
            }
            i++;
        }
        return list;
    }

    /**
     * Return a list of resource model configuration
     *
     * @param serviceName
     * @param keyprefix prefix for properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public List<ExtPluginConfiguration> listPluginConfigurations(final String keyprefix, final String serviceName)
    {
        return listPluginConfigurations(keyprefix, serviceName, true);
    }

    /**
     * Return a list of resource model configuration
     *
     * @param serviceName
     * @param keyprefix   prefix for properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public List<ExtPluginConfiguration> listPluginConfigurations(
            final String keyprefix,
            final String serviceName,
            boolean extra
    )
    {
        return listPluginConfigurations(projectConfig.getProjectProperties(), keyprefix, serviceName, extra);
    }

    /**
     * Return a list of resource model configuration
     *
     * @param serviceName service name
     * @param props       properties
     * @param keyprefix   prefix for properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public static List<ExtPluginConfiguration> listPluginConfigurations(
            final Map<String, String> props,
            final String keyprefix,
            final String serviceName
    )
    {
        return listPluginConfigurations(props, keyprefix, serviceName, false);

    }

    /**
     * Return a list of resource model configuration
     *
     * @param serviceName service name
     * @param props       properties
     * @param keyprefix   prefix for properties
     * @return List of Maps, each map containing "type": String, "props":Properties
     */
    public static List<ExtPluginConfiguration> listPluginConfigurations(
            final Map<String, String> props,
            final String keyprefix,
            final String serviceName,
            final boolean extra
    )
    {
        final ArrayList<ExtPluginConfiguration> list = new ArrayList<>();
        int i = 1;
        boolean done = false;
        while (!done) {
            final String prefix = keyprefix + "." + i;
            if (props.containsKey(prefix + ".type")) {
                final String providerType = props.get(prefix + ".type");
                final Map<String, Object> configProps = new HashMap<>();
                final Map<String, Object> extraData = extra ? new HashMap<>() : null;
                final int len = (prefix + ".config.").length();
                for (final Object o : props.keySet()) {
                    final String key = (String) o;
                    if (key.startsWith(prefix + ".config.")) {
                        configProps.put(key.substring(len), props.get(key));
                    }
                }
                if (extra) {
                    Map<String, Object> extraConfig = new HashMap<>();
                    //list all extra props
                    for (String s : props.keySet()) {
                        if (s.startsWith(prefix + ".")) {
                            String suffix = s.substring(prefix.length() + 1);
                            if (!"type".equalsIgnoreCase(suffix) && !suffix.startsWith("config.")) {
                                extraConfig.put(suffix, props.get(s));
                            }
                        }
                    }

                    Map<String, Object> extraMap = createMapExtraProperties(extraConfig );
                    if(extraMap.size()>0){
                        extraData.putAll(extraMap);
                    }
                }
                list.add(new SimplePluginConfiguration(serviceName, providerType, configProps, extraData));
            } else {
                done = true;
            }
            i++;
        }
        return list;
    }

    public static Map<String, Object> createMapExtraProperties(final Map<String, Object> extraProps){
        Map<String, Object> extraMap = new HashMap<>();

        for (String key : extraProps.keySet()) {
            String[] paths = key.split("\\.");
            Map<String, Object> nestedMap = new HashMap<>();

            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];
                if (i < paths.length-1) {
                    Map<String, Object> newMap = new HashMap<>();
                    if(i==0){
                        extraMap.putIfAbsent(path, newMap);
                        nestedMap = (Map)extraMap.get(path);
                    }else{
                        nestedMap.putIfAbsent(path, newMap);
                        nestedMap = (Map)nestedMap.get(path);
                    }

                } else {
                    if(i==0){
                        extraMap.putIfAbsent(path, extraProps.get(key));
                    }else{
                        nestedMap.put(path, extraProps.get(key));
                    }

                }
            }
        }
        return extraMap;
    }


    public IRundeckProjectConfig getProjectConfig() {
        return projectConfig;
    }
}
