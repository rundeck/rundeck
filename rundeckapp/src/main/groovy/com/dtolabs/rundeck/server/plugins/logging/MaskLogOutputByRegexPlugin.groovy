package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Plugin(name = MaskLogOutputByRegexPlugin.PROVIDER_NAME, service = 'LogFilter')
@PluginDescription(title = 'Mask Log Output By Regex',
        description = "Mask sensitive output that match the defined Regex")

class MaskLogOutputByRegexPlugin implements LogFilterPlugin {
    public static final String PROVIDER_NAME = 'mask-log-output-regex'

    @PluginProperty(
            title = "Pattern",
            description = "Regular Expression for matching output data.",
            required = true,
            validatorClass = MaskLogOutputByRegexPlugin.RegexValidator
    )
    String regex

    @PluginProperty(
            title = "Replacement",
            description = "Text to replace secure values",
            defaultValue = "[SECURE]",
            required = true
    )
    String replacement

    @PluginProperty(
            title = 'Mask only value on key/value text based',
            description = '''If true, will mask only value on a key/value text based. The regular expression must define two Capturing Groups.''',
            defaultValue = 'false'
    )
    Boolean maskOnlyValue


    static class RegexValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                def compile = Pattern.compile(value)
                return true
            } catch (PatternSyntaxException e) {
                throw new ValidationException(e.message, e)
            }
        }
    }

    @Override
    void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        def message = event.message
        if (event.eventType == 'log' && message) {
            String remessage = message.replaceAll(regex, (maskOnlyValue ? '$1' + replacement : replacement))
            event.setMessage(remessage)
        }
    }
}
