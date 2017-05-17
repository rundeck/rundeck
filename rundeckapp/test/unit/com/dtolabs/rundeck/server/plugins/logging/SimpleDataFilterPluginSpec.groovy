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
        def sharedoutput = new DataOutput(ContextView.global())
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
        }
        def events = []
        lines.each { line ->
            events << Mock(LogEventControl) {
                getMessage() >> line
                getEventType() >> 'log'
            }
        }
        when:
        plugin.init(context)
        events.each {
            plugin.handleEvent(context, it)
        }
        plugin.complete(context)
        then:
        if (expect) {
            sharedoutput.getSharedContext().getData(ContextView.global()) == ['data': expect]
        } else {
            sharedoutput.getSharedContext().getData(ContextView.global()) == [:]
        }


        where:
        lines                                                 | expect
        ['blah', 'blee']                                      | [:]
        ['RUNDECK:DATA:wimple=']                              | [:]
        ['RUNDECK:DATA:=asdf']                                | [:]
        ['RUNDECK:DATA:wimple=zangief']                       | [wimple: 'zangief']
        ['RUNDECK:DATA:awayward wingling samzait = bogarting: sailboat heathen',
         'RUNDECK:DATA:simple digital salad : = ::djkjdf= ',] | ['awayward wingling samzait': 'bogarting: sailboat ' +
                'heathen',
                                                                 'simple digital salad :'   : '::djkjdf= ']
    }
}
