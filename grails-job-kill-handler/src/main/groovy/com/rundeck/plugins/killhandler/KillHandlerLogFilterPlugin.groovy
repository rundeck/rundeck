package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Plugin(name = KillHandlerLogFilterPlugin.PROVIDER_NAME, service = ServiceNameConstants.LogFilter)
@PluginDescription(title = KillHandlerLogFilterPlugin.PLUGIN_TITLE, description = '''Captures process id numbers for later cleanup by the Kill Handler Plugin.\n\n
By default, to capture a pid entry, echo a line similar to this:

    RUNDECK:PID:(pid)

Where `(pid)` is any process id number to track.

To customize the PID capture pattern, you can define the regular expression used to read process numbers from the log output.
''')
class KillHandlerLogFilterPlugin implements LogFilterPlugin {
    private final Logger log = LoggerFactory.getLogger(KillHandlerLogFilterPlugin.name)
    static final String PLUGIN_TITLE = "Capture Process IDs"
    static final String PROVIDER_NAME = 'killhandler-logfilter'
    static final String PATTERN = '^RUNDECK:PID:\\h*(\\d+)\\h*$'

    @PluginProperty(
            title = "Pattern",
            description = '''Regular Expression for matching process ids.

The regular expression must define one Capturing Group. The first group matched defines
the process id to parse, and the text matched must be a valid integer number.

See the [Java Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) documentation.''',
            defaultValue = KillHandlerLogFilterPlugin.PATTERN,
            required = true,
            validatorClass = RegexValidator
    )
    String regex

    private Pattern dataPattern;
    KillHandlerProcessTrackingService processTrackingService

    @Override
    void init(PluginLoggingContext context) {
        dataPattern = Pattern.compile(regex)
    }

    @Override
    void handleEvent(PluginLoggingContext context, LogEventControl event) {
        if (event.eventType == 'log' && event.loglevel == LogLevel.NORMAL && event.message?.length() > 0) {
            Matcher match = dataPattern.matcher(event.message)
            if (match.matches()) {
                if (match.groupCount() > 0) {
                    def execId = context.dataContext.job.execid
                    def nodename = event.metadata.node
                    def pid = match.group(1)
                    processTrackingService.registerPID(execId, nodename, pid)
                }
            }
        }
    }

    static class RegexValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                return Pattern.compile(value) != null
            } catch (PatternSyntaxException e) {
                throw new ValidationException(e.message, e)
            }
        }
    }
}

