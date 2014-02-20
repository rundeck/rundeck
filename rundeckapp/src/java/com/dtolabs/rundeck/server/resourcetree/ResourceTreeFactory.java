package com.dtolabs.rundeck.server.resourcetree;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.resourcetree.ResourceConverterPluginAdapter;
import com.dtolabs.rundeck.core.resourcetree.ResourceMeta;
import com.dtolabs.rundeck.core.resourcetree.ResourceTree;
import com.dtolabs.rundeck.core.resourcetree.ResourceUtil;
import com.dtolabs.rundeck.plugins.resourcetree.ResourceConverterPlugin;
import com.dtolabs.rundeck.plugins.resourcetree.ResourceStoragePlugin;
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin;
import com.dtolabs.rundeck.server.plugins.PluginRegistry;
import com.dtolabs.rundeck.server.plugins.services.ResourceConverterPluginProviderService;
import com.dtolabs.rundeck.server.plugins.services.ResourceStoragePluginProviderService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import us.vario.greg.lct.conf.TreeBuilder;
import us.vario.greg.lct.model.PathUtil;
import us.vario.greg.lct.model.ResourceSelector;
import us.vario.greg.lct.model.Tree;

import java.util.HashMap;
import java.util.Map;

/**
 * ResourceTreeFactory constructs Rundeck's ResourceTree based on the configuration in the framework.properties
 *
 * @author greg
 * @since 2/19/14 3:24 PM
 */
public class ResourceTreeFactory implements FactoryBean<ResourceTree>, InitializingBean {
    public static final String TYPE = "type";
    public static final String PATH = "path";
    public static final String CONFIG = "config";
    public static final String SEP = ".";
    public static final String REMOVE_PATH_PREFIX = "removePathPrefix";
    public static final String RUNDECK_RESOURCE_STORAGE = "rundeck.resource.storage";
    public static final String RUNDECK_RESOURCE_CONVERTER = "rundeck.resource.converter";
    public static final String DEFAULT_PLUGIN_TYPE = "file";
    public static final String RESOURCE_SELECTOR = "resourceSelector";
    Framework rundeckFramework;
    private PluginRegistry pluginRegistry;
//    private ConfigObject serverConfig;
    private String storageConfigPrefix = RUNDECK_RESOURCE_STORAGE;
    private String converterConfigPrefix = RUNDECK_RESOURCE_CONVERTER;
    private String defaultPluginType = DEFAULT_PLUGIN_TYPE;
    private Map<String,String> defaultPluginConfig = new HashMap<String, String>();

    private Tree<ResourceMeta> constructedTree;
    private ResourceStoragePluginProviderService resourceStoragePluginProviderService;
    private ResourceConverterPluginProviderService resourceConverterPluginProviderService;

    //injected
    public void setRundeckFramework(Framework framework) {
        this.rundeckFramework = framework;
    }


    @Override
    public ResourceTree getObject() throws Exception {
        if (null == rundeckFramework) {
            throw new FactoryBeanNotInitializedException("'rundeckFramework' is required");
        }
        if (null == pluginRegistry) {
            throw new FactoryBeanNotInitializedException("'pluginRegistry' is required");
        }
        return ResourceUtil.asResourceTree(buildTree());
    }

    @Override
    public Class<?> getObjectType() {
        return ResourceTree.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(getDefaultPluginConfig().size()<1){
            //set defaults

        }
    }

    private Tree<ResourceMeta> buildTree() {
        //configure the tree
        TreeBuilder<ResourceMeta> builder = TreeBuilder.builder();
        int storeIndex = 1;
        boolean seen=false;
        while (rundeckFramework.hasProperty(getStorageConfigPrefix() + SEP + storeIndex + TYPE)) {
            configureStoragePlugin(builder, storeIndex);
            seen=true;
            storeIndex++;
        }
        if (!seen) {
            //load default resource storage
            builder.base(loadDefaultStoragePlugin());
        }
        int converterIndex = 1;
        while (rundeckFramework.hasProperty(getConverterConfigPrefix() + SEP + converterIndex + TYPE)) {
            builder=configureConverterPlugin(builder, converterIndex);
            converterIndex++;
        }
        return builder.build();
    }

    /**
     * Configure converter plugins for the builder
     * @param builder builder
     * @param index given index
     * @return builder
     */
    private TreeBuilder<ResourceMeta> configureConverterPlugin(TreeBuilder<ResourceMeta> builder, int index) {
        String pref1 = getConverterConfigPrefix() + SEP + index;
        String pluginType = rundeckFramework.getProperty(pref1 + SEP + TYPE);
        String pathProp = pref1 + SEP + PATH;
        String selectorProp = pref1 + SEP + RESOURCE_SELECTOR;
        String path = rundeckFramework.getProperty(pathProp);
        String selector = rundeckFramework.getProperty(selectorProp);
        if(null==path && null==selector) {
            throw new IllegalArgumentException("Converter plugin [" + index + "] specified by " + (pref1) + " MUST " +
                    "define one of: " +
                    pathProp + " OR " + selectorProp);
        }

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, rundeckFramework.getPropertyLookup().getPropertiesMap());

        ResourceConverterPlugin converterPlugin = loadPlugin(pluginType, config, resourceConverterPluginProviderService);
        //convert tree under the subpath if specified, AND matching the selector if specified
        builder.convert(new ResourceConverterPluginAdapter(converterPlugin),
                null != path ? PathUtil.asPath(path.trim()) : null,
                createResourceMetaSelector(selector));
        return builder;
    }

    /**
     * Selector syntax:<br/>
     * <pre>
     * key OP value [; key OP value]*
     * </pre>
     * OP can be "=" or "=~" to indicate a regular expression match.
     * @param selector the selector syntax string to parse
     * @return a resource selector corresponding to the parsed selector string
     */
    private ResourceSelector<ResourceMeta> createResourceMetaSelector(String selector) {
        if(null==selector){
            return null;
        }
        String[] split = selector.split(";");
        Map<String, String> values = new HashMap<String, String>();
        Map<String, String> regexes = new HashMap<String, String>();
        for (int i = 0; i < split.length; i++) {
            String s = split[i].trim();
            String[] split1 = s.split("=", 2);
            if(split1.length==2) {
                String key = split1[0].trim();
                String value = split1[1].trim();
                if (value.startsWith("~")) {
                    //regex
                    regexes.put(key, value.substring(1));
                }else {
                    values.put(key, value);
                }
            }
        }
        ResourceSelector<ResourceMeta> equalsSelector=null;
        ResourceSelector<ResourceMeta> regexSelector=null;

        if(values.size()>0) {
            equalsSelector = PathUtil.exactMetadataResourceSelector(values, true);
        }
        if(regexes.size()>0) {
            regexSelector  = PathUtil.regexMetadataResourceSelector(regexes, true);
        }
        if(null==equalsSelector){
            return regexSelector;
        }
        if(null==regexSelector){
            return equalsSelector;
        }
        return PathUtil.composeSelector(equalsSelector, regexSelector, true);
    }


    /**
     * Extract a map of the property values starting with the given prefix
     * @return map
     * @param configPrefix prefix
     * @param propertiesMap input
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
     * @param builder
     * @param index
     */
    private void configureStoragePlugin(TreeBuilder<ResourceMeta> builder, int index) {
        String pref1 = getStorageConfigPrefix() + SEP + index;
        String pluginType = rundeckFramework.getProperty(pref1 + SEP + TYPE);
        String path = rundeckFramework.getProperty(pref1 + SEP + PATH);
        boolean removePathPrefix = Boolean.parseBoolean(rundeckFramework.getProperty(pref1 + SEP +
                REMOVE_PATH_PREFIX));

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, rundeckFramework.getPropertyLookup().getPropertiesMap());

        Tree<ResourceMeta> resourceMetaTree = loadPlugin(pluginType, config, resourceStoragePluginProviderService);
        if (index == 1 && "/".equals(path.trim())) {
            builder.base(resourceMetaTree);
        } else {
            builder.subTree(PathUtil.asPath(path.trim()), resourceMetaTree, !removePathPrefix);
        }
    }

    private ResourceStoragePlugin loadDefaultStoragePlugin() {
        return loadPlugin(getDefaultPluginType(), getDefaultPluginConfig(), resourceStoragePluginProviderService);
    }

    private <T> T loadPlugin(String pluginType, Map<String, String> config,
            PluggableProviderService<T> resourceStoragePluginProviderService1) {
        ConfiguredPlugin<T> configured = getPluginRegistry().configurePluginByName(pluginType,
                resourceStoragePluginProviderService1, config);
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

    public ResourceConverterPluginProviderService getResourceConverterPluginProviderService() {
        return resourceConverterPluginProviderService;
    }

    public void setResourceConverterPluginProviderService(ResourceConverterPluginProviderService
            resourceConverterPluginProviderService) {
        this.resourceConverterPluginProviderService = resourceConverterPluginProviderService;
    }

    public ResourceStoragePluginProviderService getResourceStoragePluginProviderService() {
        return resourceStoragePluginProviderService;
    }

    public void setResourceStoragePluginProviderService(ResourceStoragePluginProviderService
            resourceStoragePluginProviderService) {
        this.resourceStoragePluginProviderService = resourceStoragePluginProviderService;
    }

    public String getDefaultPluginType() {
        return defaultPluginType;
    }

    public void setDefaultPluginType(String defaultPluginType) {
        this.defaultPluginType = defaultPluginType;
    }

    public Map<String, String> getDefaultPluginConfig() {
        return defaultPluginConfig;
    }

    public void setDefaultPluginConfig(Map<String, String> defaultPluginConfig) {
        this.defaultPluginConfig = defaultPluginConfig;
    }
}
