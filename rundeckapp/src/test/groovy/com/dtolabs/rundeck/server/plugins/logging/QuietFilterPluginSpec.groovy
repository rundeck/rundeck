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

package com.dtolabs.rundeck.server.plugins.logging

import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 6/6/17
 */
class QuietFilterPluginSpec extends Specification {
    @Unroll
    def "quiet match with regex #regex"() {
        given:
        def plugin = new QuietFilterPlugin()
        plugin.regex = regex
        plugin.quietMatch = quietMatch
        def loggingContext = Mock(PluginLoggingContext) {
        }
        def event = Mock(LogEventControl) {
            getMessage() >> message
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        when:
        plugin.init(loggingContext)
        plugin.handleEvent(loggingContext, event)


        then:
        if (quiet) {
            1 * event.quiet()
        } else {
            0 * event.quiet()
        }

        where:
        regex     | quietMatch | quiet
        '(test)'  | true       | true
        '(test)'  | false      | false
        '(testz)' | true       | false
        '(testz)' | false      | true
        ''        | true       | true
        ''        | false      | false
        null      | true       | true
        null      | false      | false
        message = 'this is a test'
    }

    @Unroll
    def "quiet match with match log level #inlevel and match level #matchLoglevel"() {
        given:
        def plugin = new QuietFilterPlugin()
        plugin.regex = regex
        plugin.quietMatch = quietMatch
        plugin.matchLoglevel = matchLoglevel
        def loggingContext = Mock(PluginLoggingContext) {
        }
        def event = Mock(LogEventControl) {
            getMessage() >> message
            getEventType() >> 'log'
            getLoglevel() >> inlevel
        }
        when:
        plugin.init(loggingContext)
        plugin.handleEvent(loggingContext, event)


        then:
        if (quiet) {
            1 * event.quiet()
        } else {
            0 * event.quiet()
        }

        where:
        inlevel          | matchLoglevel || quiet
        LogLevel.NORMAL  | null          || true
        LogLevel.NORMAL  | ''            || true
        LogLevel.NORMAL  | 'normal'      || true
        LogLevel.NORMAL  | 'warn'        || false
        LogLevel.WARN    | 'normal'      || false
        LogLevel.WARN    | 'warn'        || true
        LogLevel.ERROR   | 'error'       || true
        LogLevel.VERBOSE | 'verbose'     || true
        LogLevel.DEBUG   | 'debug'       || true
        LogLevel.WARN    | 'all'         || true
        quietMatch = true
        regex = 'test'
        message = 'this is a test'
    }

    @Unroll
    def "quiet match output loglevel #loglevel"() {
        given:
        def plugin = new QuietFilterPlugin()
        plugin.regex = regex
        plugin.quietMatch = quietMatch
        plugin.loglevel = loglevel
        def loggingContext = Mock(PluginLoggingContext) {
        }
        def event = Mock(LogEventControl) {
            getMessage() >> message
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        when:
        plugin.init(loggingContext)
        plugin.handleEvent(loggingContext, event)


        then:
        if (loglevel) {
            1 * event.setLoglevel(expect)
        }
        if (quiet) {
            1 * event.quiet()
        }

        where:
        loglevel  | expect           | quiet
        'error'   | LogLevel.ERROR   | false
        'warn'    | LogLevel.WARN    | false
        'normal'  | LogLevel.NORMAL  | false
        'verbose' | LogLevel.VERBOSE | false
        'debug'   | LogLevel.DEBUG   | false
        ''        | null             | true
        null      | null             | true

        quietMatch = true
        regex = 'test'
        message = 'this is a test'
    }
}
