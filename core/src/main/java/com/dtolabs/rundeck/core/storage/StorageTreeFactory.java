/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyUtil;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.rundeck.plugins.storage.StoragePlugin;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Tree;
import org.rundeck.storage.conf.TreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StorageTreeFactory {
    public static final String ORG_RUNDECK_STORAGE_EVENTS_LOGGER_NAME = "org.rundeck.storage.events";
    public static final String LOGGER_NAME                            = "loggerName";
    static              Logger logger                                 = LoggerFactory.getLogger(StorageTreeFactory.class);
    public static final String TYPE                                   = "type";
    public static final String PATH                                   = "path";
    public static final String CONFIG                                 = "config";
    public static final String SEP                                    = ".";
    public static final String REMOVE_PATH_PREFIX                     = "removePathPrefix";
    public static final String              RESOURCE_SELECTOR                      = "resourceSelector";
    private             IPropertyLookup     frameworkPropertyLookup;
    private             PluginRegistry      pluginRegistry;
    private             String              storageConfigPrefix;
    private             String              converterConfigPrefix;
    private             String              baseStorageType;
    private             String              loggerName                             =ORG_RUNDECK_STORAGE_EVENTS_LOGGER_NAME;
    private             Map<String, String> baseStorageConfig                      = new HashMap<String, String>();
    private             Map<String, String> configuration                          = new HashMap<String, String>();
    private             Set<String>         defaultConverters                      = new HashSet<String>();

    private PluggableProviderService<StoragePlugin> storagePluginProviderService;
    private PluggableProviderService<StorageConverterPlugin> storageConverterPluginProviderService;


    public StorageTree createTree() throws Exception {
        if ( null == frameworkPropertyLookup) {
            throw new StorageTreeInitializationException("'frameworkPropertyLookup' is required");
        }
        if (null == pluginRegistry) {
            throw new StorageTreeInitializationException("'pluginRegistry' is required");
        }
        if (null == storagePluginProviderService) {
            throw new StorageTreeInitializationException("'storagePluginProviderService' is required");
        }
        if (null == storageConverterPluginProviderService) {
            throw new StorageTreeInitializationException("'storageConverterPluginProviderService' is required");
        }
        if (null == storageConfigPrefix) {
            throw new StorageTreeInitializationException("'storageConfigPrefix' is required");
        }
        if (null == converterConfigPrefix) {
            throw new StorageTreeInitializationException("'converterConfigPrefix' is required");
        }
        if (null == baseStorageType) {
            throw new StorageTreeInitializationException("'baseStorageType' is required");
        }
        return StorageUtil.asStorageTree(buildTree(configuration));
    }

    /**
     * Create a tree mapped to a subtree of the given storage tree
     * @param storageTree storage tree
     * @param subpath subpath
     * @return StorageTree view onto the subpath
     */
    public static StorageTree subTree(StorageTree storageTree, String subpath){
        return StorageUtil.asStorageTree(TreeBuilder.subPathTree(storageTree, PathUtil.asPath(subpath)));
    }

    private Tree<ResourceMeta> buildTree(Map<String,String> config) {
        if(null==config) {
            config = new HashMap<String, String>();
        }
        //base layer of storage
        TreeBuilder<ResourceMeta> builder = baseStorage(TreeBuilder.<ResourceMeta>builder());

        int storeIndex = 1;

        while (config.containsKey(getStorageConfigPrefix() + SEP + storeIndex + SEP + TYPE)) {
            configureStoragePlugin(builder, storeIndex, config);
            storeIndex++;
        }
        if (1 == storeIndex) {
            logger.debug("No storage plugins configured with prefix " + getStorageConfigPrefix());
        }
        builder = addLogger(builder,config);
        //apply default converters on top of storage
        builder = baseConverter(builder);

        //add plugin converters
        int converterIndex = 1;
        while (config.containsKey(getConverterConfigPrefix() + SEP + converterIndex + SEP + TYPE)) {
            builder = configureConverterPlugin(builder, converterIndex, config);
            converterIndex++;
        }
        if (1 == converterIndex) {
            logger.debug("No converter plugins configured with prefix " + getConverterConfigPrefix());
        }
        return builder.build();
    }

    /**
     * Apply base converters for metadata timestamps
     * @param builder builder
     * @return builder
     */
    private TreeBuilder<ResourceMeta> baseConverter(TreeBuilder<ResourceMeta> builder) {
        if(null!=defaultConverters && defaultConverters.contains("StorageTimestamperConverter")) {
            logger.debug("Configuring base converter: StorageTimestamperConverter" );
            builder=builder.convert(
                    new StorageConverterPluginAdapter(
                            "builtin:timestamp",
                            new StorageTimestamperConverter()
                    )
            );
        }
        if(null!=defaultConverters && defaultConverters.contains("KeyStorageLayer")){
            logger.debug("Configuring base converter: KeyStorageLayer" );
            builder=builder.convert(
                    new StorageConverterPluginAdapter(
                            "builtin:ssh-storage",
                            new KeyStorageLayer()
                    ), PathUtil.asPath("/keys")
            );
        }
        return builder;
    }

    /**
     * Append final listeners to the tree
     *
     * @param builder builder
     *
     * @return builder
     */
    private TreeBuilder<ResourceMeta> addLogger(TreeBuilder<ResourceMeta> builder, Map<String,String> config) {
        String loggerName= getLoggerName();
        if (null != config.get(LOGGER_NAME)) {
            loggerName = config.get(LOGGER_NAME);
        }
        if (null == loggerName) {
            loggerName = ORG_RUNDECK_STORAGE_EVENTS_LOGGER_NAME;
        }
        logger.debug("Add log4j logger for storage with name: " + loggerName);
        return builder.listen(new StorageLogger(loggerName));
    }

    /**
     * Set up the base storage layer for the tree
     *
     * @param builder builder
     *
     * @return builder
     */
    private TreeBuilder<ResourceMeta> baseStorage(TreeBuilder<ResourceMeta> builder) {
        //set base using file storage, could be overridden
        Map<String, String> config1 = expandConfig(getBaseStorageConfig());
        logger.debug("Default base storage provider: " + getBaseStorageType() + ", " +
                     "config: " + config1);

        StoragePlugin base = loadPlugin(
                getBaseStorageType(),
                config1,
                storagePluginProviderService
        );
        if(null==base) {
            throw new IllegalArgumentException("Plugin could not be loaded: " + getBaseStorageType());
        }
        return builder.base(base);
    }

    private Map<String, String> stringStringMap(Map map) {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        for (Object o : map.keySet()) {
            stringStringHashMap.put(o.toString(), map.get(o).toString());
        }
        return stringStringHashMap;
    }

    /**
     * Configure converter plugins for the builder
     *
     * @param builder     builder
     * @param index       given index
     * @param configProps configuration properties
     *
     * @return builder
     */
    private TreeBuilder<ResourceMeta> configureConverterPlugin(TreeBuilder<ResourceMeta> builder, int index,
                                                               Map<String, String> configProps) {
        String pref1 = getConverterConfigPrefix() + SEP + index;
        String pluginType = configProps.get(pref1 + SEP + TYPE);
        String pathProp = pref1 + SEP + PATH;
        String selectorProp = pref1 + SEP + RESOURCE_SELECTOR;
        String path = configProps.get(pathProp);
        String selector = configProps.get(selectorProp);
        if (null == path && null == selector) {
            throw new IllegalArgumentException("Converter plugin [" + index + "] specified by " + (pref1) + " MUST " +
                                               "define one of: " +
                                               pathProp + " OR " + selectorProp);
        }

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, configProps);
        config = expandConfig(config);
        logger.debug("Add Converter[" + index + "]:"
                     + (null != path ? path : "/")
                     + "[" + (null != selector ? selector : "*") + "]"
                     + " " + pluginType + ", config: " + config);


        return buildConverterPlugin(builder, pluginType, path, selector, config);
    }

    /**
     * Append a converter plugin to the tree builder
     *
     * @param builder    builder
     * @param pluginType converter plugin type
     * @param path       path
     * @param selector   metadata selector
     * @param config     plugin config data
     *
     * @return builder
     */
    private TreeBuilder<ResourceMeta> buildConverterPlugin(TreeBuilder<ResourceMeta> builder, String pluginType,
                                                           String path, String selector, Map<String, String> config) {
        StorageConverterPlugin converterPlugin = loadPlugin(
                pluginType,
                config,
                storageConverterPluginProviderService
        );
        //convert tree under the subpath if specified, AND matching the selector if specified
        return builder.convert(
                new StorageConverterPluginAdapter(pluginType,converterPlugin),
                null != path ? PathUtil.asPath(path.trim()) : null,
                null != selector ? PathUtil.<ResourceMeta>resourceSelector(selector) : null
        );
    }


    /**
     * Extract a map of the property values starting with the given prefix
     *
     * @param configPrefix  prefix
     * @param propertiesMap input
     *
     * @return map
     */
    private Map<String, String> subPropertyMap(String configPrefix, Map propertiesMap) {
        Map<String, String> config = new HashMap<String, String>();
        for (Object o : propertiesMap.keySet()) {
            String key = (String) o;
            if (key.startsWith(configPrefix)) {
                String conf = key.substring(configPrefix.length());
                config.put(conf, propertiesMap.get(key).toString());
            }
        }
        return config;
    }

    /**
     * Configures storage plugins with the builder
     *
     * @param builder     builder
     * @param index       current prop index
     * @param configProps configuration properties
     */
    private void configureStoragePlugin(TreeBuilder<ResourceMeta> builder, int index, Map<String, String> configProps) {
        String pref1 = getStorageConfigPrefix() + SEP + index;
        String pluginType = configProps.get(pref1 + SEP + TYPE);
        String path = configProps.get(pref1 + SEP + PATH);
        boolean removePathPrefix = Boolean.parseBoolean(configProps.get(pref1 + SEP +
                                                                        REMOVE_PATH_PREFIX));

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, configProps);
        config = expandConfig(config);
        Tree<ResourceMeta> resourceMetaTree = loadPlugin(
                pluginType,
                config,
                storagePluginProviderService
        );
        if (index == 1 && PathUtil.isRoot(path)) {
            logger.debug("New base Storage[" + index + "]:" + path + " " + pluginType + ", config: " + config);
            builder.base(resourceMetaTree);
        } else {
            logger.debug("Subtree Storage[" + index + "]:" + path + " " + pluginType + ", config: " + config);
            builder.subTree(PathUtil.asPath(path.trim()), resourceMetaTree, !removePathPrefix);
        }
    }

    /**
     * Expand embedded framework property references in the map values
     *
     * @param map map
     *
     * @return expanded map
     */
    private Map<String, String> expandConfig(Map<String, String> map) {
        return expandAllProperties(map, getPropertyLookup().getPropertiesMap());
    }

    private IPropertyLookup getPropertyLookup() {
        return frameworkPropertyLookup;
    }

    private Map<String, String> expandAllProperties(Map<String, String> source, Map values) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String s : source.keySet()) {
            result.put(s, PropertyUtil.expand(source.get(s), values));
        }
        return result;
    }

    private <T> T loadPlugin(String pluginType, Map<String, String> config,
                             PluggableProviderService<T> service)
    {
        ConfiguredPlugin<T> configured = getPluginRegistry().retainConfigurePluginByName(
                pluginType,
                service,
                PropertyResolverFactory.createResolver(
                        PropertyResolverFactory.instanceRetriever(config),
                        null,
                        getPropertyLookup()
                ),
                PropertyScope.Instance
        );
        if (null == configured) {
            throw new IllegalArgumentException(
                    service.getName() + " Plugin named \"" + pluginType + "\" could not be" +
                    " " +
                    "loaded"
            );
        }
        return configured.getInstance();
    }

    public String getStorageConfigPrefix() {
        return storageConfigPrefix;
    }

    public void setStorageConfigPrefix(String storageConfigPrefix) {
        this.storageConfigPrefix = storageConfigPrefix;
    }

    public String getConverterConfigPrefix() {
        return converterConfigPrefix;
    }

    public void setConverterConfigPrefix(String converterConfigPrefix) {
        this.converterConfigPrefix = converterConfigPrefix;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    public void setPluginRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public PluggableProviderService getStorageConverterPluginProviderService() {
        return storageConverterPluginProviderService;
    }

    public void setStorageConverterPluginProviderService(PluggableProviderService
                                                                 storageConverterPluginProviderService) {
        this.storageConverterPluginProviderService = storageConverterPluginProviderService;
    }

    public PluggableProviderService getStoragePluginProviderService() {
        return storagePluginProviderService;
    }

    public void setStoragePluginProviderService(PluggableProviderService
                                                        storagePluginProviderService) {
        this.storagePluginProviderService = storagePluginProviderService;
    }

    public String getBaseStorageType() {
        return baseStorageType;
    }

    public void setBaseStorageType(String baseStorageType) {
        this.baseStorageType = baseStorageType;
    }

    public Map<String, String> getBaseStorageConfig() {
        return baseStorageConfig;
    }

    public void setBaseStorageConfig(Map<String, String> baseStorageConfig) {
        this.baseStorageConfig = baseStorageConfig;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public Set<String> getDefaultConverters() {
        return defaultConverters;
    }

    public void setDefaultConverters(final Set<String> defaultConverters) {
        this.defaultConverters = defaultConverters;
    }

    public IPropertyLookup getFrameworkPropertyLookup() {
        return frameworkPropertyLookup;
    }

    public void setFrameworkPropertyLookup(final IPropertyLookup frameworkPropertyLookup) {
        this.frameworkPropertyLookup = frameworkPropertyLookup;
    }
}
