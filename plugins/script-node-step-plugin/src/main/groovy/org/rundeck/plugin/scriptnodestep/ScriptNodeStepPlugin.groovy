package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import groovy.transform.CompileStatic
import org.rundeck.core.execution.ScriptCommand
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext

@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = SCRIPT_COMMAND_TYPE)
@PluginDescription(title = "Script", description = "Execute an inline script", isHighlighted = true, order = 1)
@CompileStatic
class ScriptNodeStepPlugin extends ScriptProxyRunner implements NodeStepPlugin, ScriptCommand, PluginResourceLoader, SecureInputProps {
    public static final String PROVIDER_NAME = "script-node-step-plugin";

    @PluginProperty(
            title = "Script",
            description = "Enter the entire script to execute",
            required = true
    )
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'CODE'),
                    @RenderingOption(key = 'codeSyntaxMode', value = 'sh')
            ]
    )
    String adhocLocalString

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
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws
        NodeStepException
    {
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

        ScriptFileNodeStepExecutor scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
            scriptInterpreter,
            interpreterArgsQuoted,
            fileExtension,
            argString,
            null,
            adhocLocalString,
            true,
            modifier
        )

        scriptFileNodeStepExecutor.executeScriptFile(context, entry, input)
    }


    @Override
    List<String> listResources() throws PluginException, IOException {
        ['WorkflowNodeStep.script-inline.icon.png']
    }

    @Override
    InputStream openResourceStreamFor(String name) throws PluginException, IOException {
        return this.getClass().getResourceAsStream("/" + name)
    }
}
