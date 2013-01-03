/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
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
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
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
    public GeneratedScript generateScript(final PluginStepContext context,
                                          final Map<String, Object> configuration,
                                          final INodeEntry entry)  {
        final ScriptPluginProvider provider = getProvider();
        final String args = provider.getScriptArgs();
        final String[] argsarr;
        if (null != args) {
            argsarr = args.split(" ");
        } else {
            argsarr = null;
        }

        return createFileGeneratedScript(
            provider.getScriptFile(),
            argsarr,
            provider.getScriptInterpreter(),
            provider.getInterpreterArgsQuoted()
        );
    }


    static GeneratedScript createFileGeneratedScript(final File file,
                                                     final String[] args,
                                                     final String scriptInterpreter,
                                                     final boolean interpreterArgsQuoted) {

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
            public String getScriptInterpreter() {
                return scriptInterpreter;
            }

            @Override
            public boolean isInterpreterArgsQuoted() {
                return interpreterArgsQuoted;
            }
        };
    }

}
