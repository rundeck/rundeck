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
import com.dtolabs.rundeck.core.plugins.CloseableProvider;
import com.dtolabs.rundeck.core.plugins.Closeables;
import com.dtolabs.rundeck.core.resources.*;
import com.dtolabs.rundeck.core.resources.format.*;
import com.dtolabs.rundeck.core.utils.TextUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manage node source loading for a project
 */
public class ProjectNodeSupport implements IProjectNodes, Closeable {
    private static final Logger logger = Logger.getLogger(ProjectNodeSupport.class);
    public static final String NODES_XML = "resources.xml";
    public static final String PROJECT_RESOURCES_URL_PROPERTY = "project.resources.url";
    public static final String PROJECT_RESOURCES_FILE_PROPERTY = "project.resources.file";
    public static final String PROJECT_RESOURCES_FILEFORMAT_PROPERTY = "project.resources.file.format";
    public static final String RESOURCES_SOURCE_PROP_PREFIX = "resources.source";
    public static final String PROJECT_RESOURCES_MERGE_NODE_ATTRIBUTES = "project.resources.mergeNodeAttributes";
    public static final String PROJECT_RESOURCES_ALLOWED_URL_PREFIX = "project.resources.allowedURL.";
    public static final String FRAMEWORK_RESOURCES_ALLOWED_URL_PREFIX = "framework.resources.allowedURL.";

    private IRundeckProjectConfig projectConfig;
    private Map<String,Exception> nodesSourceExceptions;
    private long nodesSourcesLastReload = -1L;
    private List<LoadedResourceModelSource> nodesSourceList;
    /**
     * Closeables for releasing plugin loaders when sources are disposed
     */
    private Set<Closeable> nodeSourceReferences = new HashSet<>();
    private ResourceFormatGeneratorService resourceFormatGeneratorService;
    private ResourceModelSourceService resourceModelSourceService;
    private boolean sourcesOpened;

    public ProjectNodeSupport(
            final IRundeckProjectConfig projectConfig,
            final ResourceFormatGeneratorService resourceFormatGeneratorService,
            final ResourceModelSourceService resourceModelSourceService
    )
    {
        this.projectConfig = projectConfig;
        this.resourceFormatGeneratorService = resourceFormatGeneratorService;
        this.resourceModelSourceService = resourceModelSourceService;
        this.nodesSourceExceptions = Collections.synchronizedMap(new HashMap<String, Exception>());
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
        Map<String,Exception> exceptions = Collections.synchronizedMap(new HashMap<String, Exception>());
        int index=1;
        Set<String> validSources = new HashSet<>();
        for (final ResourceModelSource nodesSource : getResourceModelSourcesInternal()) {
            try {
                INodeSet nodes = nodesSource.getNodes();
                if (null == nodes) {
                    logger.warn("Empty nodes result from [" + nodesSource.toString() + "]");
                } else {
                    list.addNodeSet(nodes);
                }
                boolean hasErrors=false;
                if(nodesSource instanceof ResourceModelSourceErrors){
                    ResourceModelSourceErrors nodeerrors = (ResourceModelSourceErrors) nodesSource;
                    List<String> modelSourceErrors = nodeerrors.getModelSourceErrors();
                    if(modelSourceErrors!=null && modelSourceErrors.size()>0){
                        hasErrors=true;
                        logger.error("Some errors getting nodes from [" +
                                     nodesSource.toString() +
                                     "]: " +
                                     modelSourceErrors);
                        exceptions.put(
                                index + ".source",
                                new ResourceModelSourceException(
                                        TextUtils.join(
                                                modelSourceErrors.toArray(new String[modelSourceErrors.size()]),
                                                ';'
                                        )
                                )
                        );
                    }
                }
                if(!hasErrors) {
                    validSources.add(index + ".source");
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
            for (String validSource : validSources) {
                nodesSourceExceptions.remove(validSource);
            }
        }
        return list;

    }

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    @Override
    public ArrayList<Exception> getResourceModelSourceExceptions() {
        synchronized (nodesSourceExceptions) {
            return new ArrayList<>(nodesSourceExceptions.values());
        }
    }
    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    @Override
    public Map<String,Exception> getResourceModelSourceExceptionsMap() {
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
        Map<String,Exception> exceptions = Collections.synchronizedMap(new HashMap<String, Exception>());
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

            logger.info("Source #" + i + " (" + providerType + "): loading with properties: " + props);
            try {
                nodesSourceList.add(
                        loadResourceModelSource(
                                providerType, props, shouldCacheForType(providerType),
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

        public StoreExceptionHandler(final String sourceIdent) {
            this.sourceIdent = sourceIdent;
        }

        @Override
        public void handleException(final Exception t, final ResourceModelSource origin) {
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

        public ProjectNodesSource(final IProjectNodes nodes) {
            this.nodes = nodes;
        }

        @Override
        public INodeSet getNodes() throws ResourceModelSourceException {
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
        final ResourceModelSourceService nodesSourceService = getResourceModelSourceService();
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

    private ResourceModelSourceService getResourceModelSourceService() {
        return resourceModelSourceService;
    }

    static interface LoadedResourceModelSource extends ResourceModelSource, ReadableProjectNodes {
        int getIndex();

        String getType();
    }

    @Data
    static class LoadedSource implements LoadedResourceModelSource {
        final int index;
        final String type;
        @Delegate final ResourceModelSource source;
    }

    private LoadedResourceModelSource loadResourceModelSource(
            String type, Properties configuration, boolean useCache,
            String ident,
            int index
    ) throws ExecutionServiceException
    {

        final ResourceModelSourceService nodesSourceService =
                getResourceModelSourceService();
        configuration.put("project", projectConfig.getName());

        CloseableProvider<ResourceModelSource> sourceForConfiguration =
                nodesSourceService.getCloseableSourceForConfiguration(
                        type,
                        configuration
                );

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


    private Properties createFileSourceConfiguration() {
        String format = null;
        if (projectConfig.hasProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY)) {
            format = projectConfig.getProperty(PROJECT_RESOURCES_FILEFORMAT_PROPERTY);
        }
        return generateFileSourceConfigurationProperties(
                projectConfig.getProperty(PROJECT_RESOURCES_FILE_PROPERTY), format, true,
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
        Map propertiesMap = projectConfig.getProperties();
        Properties properties = new Properties();
        properties.putAll(propertiesMap);
        return listResourceModelConfigurations(properties);
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

    public IRundeckProjectConfig getProjectConfig() {
        return projectConfig;
    }
}
