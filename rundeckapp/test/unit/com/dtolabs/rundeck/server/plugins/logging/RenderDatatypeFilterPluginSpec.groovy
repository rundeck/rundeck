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
import spock.lang.Specification

/**
 * @author greg
 * @since 5/26/17
 */
class RenderDatatypeFilterPluginSpec extends Specification {

    def "test"() {
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
}
