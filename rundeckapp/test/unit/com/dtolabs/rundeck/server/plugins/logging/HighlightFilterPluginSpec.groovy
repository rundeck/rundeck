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
class HighlightFilterPluginSpec extends Specification {
    @Unroll
    def "test matching"() {
        given:
        def plugin = new HighlightFilterPlugin()
        plugin.regex = regex
        plugin.testMark = 'X'
        def context = Mock(PluginLoggingContext)
        def event = Mock(LogEventControl) {
            getMessage() >> message
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        when:
        plugin.init(context)
        plugin.handleEvent(context, event)

        then:
        1 * event.setMessage(expected)

        where:
        regex                         | message                   || expected
        'test'                        | 'this is a test'          || 'this is a XtestX'
        'this is a (test)'            | 'this is a test'          || 'this is a XtestX'
        'this is a (test) whatever'   | 'this is a test whatever' || 'this is a XtestX whatever'
        'this (is) a (test) whatever' | 'this is a test whatever' || 'this XisX a XtestX whatever'
    }

    @Unroll
    def "test fg colorization"() {
        given:
        def plugin = new HighlightFilterPlugin()
        plugin.regex = regex
        plugin.fgcolor = fgcolor
        plugin.bgcolor = bgcolor
        def context = Mock(PluginLoggingContext)
        def event = Mock(LogEventControl) {
            getMessage() >> message
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        when:
        plugin.init(context)
        plugin.handleEvent(context, event)

        then:
        1 * event.setMessage(expected)

        where:
        fgcolor | bgcolor | expected
        'red'   | null    | 'this \u001B[31mis\u001B[0m a \u001B[31mtest\u001B[0m whatever'
        'red'   | 'black' | 'this \u001B[31;40mis\u001B[0m a \u001B[31;40mtest\u001B[0m whatever'
        null    | 'black' | 'this \u001B[40mis\u001B[0m a \u001B[40mtest\u001B[0m whatever'

        regex = 'this (is) a (test) whatever'
        message = 'this is a test whatever'
    }

}
