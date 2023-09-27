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

package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.BaseCommandExec
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = ScriptFileNodeStepPlugin.PROVIDER_NAME)
@PluginDescription(title = "Script file or URL", description = "Verify and validate design", isHighlighted = true, order = 2)
public class ScriptFileNodeStepPlugin implements NodeStepPlugin, BaseCommandExec {
    public static final String PROVIDER_NAME = "script-file-node-step-plugin";

    @PluginProperty(title = "File Path",
            description = "Path",
            required = true)
    String adhocFilepath;

    @PluginProperty(title = "Arguments",
            description = "Arguments",
            required = false)
    String argString;

    @PluginProperty(title = "Invocation String",
            description = "",
            required = false)
    String scriptInterpreter;

    @PluginProperty(title = "Expand variables in script file",
            description = "",
            required = false)
    Boolean expandTokenInScriptFile;

    @PluginProperty(title = "Quote arguments to script invocation string?",
            description = "",
            required = false)
    Boolean interpreterArgsQuoted;

    @PluginProperty(title = "File Extension",
            description = "",
            required = false)
    String fileExtension;

    private DefaultScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    @Override
    public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
        {
            boolean expandTokens = true;
            if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
                expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
            }
            if(null != adhocFilepath){
                expandTokens = expandTokenInScriptFile;
            }

            String expandedVarsInURL = SharedDataContextUtils.replaceDataReferences(
                    adhocFilepath,
                    context.getExecutionContext().getSharedDataContext(),
                    //add node name to qualifier to read node-data first
                    ContextView.node(entry.getNodename()),
                    ContextView::nodeStep,
                    DataContextUtils.replaceMissingOptionsWithBlank,
                    false,
                    false
            );

            if( DataContextUtils.hasOptionsInString(expandedVarsInURL) ){
                Map<String, Map<String, String>> optionsContext = new HashMap();
                optionsContext.put("option", context.getDataContext().get("option"));
                expandedVarsInURL = DataContextUtils.replaceDataReferencesInString(expandedVarsInURL, optionsContext);
            }

            final String[] args;
            if (null != argString) {
                args = OptsUtil.burst(argString);
            } else {
                args = new String[0];
            }

            scriptUtils.executeScriptFile(
                    context.getExecutionContext(),
                    entry,
                    null,
                    expandedVarsInURL,
                    null,
                    fileExtension,
                    args,
                    scriptInterpreter,
                    interpreterArgsQuoted,
                    context.getFramework().getExecutionService(),
                    expandTokens
            );
        }
    }

    @Override
    String getAdhocRemoteString() {
        return null
    }

    @Override
    String getAdhocLocalString() {
        return null
    }

    @Override
    Boolean getAdhocExecution() {
        return null
    }
}
