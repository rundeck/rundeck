package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import spock.lang.Specification
import spock.lang.Unroll

class MaskLogOutputByRegexPluginSpec extends Specification {
    @Unroll
    def "test"() {
        given:
        def plugin = new MaskLogOutputByRegexPlugin()
        plugin.regex = regex
        plugin.replacement = replacement
        plugin.maskOnlyValue = maskOnlyValue
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        lines.each { line ->
            events << Mock(LogEventControl) {
                getMessage() >> line
                getEventType() >> 'log'
                getLoglevel() >> LogLevel.NORMAL
            }
        }

        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)

        then:
        events.each {
            1 * it.setMessage({ expect.contains(it) })
        }

        where:
        replacement | regex                      | lines                                                                               | expect                                                                             | maskOnlyValue
        "[****]"    | "-Dsome-password=pwsd123"  | ['-Drun.active=true -Dsome-password=pwsd123 -Drundeck.server.logDir=some/path/dir'] | ['-Drun.active=true [****] -Drundeck.server.logDir=some/path/dir']                 | false
        "[****]"    | "(-Dsome-password=)(\\w+)" | ['-Drun.active=true -Dsome-password=pwsd123 -Drundeck.server.logDir=some/path/dir'] | ['-Drun.active=true -Dsome-password=[****] -Drundeck.server.logDir=some/path/dir'] | true
        "[SECURE]"  | "(RD_JOB_PASSWORD=)(.*)"   | ["RD_JOB_USER_NAME=admin", "RD_JOB_PASSWORD=pswd123"]                               | ["RD_JOB_USER_NAME=admin", "RD_JOB_PASSWORD=[SECURE]"]                             | true
        "[SECURE]"  | "(RD_JOB_PASSWORD=)(.*)"   | ["RD_JOB_USER_NAME=admin", "RD_JOB_PASSWORD=pswd123"]                               | ["RD_JOB_USER_NAME=admin", "[SECURE]"]                                             | false
    }
}
