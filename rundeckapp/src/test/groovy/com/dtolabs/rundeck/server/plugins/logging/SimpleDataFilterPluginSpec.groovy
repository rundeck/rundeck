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

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/17/17
 */
class SimpleDataFilterPluginSpec extends Specification {
    @Unroll
    def "test"() {
        given:
        def plugin = new SimpleDataFilterPlugin()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.invalidKeyPattern = null
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

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == (expect ? ['data': expect] : null)
        if (expect) {
            if (dolog) {
                1 * context.log(2, _, _)
            } else {
                0 * context.log(*_)
            }
        }


        where:
        dolog | regex                          | lines                           | expect
        true  | SimpleDataFilterPlugin.PATTERN | ['RUNDECK:DATA:wimple=zangief'] | [wimple: 'zangief']
        false | SimpleDataFilterPlugin.PATTERN | ['RUNDECK:DATA:wimple=zangief'] | [wimple: 'zangief']
        true  | SimpleDataFilterPlugin.PATTERN | ['blah', 'blee']                | [:]
        true  | SimpleDataFilterPlugin.PATTERN | ['RUNDECK:DATA:wimple=']        | [:]
        true  | SimpleDataFilterPlugin.PATTERN | ['RUNDECK:DATA:=asdf']          | [:]
        false  | SimpleDataFilterPlugin.PATTERN |
                [
                        'RUNDECK:DATA:awayward wingling samzait = bogarting: sailboat heathen',
                        'RUNDECK:DATA:simple digital salad : = ::djkjdf= ',
                ]                                                                |
                [:]
    }

    @Unroll
    def "only filter NORMAL log level"() {
        given:
        def plugin = new SimpleDataFilterPlugin()
        plugin.regex = SimpleDataFilterPlugin.PATTERN
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        events << Mock(LogEventControl) {
            getMessage() >> 'RUNDECK:DATA:a=b'
            getEventType() >> 'log'
            getLoglevel() >> level
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)
        then:

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() ==
                (expect ? ['data': expect] : null)


        where:
        level            | expect
        LogLevel.ERROR   | null
        LogLevel.WARN    | null
        LogLevel.NORMAL  | [a: 'b']
        LogLevel.DEBUG   | null
        LogLevel.VERBOSE | null
    }

    @Unroll
    def "named capture test"() {
        given:
        def plugin = new SimpleDataFilterPlugin()
        plugin.regex = regex
        plugin.logData = dolog
        plugin.name = name
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

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == (expect ? ['data': expect] : null)
        if (expect) {
            if (dolog) {
                1 * context.log(2, _, _)
            } else {
                0 * context.log(*_)
            }
        }


        where:
        dolog | regex                    | name | lines                           | expect
        true  | '^RUNDECK:DATA:(.+?)$'   | 'wimple' | ['RUNDECK:DATA:zangief'] | [wimple: 'zangief']
        false | '^RUNDECK:DATA:(.+?)$'   | 'wimple' | ['RUNDECK:DATA:zangief'] | [wimple: 'zangief']
        true  | '^RUNDECK:DATA:(.+?)$'   | 'wimple' | ['blah', 'blee']         | [:]
        true  | '^RUNDECK:DATA:(.+?)$'   | 'wimple' | ['RUNDECK:DATA:']        | [:]
        true  | '^RUNDECK:DATA:(.+?)$'   | 'wimple' |
                [
                        'RUNDECK:DATA:bogarting: sailboat heathen',
                        'RUNDECK:DATA:::djkjdf= ',
                ]                                                                |
                [
                        'wimple': '::djkjdf= '
                ]
    }

    @Unroll
    def "named capture test invalid key characters"() {
        given:
        def plugin = new SimpleDataFilterPlugin()
        plugin.regex = regex
        plugin.logData = false
        plugin.name = name
        plugin.invalidKeyPattern = validKeyPattern
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

        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == (expect ? ['data': expect] : null)
        if (expect) {
            if(doWarn){
                1 * context.log(1, _)
            }else{
                0 * context.log(1, _)
            }
        }

        where:
        doWarn | validKeyPattern | regex                    | name      | lines                           | expect
        true   | '\\s|\\$|\\{|\\}|\\\\' | '^RUNDECK:DATA:(.+?)$'   | ' wimple' | ['RUNDECK:DATA:zangief'] | [_wimple: 'zangief']
        true   | '\\s|\\$|\\{|\\}|\\\\' | '^RUNDECK:DATA:(.+?)$'   | '${wimple}' | ['RUNDECK:DATA:zangief'] | [__wimple_: 'zangief']
        true   | '\\s|\\$|\\{|\\}|\\\\' | '^RUNDECK:DATA:(.+?)$'   | '\\wimple\\' | ['RUNDECK:DATA:zangief'] | [_wimple_: 'zangief']
        true   | '[^a-zA-Z0-9]'  |'^RUNDECK:DATA:(.+?)$'    | 'wimple?' | ['RUNDECK:DATA:zangief'] | [wimple_: 'zangief']
        false  | ''              |'^RUNDECK:DATA:(.+?)$'    | 'wimple'  | ['RUNDECK:DATA:zangief'] | [wimple: 'zangief']
        true   | '\\s|\\$|\\{|\\}|\\\\'           | '.*:(.+?)\\s*=\\s*(.+)$'   | null | ['Incident: Number= 1235'] | [_Number: '1235']
        true   | '[^a-zA-Z0-9]'  | '.*:(.+?)\\s*=\\s*(.+)$'   | null | ['Incident:Number$= 1235'] | [Number_: '1235']
        false  | ''              | '.*:(.+?)\\s*=\\s*(.+)$'   | null | ['Incident:Number= 1235'] | [Number: '1235']


    }
}
