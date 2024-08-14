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
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
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
@PluginDescription(title = "Script file or URL", description = "Execute a local script file or a script from a URL", isHighlighted = true, order = 2)
class ScriptFileNodeStepPlugin extends ScriptProxyRunner
    implements NodeStepPlugin, ScriptFileCommand, PluginResourceLoader, ProxyRunnerPlugin, SecureInputProps {

    @PluginProperty(title = "File Path or URL",
            description = "Enter the path to a script file on the server or a URL",
            required = true)
    String adhocFilepath;

    @PluginProperty(title = "Arguments",
            description = "Enter the commandline arguments for the script",
            required = false)
    String argString;

    @PluginProperty(title = "Invocation String",
            description = '''Leave blank to run script directly
Specify how to invoke the script file. By default the temporary script file path will be appended to this string, followed by any arguments. Include `${scriptfile}` anywhere to change the file path argument location:
_Examples_:
*   `sudo ${scriptfile}`
*   `time ${scriptfile}`
*   `python -u ${scriptfile}`
*   `mytool -f ${scriptfile} -action execute -args`
''',
            required = false)
    String scriptInterpreter;

    @PluginProperty(title = "Expand variables in script file",
            description = "",
            required = false)
    Boolean expandTokenInScriptFile;

    @PluginProperty(title = "Quote arguments to script invocation string?",
            description = '''If arguments are quoted, then the arguments passed to the invocation string will be quoted as one string.
- Unquoted invocation: 
	    $ [invocation string] args ...
- Quoted invocation: 
	    $ [invocation string] 'args ...'
Note: the scriptfile can be included in the quoted arguments by not specifying `${scriptfile}` within the Invocation String.
''',
            required = false)
    Boolean interpreterArgsQuoted;

    @PluginProperty(title = "File Extension",
            description = '''Leave blank to use the default for the target node.
The file extension is used by the script file when it is copied to the node. Leave blank to use the default for the target node.  
The `.` is optional.  
E.g.: `.ps1`, or `abc`.
''',
            required = false)
    String fileExtension;

    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry)
        throws NodeStepException {
        InputStream input = null
        FileCopierUtil.ContentModifier modifier = null

        if (nodeExecutorSecureInput && passSecureInput) {
            if (secureFormat != 'shell') {
                throw new IllegalArgumentException("Unsupported secure input format: " + secureFormat)
            }
            input = new SecureInputCreator(new BashShellUtil()).createInputForProcess(context.executionContext)
        }
        if (nodeExecutorSecureInput && passSecureInput && autoSecureInput) {
            modifier = new ShellScriptModifier()
        }
        if(adhocFilepath ==~ /^(?i:https?|file):.*$/) {
            ScriptURLNodeStepExecutor scriptURLNodeStepExecutor = new ScriptURLNodeStepExecutor(
                    context,
                    scriptInterpreter,
                    interpreterArgsQuoted,
                    fileExtension,
                    argString,
                    adhocFilepath,
                    expandTokenInScriptFile,
                    modifier
            );

            scriptURLNodeStepExecutor.executeScriptURL(entry, input);

        } else {
            ScriptFileNodeStepExecutor scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                    scriptInterpreter,
                    interpreterArgsQuoted,
                    fileExtension,
                    argString,
                    adhocFilepath,
                    null,
                    expandTokenInScriptFile,
                    modifier
            );

            scriptFileNodeStepExecutor.executeScriptFile(context, entry, input);
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

    @Override
    List<String> listResources() throws PluginException, IOException {
        ['WorkflowNodeStep.script-file-url.icon.png']
    }

    @Override
    InputStream openResourceStreamFor(String name) throws PluginException, IOException {
        return this.getClass().getResourceAsStream("/" + name)
    }

}
