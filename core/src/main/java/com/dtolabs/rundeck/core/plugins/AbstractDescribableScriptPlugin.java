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
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.step.AbstractBasePlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

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
 * </pre>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class AbstractDescribableScriptPlugin extends AbstractBasePlugin implements Describable {
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

    private final ScriptPluginProvider provider;
    private final Framework framework;

    public AbstractDescribableScriptPlugin(final ScriptPluginProvider provider, final Framework framework) {
        this.provider = provider;
        this.framework = framework;
    }

    /**
     * Return data with exported plugin details
     */
    public Map<String,String> createPluginDataContext() {
        final Map<String,String> pluginDataContext = new HashMap<String, String>();

        pluginDataContext.put("file", provider.getArchiveFile().getAbsolutePath());
        pluginDataContext.put("scriptfile", provider.getScriptFile().getAbsolutePath());
        pluginDataContext.put("base", provider.getContentsBasedir().getAbsolutePath());

        return pluginDataContext;
    }


    static private void createProperties(final ScriptPluginProvider provider,
                                                   final DescriptionBuilder dbuilder) throws ConfigurationException {
        final Map<String, Object> metadata = provider.getMetadata();
        final Object config = metadata.get("config");
        if (config instanceof List) {
            final List configs = (List) config;
            for (final Object citem : configs) {
                if (citem instanceof Map) {
                    final PropertyBuilder pbuild = PropertyBuilder.builder();
                    final Map<String, Object> itemmeta = (Map<String, Object>) citem;
                    final String typestr = metaStringProp(itemmeta, CONFIG_TYPE);
                    final Property.Type type;
                    try {
                        pbuild.type(Property.Type.valueOf(typestr));
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException("Invalid property type: " + typestr);
                    }

                    pbuild
                        .name(metaStringProp(itemmeta, CONFIG_NAME))
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
        builder
            .name(provider.getName())
            .title(metaStringProp(provider.getMetadata(), TITLE_PROP, provider.getName() + " Script Plugin"))
            .description(metaStringProp(provider.getMetadata(), DESCRIPTION_PROP, ""));

        if(allowCustomProperties) {
            createProperties(provider, builder);
        }
    }

    @Override
    protected void buildDescription(final DescriptionBuilder builder) {
        try {
            createDescription(provider, isAllowCustomProperties(), builder);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }


    /**
     * Subclasses return true if the script-plugin allows custom configuration properties defined in plugin metadata.
     */
    public abstract boolean isAllowCustomProperties();

    public ScriptPluginProvider getProvider() {
        return provider;
    }

    public Framework getFramework() {
        return framework;
    }
}
