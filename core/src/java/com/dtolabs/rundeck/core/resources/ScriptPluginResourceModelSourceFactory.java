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
* ScriptPluginResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 11:58 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.*;

import java.util.*;

/**
 * ScriptPluginResourceModelSource is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    public static final String RESOURCE_FORMAT_PROP = "resource-format";
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

    final ScriptPluginProvider provider;
    final Framework framework;
    Description providerDescription;
    final String format;
    final Nodes.Format resformat;

    public ScriptPluginResourceModelSourceFactory(final ScriptPluginProvider provider, final Framework framework) {
        this.provider = provider;
        this.framework = framework;
        format = provider.getMetadata().get(RESOURCE_FORMAT_PROP);
        resformat = Nodes.Format.valueOf(format);
    }


    static private List<Property> createProperties(final ScriptPluginProvider provider) throws ConfigurationException {
        final ArrayList<Property> properties = new ArrayList<Property>();
        int i = 1;
        final Map<String, String> metadata = provider.getMetadata();
        while (metadata.containsKey(CONFIG_PROP_PREFIX + "." + i + "." + CONFIG_TYPE)) {
            final String prefix = CONFIG_PROP_PREFIX + "." + i + ".";
            i++;
            final String typestr = metadata.get(prefix + CONFIG_TYPE);
            final Property.Type type;
            try {
                type = Property.Type.valueOf(typestr);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Invalid property type: " + typestr);
            }
            final String key = metadata.get(prefix + CONFIG_NAME);
            final String name = metadata.get(prefix + CONFIG_TITLE);
            final String description = metadata.get(prefix + CONFIG_DESCRIPTION);
            final boolean required = Boolean.parseBoolean(metadata.get(prefix + CONFIG_REQUIRED));
            final String defaultValue = metadata.get(prefix + CONFIG_DEFAULT);
            if (null == key) {
                throw new ConfigurationException("Name required");
            }
            final String valuesstr = metadata.get(prefix + CONFIG_VALUES);
            final String[] split = null != valuesstr ? valuesstr.split(",") : null;
            final List<String> values = null != split ? Arrays.asList(split) : null;
            if (type == Property.Type.Select) {
                properties.add(PropertyUtil.select(key, name, description, required, defaultValue, values));
            } else if (type == Property.Type.FreeSelect) {
                properties.add(PropertyUtil.freeSelect(key, name, description, required, defaultValue, values));
            } else {
                properties.add(PropertyUtil.forType(type, key, name, description, required, defaultValue, null));
            }
        }
        return properties;
    }

    public static void validateScriptPlugin(final ScriptPluginProvider provider) throws PluginException {

        try {
            createDescription(provider);
        } catch (ConfigurationException e) {
            throw new PluginException(e);
        }
        if (!provider.getMetadata().containsKey(RESOURCE_FORMAT_PROP)) {
            throw new PluginException(RESOURCE_FORMAT_PROP + " script plugin property is required");
        }else{

            final String name = provider.getMetadata().get(RESOURCE_FORMAT_PROP);
            try {
                Nodes.Format.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new PluginException("Invalid format: " + name);
            }
        }
    }

    private static Description createDescription(final ScriptPluginProvider provider) throws ConfigurationException {
        final String title = null != provider.getMetadata().get(TITLE_PROP) ? provider.getMetadata().get(TITLE_PROP)
                                                                            : provider.getName() + " Script Plugin";
        final String description = null != provider.getMetadata().get(DESCRIPTION_PROP) ? provider.getMetadata().get(
            DESCRIPTION_PROP) : "";
        final List<Property> properties = createProperties(provider);

        return new Description() {
            public String getName() {
                return provider.getName();
            }

            public String getTitle() {
                return title;
            }

            public String getDescription() {
                return description;
            }

            public List<Property> getProperties() {
                return properties;
            }
        };
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {

        final ScriptPluginResourceModelSource urlResourceModelSource = new ScriptPluginResourceModelSource(
            provider,framework);
        urlResourceModelSource.configure(configuration);
        return urlResourceModelSource;
    }

    public Description getDescription() {
        if (null == providerDescription) {
            try {
                providerDescription = createDescription(provider);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
        return providerDescription;
    }
}
