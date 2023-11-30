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

import com.dtolabs.rundeck.core.common.INodeEntry
import org.rundeck.core.execution.ScriptFileCommand
import com.dtolabs.rundeck.core.execution.proxy.ProxyRunnerPlugin
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext


@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = SCRIPT_FILE_COMMAND_TYPE)
@PluginDescription(title = "Script file or URL", description = "Verify and validate design", isHighlighted = true, order = 2)
class ScriptFileNodeStepPlugin extends ScriptProxyRunner implements NodeStepPlugin, ScriptFileCommand, ProxyRunnerPlugin {

    @PluginProperty(title = "File Path or URL",
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

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {

        if(adhocFilepath ==~ /^(?i:https?|file):.*$/) {
            ScriptURLNodeStepExecutor scriptURLNodeStepExecutor = new ScriptURLNodeStepExecutor(
                    context,
                    scriptInterpreter,
                    interpreterArgsQuoted,
                    fileExtension,
                    argString,
                    adhocFilepath,
                    expandTokenInScriptFile
            );

            scriptURLNodeStepExecutor.executeScriptURL(configuration, entry);

        } else {
            ScriptFileNodeStepExecutor scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                    scriptInterpreter,
                    interpreterArgsQuoted,
                    fileExtension,
                    argString,
                    adhocFilepath,
                    null,
                    expandTokenInScriptFile
            );

            scriptFileNodeStepExecutor.executeScriptFile(context, configuration, entry);
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
