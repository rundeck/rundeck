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
* ScriptBasedRemoteScriptNodeStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/12/12 10:14 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.data.MutableDataContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.step.GeneratedScript;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.File;
import java.util.Map;


/**
 * ScriptBasedRemoteScriptNodeStepPlugin is a {@link RemoteScriptNodeStepPlugin} that uses a {@link
 * ScriptPluginProvider}. The script used by the provider is dispatched to the node.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptBasedRemoteScriptNodeStepPlugin extends BaseScriptPlugin implements RemoteScriptNodeStepPlugin {

    public static final String SCRIPT_FILE_EXTENSION_META_KEY = "script-file-extension";
    public static final String SCRIPT_FILE_USE_EXTENSION_META_KEY = "use-original-file-extension";

    ScriptBasedRemoteScriptNodeStepPlugin(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
        try {
            createDescription(plugin, true, DescriptionBuilder.builder());
        } catch (ConfigurationException e) {
            throw new PluginException(e);
        }
    }

    @Override
    public GeneratedScript generateScript(
            final PluginStepContext context,
            final Map<String, Object> configuration,
            final INodeEntry node
    ) throws NodeStepException
    {
        final ScriptPluginProvider provider = getProvider();
        Description description = getDescription();

        Map<String, String> configData = toStringStringMap(configuration);
        try {
            loadContentConversionPropertyValues(
                    configData,
                    context.getExecutionContext(),
                    description.getProperties()
            );
        } catch (ConfigurationException e) {
            throw new NodeStepException(e.getMessage(), e, StepFailureReason.ConfigurationFailure, node.getNodename());
        }

        final MutableDataContext finalDataContext = DataContextUtils.context("config", configData);
        finalDataContext.merge(context.getDataContextObject());

        //NB: dont generate final args yet, they will be constructed by node dispatch layer
        final String args = provider.getScriptArgs();
        String[] argsarr = provider.getScriptArgsArray();
        if (null != args) {
            argsarr = args.split(" ");
        }
        argsarr = finalDataContext.replaceDataReferences(argsarr);

        boolean useOriginalFileExtension = true;
        if (provider.getMetadata().containsKey(SCRIPT_FILE_USE_EXTENSION_META_KEY)) {
            useOriginalFileExtension = getMetaBoolean(
                    provider,
                    SCRIPT_FILE_USE_EXTENSION_META_KEY,
                    true
            );
        }
        String fileExtension = null;
        if (provider.getMetadata().containsKey(SCRIPT_FILE_EXTENSION_META_KEY)) {
            Object o = provider.getMetadata().get(SCRIPT_FILE_EXTENSION_META_KEY);
            if (o instanceof String) {
                fileExtension = (String) o;
            }
        }
        if (null == fileExtension && useOriginalFileExtension) {
            fileExtension = getFileExtension(provider.getScriptFile().getName());
        }

        return createFileGeneratedScript(
                provider.getScriptFile(),
                argsarr,
                fileExtension,
                provider.getScriptInterpreter(),
                provider.getInterpreterArgsQuoted(),
                configData
        );
    }

    static String getFileExtension(final String name) {
        int i = name.lastIndexOf(".");
        if (i > 0 && i < name.length() - 1) {
            return name.substring(i + 1);
        }
        return null;
    }

    private boolean getMetaBoolean(final ScriptPluginProvider provider, final String key, final boolean defVal) {
        Object o = provider.getMetadata().get(key);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else if(o!=null) {
            return Boolean.parseBoolean(o.toString());
        }
        return defVal;
    }


    static GeneratedScript createFileGeneratedScript(
            final File file,
            final String[] args,
            final String fileExtension,
            final String scriptInterpreter,
            final boolean interpreterArgsQuoted,
            final Map<String, String> configData
    )
    {

        return new FileBasedGeneratedScript() {
            @Override
            public File getScriptFile() {
                return file;
            }

            @Override
            public String getScript() {
                return null;
            }

            @Override
            public String[] getArgs() {
                return args;
            }

            @Override
            public String[] getCommand() {
                return null;
            }

            @Override
            public String getFileExtension() {
                return fileExtension;
            }

            @Override
            public String getScriptInterpreter() {
                return scriptInterpreter;
            }

            @Override
            public boolean isInterpreterArgsQuoted() {
                return interpreterArgsQuoted;
            }

            @Override
            public Map<String, String> getConfigData() {
                return configData;
            }
        };
    }

}
