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

/*
* ScriptPluginResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 11:58 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.plugins.AbstractDescribableScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import org.rundeck.app.spi.Services;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ScriptPluginResourceModelSourceFactory creates ResourceModelSource from a ScriptPluginProvider and a set of
 * configuration properties.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptPluginResourceModelSourceFactory extends AbstractDescribableScriptPlugin implements
    ResourceModelSourceFactory {
    public static final String RESOURCE_FORMAT_PROP = "resource-format";
    public static final String DISABLE_CONTENT_CONVERSION = "disableContentConversion";

    final String format;

    public ScriptPluginResourceModelSourceFactory(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
        final Object o = provider.getMetadata().get(RESOURCE_FORMAT_PROP);
        if (o instanceof String) {
            format = (String) o;
        } else {
            throw new IllegalArgumentException(RESOURCE_FORMAT_PROP + " was not a string");
        }
    }


    public static void validateScriptPlugin(final ScriptPluginProvider provider) throws PluginException {

        try {
            createDescription(provider, true, DescriptionBuilder.builder());
        } catch (ConfigurationException e) {
            throw new PluginException(e);
        }
        if (!provider.getMetadata().containsKey(RESOURCE_FORMAT_PROP) || !(provider.getMetadata().get(
            RESOURCE_FORMAT_PROP) instanceof String)) {
            throw new PluginException(RESOURCE_FORMAT_PROP + " script plugin property string is required");
        }
    }


    @Override
    public ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException {
        return createResourceModelSource(null, configuration);
    }

    @Override
    public ResourceModelSource createResourceModelSource(final Services services, final Properties configuration) throws ConfigurationException {

        Map<String, String> data = configuration.entrySet().stream().collect(
                Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().toString()
                )
        );

        ExecutionContext context = null;

        if(services!=null){
            context = new ExecutionContextImpl.Builder()
                    .framework(this.getFramework())
                    .storageTree(services.getService(KeyStorageTree.class))
                    .build();
        }else{
            context = new ExecutionContextImpl.Builder()
                    .framework(this.getFramework())
                    .build();
        }

        Description pluginDesc = getDescription();

        boolean disableContentConversion = false;

        if(configuration.containsKey(DISABLE_CONTENT_CONVERSION)){
            disableContentConversion = (Boolean)configuration.get(DISABLE_CONTENT_CONVERSION);
        }

        if(!disableContentConversion){
            loadContentConversionPropertyValues(
                    data,
                    context,
                    pluginDesc.getProperties()
            );
        }


        Properties properties = new Properties();
        properties.putAll(data);

        final ScriptPluginResourceModelSource urlResourceModelSource = new ScriptPluginResourceModelSource(
            getProvider(), getFramework(), this);
        urlResourceModelSource.configure(properties);
        return urlResourceModelSource;
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }
}
