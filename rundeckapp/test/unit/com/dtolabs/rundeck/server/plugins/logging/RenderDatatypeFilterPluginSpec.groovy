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

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/26/17
 */
class RenderDatatypeFilterPluginSpec extends Specification {

    @Unroll
    def "test detect type"() {
        given:
        def plugin = new RenderDatatypeFilterPlugin()
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
        1 * context.log(2, output, meta)

        where:
        lines                                                         | output                | meta
        ['#BEGIN:RUNDECK:DATATYPE:text/csv', '1,2,3', '---', 'a,b,c'] | '1,2,3\n---\na,b,c\n' | ['content-data-type': 'text/csv']
        ['#BEGIN:RUNDECK:DATATYPE:text/csv', '','','1,2,3', '---', 'a,b,c'] | '1,2,3\n---\na,b,c\n' | ['content-data-type': 'text/csv']

    }

    @Unroll
    def "test ignore non-normal log level output"() {
        given:
        def plugin = new RenderDatatypeFilterPlugin()
        plugin.datatype = 'csv'
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []

        events << Mock(LogEventControl) {
            getMessage() >> 'abc'
            getEventType() >> 'log'
            getLoglevel() >> loglevel
        }

        events << Mock(LogEventControl) {
            getMessage() >> 'expected'
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        events << Mock(LogEventControl) {
            getMessage() >> 'def'
            getEventType() >> 'log'
            getLoglevel() >> loglevel
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)

        then:
        1 * context.log(2, 'expected\n', ['content-data-type': 'text/csv'])

        where:
        loglevel         | _
        LogLevel.DEBUG   | _
        LogLevel.WARN    | _
        LogLevel.ERROR   | _
        LogLevel.VERBOSE | _

    }

    @Unroll
    def "test preset type "() {
        given:
        def plugin = new RenderDatatypeFilterPlugin()
        plugin.datatype = datatype
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
        1 * context.log(2, output, meta)

        where:
        datatype   | lines                             | output                | meta
        'text/csv' | ['1,2,3', '---', 'a,b,c']         | '1,2,3\n---\na,b,c\n' | ['content-data-type': 'text/csv']
        'csv'      | ['', '', '1,2,3', '---', 'a,b,c'] | '1,2,3\n---\na,b,c\n' | ['content-data-type': 'text/csv']
    }

    @Unroll
    def "data type synonym #datatype should be #value"() {
        given:
        def plugin = new RenderDatatypeFilterPlugin()
        plugin.datatype = datatype
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        events << Mock(LogEventControl) {
            getMessage() >> 'a'
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)

        then:
        1 * context.log(2, 'a\n', ['content-data-type': value])

        where:
        datatype        | value
        'csv'           | 'text/csv'
        'CSV'           | 'text/csv'
        'json'          | 'application/json'
        'JSON'          | 'application/json'
        'properties'    | 'application/x-java-properties'
        'PROPERTIES'    | 'application/x-java-properties'
        'html'          | 'text/html'
        'HTML'          | 'text/html'
        'markdown'      | 'text/x-markdown'
        'MARKDOWN'      | 'text/x-markdown'
        'md'            | 'text/x-markdown'
        'MD'            | 'text/x-markdown'
        'anything/else' | 'anything/else'
    }
}
