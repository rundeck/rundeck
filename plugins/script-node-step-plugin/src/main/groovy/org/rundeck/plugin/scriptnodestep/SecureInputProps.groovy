package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues

/**
 * Defines common properties for secure input configuration
 */
trait SecureInputProps {
    //Note: all properties below have explicit "name" value, because the field name created by the Groovy trait
    //will not match the expected property name.

    @PluginProperty(
        name = 'passSecureInput',
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
        name = 'secureFormat',
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
        name = 'autoSecureInput',
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


    @PluginProperty(
        name = 'nodeExecutorSecureInput',
        scope = PropertyScope.FeatureFlag
    )
    Boolean nodeExecutorSecureInput
}