/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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

/*
* AbstractDescribableScriptPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/10/11 2:55 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * AbstractDescribableScriptPlugin is a base ScriptPlugin provider implementation that can be used to provide a
 * describable interface for a script plugin.  The description provided by the base implementation is configured by the
 * {@link ScriptPluginProvider}'s metadata:
 * <pre>
 *     title = Title of the Plugin
 *     description = Description of the plugin
 * </pre>
 * <pre>
 *     config.X.PROPERTY_TYPE = Type name of the property, from {@link com.dtolabs.rundeck.core.plugins.configuration.Property.Type}
 *     config.X.name = Name of the property (key string)
 *     config.X.title = Title of the property
 *     config.X.description = description of the property
 *     config.X.required = true/false, if the property is required.
 *     config.X.default = default string of the property
 *     config.X.values = comma-separated values list for Select or FreeSelect properties
 *     config.X.scope = scope of the property, from {@link PropertyScope}
 * </pre>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class AbstractDescribableScriptPlugin implements Describable {
    public static final String TITLE_PROP = "title";
    public static final String DESCRIPTION_PROP = "description";
    public static final String CONFIG_PROP_PREFIX = "config";
    public static final String CONFIG_TITLE = "title";
    public static final String CONFIG_DESCRIPTION = "description";
    public static final String CONFIG_NAME = "name";
    public static final String CONFIG_TYPE = "type";
    public static final String CONFIG_REQUIRED = "required";
    public static final String CONFIG_DEFAULT = "default";
    public static final String CONFIG_VALUES = "values";
    public static final String CONFIG_SCOPE = "scope";
    public static final String CONFIG_RENDERING_OPTIONS = "renderingOptions";

    private final ScriptPluginProvider provider;
    private final Framework framework;
    Description description;

    public AbstractDescribableScriptPlugin(final ScriptPluginProvider provider, final Framework framework) {
        this.provider = provider;
        this.framework = framework;
    }

    /**
     * @return data with exported plugin details
     */
    public Map<String,String> createPluginDataContext() {
        final Map<String,String> pluginDataContext = new HashMap<String, String>();

        pluginDataContext.put("file", provider.getArchiveFile().getAbsolutePath());
        pluginDataContext.put("scriptfile", provider.getScriptFile().getAbsolutePath());
        pluginDataContext.put("base", provider.getContentsBasedir().getAbsolutePath());

        return pluginDataContext;
    }


    static private void createProperties(
            final ScriptPluginProvider provider,
            final boolean useConventionalPropertiesMapping,
            final DescriptionBuilder dbuilder)
            throws ConfigurationException
    {
        final Map<String, Object> metadata = provider.getMetadata();
        final Object config = metadata.get("config");
        if (config instanceof List) {
            final List configs = (List) config;
            for (final Object citem : configs) {
                if (citem instanceof Map) {
                    final PropertyBuilder pbuild = PropertyBuilder.builder();
                    final Map<String, Object> itemmeta = (Map<String, Object>) citem;
                    final String typestr = metaStringProp(itemmeta, CONFIG_TYPE);
                    try {
                        pbuild.type(Property.Type.valueOf(typestr));
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException("Invalid property type: " + typestr);
                    }

                    String propName = metaStringProp(itemmeta, CONFIG_NAME);
                    pbuild
                        .name(propName)
                        .title(metaStringProp(itemmeta, CONFIG_TITLE))
                        .description(metaStringProp(itemmeta, CONFIG_DESCRIPTION));

                    final Object reqValue = itemmeta.get(CONFIG_REQUIRED);
                    final boolean required;
                    if (reqValue instanceof Boolean) {
                        required = (Boolean) reqValue;
                    } else {
                        required = reqValue instanceof String && Boolean.parseBoolean((String) reqValue);
                    }

                    pbuild.required(required);


                    final Object defObj = itemmeta.get(CONFIG_DEFAULT);

                    pbuild.defaultValue(null != defObj ? defObj.toString() : null);

                    final List<String> valueList;

                    final String valuesstr = metaStringProp(itemmeta, CONFIG_VALUES);
                    if (null != valuesstr) {
                        final String[] split = null != valuesstr ? valuesstr.split(",") : null;
                        valueList = Arrays.asList(split);
                    } else {
                        Object vlist = itemmeta.get(CONFIG_VALUES);
                        if (vlist instanceof List) {
                            valueList = (List<String>) vlist;
                        } else {
                            valueList = null;
                        }
                    }
                    final List<String> values;
                    if (null != valueList) {
                        final ArrayList<String> valuesA = new ArrayList<String>();
                        for (final String s : valueList) {
                            valuesA.add(s.trim());
                        }
                        values = valuesA;
                    } else {
                        values = null;
                    }
                    pbuild.values(values);

                    final String scopeString = metaStringProp(itemmeta, CONFIG_SCOPE);
                    if(null!=scopeString) {
                        try {
                            pbuild.scope(PropertyScope.valueOf(scopeString.trim()));
                        } catch (IllegalArgumentException e) {
                            throw new ConfigurationException("Invalid property scope: " + scopeString);
                        }
                    }
                    if(useConventionalPropertiesMapping) {
                        final String projectPropertyPrefix =
                                PropertyResolverFactory.projectPropertyPrefix
                                        (
                                                PropertyResolverFactory
                                                        .pluginPropertyPrefix(
                                                                provider.getService(),
                                                                provider.getName()
                                                        )
                                        ) ;
                        dbuilder.mapping(propName, projectPropertyPrefix + propName);

                        final String frameworkPropertyPrefix =
                                PropertyResolverFactory.frameworkPropertyPrefix
                                        (
                                                PropertyResolverFactory
                                                        .pluginPropertyPrefix(
                                                                provider.getService(),
                                                                provider.getName()
                                                        )
                                        );

                        dbuilder.frameworkMapping(propName, frameworkPropertyPrefix + propName);
                    }
                    //rendering options
                    final Object renderingOpts = itemmeta.get(CONFIG_RENDERING_OPTIONS);
                    if(null != renderingOpts && renderingOpts instanceof Map){
                        Map<String,Object> renderingOptsMap=(Map<String,Object>) renderingOpts;
                        pbuild.renderingOptions(renderingOptsMap);
                    }

                    try {
                        dbuilder.property(pbuild.build());
                    } catch (IllegalStateException e) {
                        throw new ConfigurationException(e.getMessage());
                    }

                }
            }
        }
    }

    private static String metaStringProp(final Map<String, Object> metadata, final String prop) {
        return metaStringProp(metadata, prop, null);
    }
    private static String metaStringProp(final Map<String, Object> metadata, final String prop, final String defString){
        final Object titleobj = metadata.get(prop);
        return null != titleobj && titleobj instanceof String ? (String) titleobj : defString;
    }
    protected static void createDescription(final ScriptPluginProvider provider,
                                                   final boolean allowCustomProperties,
                                                   final DescriptionBuilder builder) throws ConfigurationException {
        createDescription(provider, allowCustomProperties, false, builder);
    }
    protected static void createDescription(final ScriptPluginProvider provider,
                                                   final boolean allowCustomProperties,
                                                   final boolean useConventionalPropertiesMapping,
                                                   final DescriptionBuilder builder) throws ConfigurationException {
        builder
            .name(provider.getName())
            .title(metaStringProp(provider.getMetadata(), TITLE_PROP, provider.getName() + " Script Plugin"))
            .description(metaStringProp(provider.getMetadata(), DESCRIPTION_PROP, ""));

        if(allowCustomProperties) {
            createProperties(provider, useConventionalPropertiesMapping, builder);
        }
    }

    /**
     * Map node attributes as instance configuration values based on property descriptions.
     * If a property has a rendering option key of
     * {@link StringRenderingConstants#INSTANCE_SCOPE_NODE_ATTRIBUTE_KEY}
     * then use the value of that option as the node attribute name to use.
     *
     * @param node        node
     * @param description plugin description
     *
     * @return instance config data
     */
    protected Map<String, Object> loadInstanceDataFromNodeAttributes(
            final INodeEntry node,
            final Description description
    )
    {
        HashMap<String, Object> config = new HashMap<String, Object>();

        for (Property property : description.getProperties()) {

            Map<String, Object> renderingOptions = property.getRenderingOptions();

            if (null == renderingOptions) {
                continue;
            }

            Object o = renderingOptions.get(
                    StringRenderingConstants.INSTANCE_SCOPE_NODE_ATTRIBUTE_KEY
            );

            if (null == o || !(o instanceof String)) {
                continue;
            }

            String attribute = (String) o;

            String s = node.getAttributes().get(attribute);

            if (s == null) {
                continue;
            }

            config.put(property.getName(), s);
        }
        return config;
    }

    /**
     * Loads the plugin configuration values stored in project or framework properties, also
     *
     * @param context          execution context
     * @param localDataContext current context data
     * @param description plugin description
     * @param instanceData instance data
     *
     * @param serviceName service name
     * @return context data with a new "config" entry containing the loaded plugin config
     *         properties.
     * @throws ConfigurationException configuration error
     */
    protected Map<String, Map<String, String>> loadConfigData(
            final ExecutionContext context,
            final Map<String, Object> instanceData,
            final Map<String, Map<String, String>> localDataContext,
            final Description description, String serviceName
    ) throws ConfigurationException
    {

        final PropertyResolver resolver = PropertyResolverFactory.createPluginRuntimeResolver(
                context,
                instanceData,
                serviceName,
                getProvider().getName()
        );

        final Map<String, Object> config =
                PluginAdapterUtility.mapDescribedProperties(
                        resolver,
                        description,
                        PropertyScope.Instance
                );

        //expand properties
        Map<String, Object> expanded =
                DataContextUtils.replaceDataReferences(
                        config,
                        localDataContext
                );

        Map<String, String> data = toStringStringMap(expanded);

        loadStoragePathProperties(
                data,
                context.getStorageTree(),
                description.getProperties()
        );


        return DataContextUtils.addContext("config", data, localDataContext);
    }

    /**
     * Looks for storage path properties, and loads the values into the config data.
     * @param data map of values for config properties
     * @param storageTree storage tree
     * @param pluginProperties definition of plugin properties
     */
    private void loadStoragePathProperties(
            Map<String, String> data,
            StorageTree storageTree,
            List<Property> pluginProperties
    ) throws ConfigurationException{
        //look for "storageAccessor" properties
        List<Property> properties = pluginProperties;
        for (Property property : properties) {
            String name = property.getName();
            String propValue = data.get(name);
            if (null == propValue) {
                continue;
            }
            Map<String, Object> renderingOptions = property.getRenderingOptions();
            if(renderingOptions !=null){
                Object conversion = renderingOptions.get(StringRenderingConstants.VALUE_CONVERSION_KEY);
                if (StringRenderingConstants.ValueConversion.STORAGE_PATH_AUTOMATIC_READ.equalsOrString(conversion)) {

                    //a storage path property
                    String root = null;
                    if (null != renderingOptions.get(StringRenderingConstants
                            .STORAGE_PATH_ROOT_KEY)) {
                        root = renderingOptions.get(StringRenderingConstants
                                .STORAGE_PATH_ROOT_KEY).toString();
                    }
                    String filter = null;
                    if (null != renderingOptions.get(StringRenderingConstants
                            .STORAGE_FILE_META_FILTER_KEY)) {
                        filter = renderingOptions.get(StringRenderingConstants
                                .STORAGE_FILE_META_FILTER_KEY).toString();
                    }

                    if (null != root && !PathUtil.hasRoot(propValue,root)) {
                        continue;
                    }
                    try {
                        Resource<ResourceMeta> resource = storageTree.getResource
                                (propValue);
                        ResourceMeta contents = resource.getContents();
                        //test filter
                        if (filter != null) {
                            String[] filterComponents = filter.split("=", 2);
                            if (filterComponents != null && filterComponents.length == 2) {
                                String key = filterComponents[0];
                                String test = filterComponents[1];
                                Map<String, String> meta = contents.getMeta();
                                if (meta == null || !test.equals(meta.get(key))) {
                                    continue;
                                }
                            }
                        }
                        //finally load storage contents into a string
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        contents.writeContent(byteArrayOutputStream);
                        data.put(name, new String(byteArrayOutputStream.toByteArray()));
                    } catch (StorageException e) {
                        throw new ConfigurationException("Unable to load configuration key '" +
                                name + "' value from storage path:  " + propValue, e);
                    } catch (IOException e) {
                        throw new ConfigurationException("Unable to load configuration key '" +
                                name + "' value from storage path:  " + propValue, e);
                    }
                }
            }
        }
    }

    private static Map<String, String> toStringStringMap(Map input) {
        Map<String, String> map = new HashMap<String, String>();
        for (Object o : input.keySet()) {
            map.put(o.toString(), input.get(o) != null ? input.get(o).toString() : "");
        }
        return map;
    }


    @Override
    public Description getDescription() {
        if(null==description){
            final DescriptionBuilder builder = DescriptionBuilder.builder();
            try {
                createDescription(provider, isAllowCustomProperties(), isUseConventionalPropertiesMapping(), builder);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
            description = builder.build();
        }
        return description;
    }

    /**
     * @return true if the script-plugin allows custom configuration properties defined in plugin metadata.
     */
    public abstract boolean isAllowCustomProperties();
    /**
     * @return true to provide conventional mapping from config properties to framework/project properties.
     */
    public boolean isUseConventionalPropertiesMapping(){
        return false;
    }

    public ScriptPluginProvider getProvider() {
        return provider;
    }

    public Framework getFramework() {
        return framework;
    }
}
