package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
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
class ScriptNodeStepPlugin extends ScriptProxyRunner implements NodeStepPlugin, ScriptCommand, PluginResourceLoader {
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

    @PluginProperty(
        title = 'Pass Secure Input',
        description = 'If enabled, secure option data will be sent via the standard input stream to the script.',
        required = false
    )
    @RenderingOptions(
        [
            @RenderingOption(key = StringRenderingConstants.FEATURE_FLAG_REQUIRED, value = 'nodeExecutorSecureInput'),
            @RenderingOption(
                key = StringRenderingConstants.GROUPING,
                value = "secondary"
            ),
            @RenderingOption(
                key = StringRenderingConstants.GROUP_NAME,
                value = "Advanced"
            ),
        ]
    )
    Boolean passSecureInput


    @PluginProperty(
        title = 'Secure Input Format',
        description = '''The format of the secure input data. See instructions for consuming this data in your script.

# Shell Script

The secure input data will be formatted as local variable definitions, this should be consumed by your
shell script by adding this snippet: 

    eval $(</dev/stdin)

If "Automatic Secure Input" is set, then the script will be modified to consume the secure input data automatically.
''',
        required = false
    )
    @SelectValues(values = ['shell'])
    @SelectLabels(values = ['Shell Script'])
    @RenderingOptions(
        [
            @RenderingOption(key = StringRenderingConstants.FEATURE_FLAG_REQUIRED, value = 'nodeExecutorSecureInput'),
            @RenderingOption(
                key = StringRenderingConstants.GROUPING,
                value = "secondary"
            ),
            @RenderingOption(
                key = StringRenderingConstants.GROUP_NAME,
                value = "Advanced"
            ),
        ]
    )
    String secureFormat

    @PluginProperty(
        title = 'Automatic Secure Input',
        description = '''If enabled, the script will be modified to consume the secure input data automatically.

# Shell Script

If the script is a shell script, the script will be modified by adding the following snippet to the beginning of the 
script, after any shebang line:

    eval $(</dev/stdin)

''',
        required = false
    )
    @RenderingOptions(
        [
            @RenderingOption(key = StringRenderingConstants.FEATURE_FLAG_REQUIRED, value = 'nodeExecutorSecureInput'),
            @RenderingOption(
                key = StringRenderingConstants.GROUPING,
                value = "secondary"
            ),
            @RenderingOption(
                key = StringRenderingConstants.GROUP_NAME,
                value = "Advanced"
            ),
        ]
    )
    Boolean autoSecureInput


    @PluginProperty(scope = PropertyScope.FeatureFlag)
    Boolean nodeExecutorSecureInput


    @Override
    void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws
        NodeStepException
    {
        InputStream input = null
        String script = adhocLocalString

        if (nodeExecutorSecureInput && passSecureInput) {
            input = createInput(context)
        }
        if (nodeExecutorSecureInput && passSecureInput && autoSecureInput) {
            script = new ShellScriptModifier().modifyScriptForSecureInput(adhocLocalString)
        }

        ScriptFileNodeStepExecutor scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
            scriptInterpreter,
            interpreterArgsQuoted,
            fileExtension,
            argString,
            null,
            script,
            true
        )

        scriptFileNodeStepExecutor.executeScriptFile(context, entry, input)
    }

    private InputStream createInput(PluginStepContext context) {
        if (secureFormat != 'shell') {
            throw new IllegalArgumentException("Unsupported secure input format: " + secureFormat)
        }

        new SecureInputCreator(new BashShellUtil()).createInputForProcess(context.executionContext)
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
