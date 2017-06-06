/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
    def "test"() {
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
        regex     | message          | quietMatch | quiet
        '(test)'  | 'this is a test' | true       | true
        '(test)'  | 'this is a test' | false      | false
        '(testz)' | 'this is a test' | true       | false
        '(testz)' | 'this is a test' | false      | true
    }
}
