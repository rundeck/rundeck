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
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/17/17
 */
class KeyValueDataLogFilterPluginSpec extends Specification {
    @Unroll
    def "test"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
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
            true  | KeyValueDataLogFilterPlugin.PATTERN  | ['.*']                               | [:]
            true  | KeyValueDataLogFilterPlugin.PATTERN  | ['RUNDECK:DATA:wimple=zangief']      | [wimple: 'zangief']
            false | KeyValueDataLogFilterPlugin.PATTERN  | ['RUNDECK:DATA:wimple=zangief']      | [wimple: 'zangief']
            true  | KeyValueDataLogFilterPlugin.PATTERN  | ['blah', 'blee']                     | [:]
            true  | KeyValueDataLogFilterPlugin.PATTERN  | ['RUNDECK:DATA:wimple=']             | [:]
            true  | KeyValueDataLogFilterPlugin.PATTERN  | ['RUNDECK:DATA:=asdf']               | [:]
            false  | KeyValueDataLogFilterPlugin.PATTERN |
            [
                        'RUNDECK:DATA:awayward wingling samzait = bogarting: sailboat heathen',
                        'RUNDECK:DATA:simple digital salad : = ::djkjdf= ',
                ]                                                                               |
            [:]
    }

    @Unroll
    def "The plugin supports empty values"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
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
        1 * context.log(2, _, _)

        where:
        dolog | regex                            | lines                           | expect
        true  | '^\\s*([^\\s]+?)\\s*=\\s*(.*)$'  | ['key=']                        | [key:'']

    }

    @Unroll
    def "only filter NORMAL log level"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
        plugin.regex = KeyValueDataLogFilterPlugin.PATTERN
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
        def plugin = new KeyValueDataLogFilterPlugin()
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
        def plugin = new KeyValueDataLogFilterPlugin()
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

    @Unroll
    def "invalid characters replaced by custom character"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
        plugin.regex = regex
        plugin.logData = false
        plugin.name = name
        plugin.invalidKeyPattern = validKeyPattern
        plugin.replaceFilteredResult = true
        plugin.invalidCharactersReplacement = invalidStringReplacement
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
        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() == ['data': expect]

        1 * context.log(1, _)

        where:
        validKeyPattern        | regex                    | invalidStringReplacement | name      | lines                           | expect
        '\\s|\\$|\\{|\\}|\\\\' | '^RUNDECK:DATA:(.+?)$'   | ''                       | ' ubuntu' | ['RUNDECK:DATA:zangief']        | [ubuntu: 'zangief']
        '\\s|\\$|\\{|\\}|\\\\' | '^RUNDECK:DATA:(.+?)$'   | 'Football'               | ' wimple' | ['RUNDECK:DATA:zangief']        | [Footballwimple: 'zangief']

    }

    @Unroll
    def "Test good parameters"() {

        KeyValueDataLogFilterPlugin.NamePropertyValidator validator = new KeyValueDataLogFilterPlugin.NamePropertyValidator()

        expect:
        validator.isValid(regexValue, props)

        where:
        regexValue                             |            props              | expectedResult
        '^RUNDECK:DATA:\\s*([^\\s]+?)\\s*=\\s*(.+)\$'         | [name: "TestValue"]           | true
        '(.*)'                                                | [name: "TestValue"]           | true
    }

    @Unroll
    def "Test failing parameters"() {

        KeyValueDataLogFilterPlugin.NamePropertyValidator validator = new KeyValueDataLogFilterPlugin.NamePropertyValidator()

        when:
        validator.isValid(regexValue, props)

        then:
        def error = thrown(expectedException)
        error.message == expectedMessage

        where:
        regexValue   |            props              | expectedException      |     expectedMessage
            '.*'     | [name: "TestValue"]           | ValidationException    |  'Pattern must have at least one group'
           '(.*)'    | [:]                           | ValidationException    |  'The Name field must be defined when only one capture group is specified'
    }
    @Unroll
    def "matchSubstrings toggle controls partial vs full-line matching"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
        plugin.regex = regex
        plugin.logData = false
        plugin.invalidKeyPattern = null
        plugin.name = "CNUM"                 // single capture group => Name Data required
        plugin.matchSubstrings = matchSubstrings

        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def event = Mock(LogEventControl) {
            getMessage() >> "PHVGBT.C0437390.FIN7.BTOY125"
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }

        when:
        plugin.init(context)
        plugin.handleEvent(context, event)
        plugin.complete(context)

        then:
        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() ==
                (expect ? ['data': expect] : null)

        where:
        matchSubstrings | regex                                         | expect
        // default  mode (matches()): substring-only pattern -> no match
        false           | '^.*\\.[A-Z]([0-9]+)\\.'                      | null
        // substring mode ON (find()): substring-only pattern -> match works
        true            | '^.*\\.[A-Z]([0-9]+)\\.'                      | [CNUM: '0437390']
        // default mode with full-line pattern -> match works
        false           | '^.*\\.[A-Z]([0-9]+)\\..*$'                   | [CNUM: '0437390']
        // substring mode with full-line pattern -> still works
        true            | '^.*\\.[A-Z]([0-9]+)\\..*$'                   | [CNUM: '0437390']
    }
    @Unroll
    def "first character extraction remains stable across modes"() {
        given:
        def plugin = new KeyValueDataLogFilterPlugin()
        plugin.regex = '^(.).*$'
        plugin.logData = false
        plugin.invalidKeyPattern = null
        plugin.name = "FIRSTCHAR"
        plugin.matchSubstrings = matchSubstrings

        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def event = Mock(LogEventControl) {
            getMessage() >> "Hello"
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }

        when:
        plugin.init(context)
        plugin.handleEvent(context, event)
        plugin.complete(context)

        then:
        sharedoutput.getSharedContext().getData(ContextView.global())?.getData() ==
                ['data': [FIRSTCHAR: 'H']]

        where:
        matchSubstrings << [false, true]
    }


}
