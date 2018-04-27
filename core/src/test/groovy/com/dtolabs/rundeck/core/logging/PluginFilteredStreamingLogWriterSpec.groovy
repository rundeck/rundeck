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

package com.dtolabs.rundeck.core.logging

import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 6/2/17
 */
class PluginFilteredStreamingLogWriterSpec extends Specification {
    def "pass through with no filters"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)

        def event = LogUtil.event(type, level, message, meta)
        when:
        writer.addEvent(event)

        then:
        1 * sink.addEvent(event)


        where:
        type << ['log', 'notlog', 'stepbegin', 'nodebegin', 'any', 'other']
        level << LogLevel.values()
        message = 'blah'
        meta = [:]

    }

    static class MessageFilterPlugin implements LogFilterPlugin {
        String message;
        Map metadata
        String metakey
        String metavalue

        @Override
        void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
            if (message != null) {
                event.message = message
            }
            if (metadata != null) {
                event.addMetadata(metadata)
            }
            if (metakey && metavalue) {
                event.addMetadata(metakey, metavalue)
            }
        }
    }

    @Unroll
    def "plugin alters log messages only"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)

        def event = LogUtil.event(type, level, message, meta)

        def plugin = new MessageFilterPlugin(message: 'test')

        writer.addPlugin(plugin)

        when:
        writer.addEvent(event)

        then:
        if (type == 'log') {
            1 * sink.addEvent({ it.message == 'test' })
        } else {
            1 * sink.addEvent(event)
        }

        where:
        type << ['log', 'notlog', 'stepbegin', 'nodebegin', 'any', 'other']
        level << LogLevel.values()
        message = 'blah'
        meta = [:]

    }

    @Unroll
    def "event control emits modified event"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)

        def event = LogUtil.event('log', LogLevel.NORMAL, message, ['old': 'data'])

        def plugin = new MessageFilterPlugin(
            message: 'origmessage',
            metadata: meta,
            metakey: metakey,
            metavalue: metavalue
        )

        writer.addPlugin(plugin)

        when:
        writer.addEvent(event)

        then:
        1 * sink.addEvent({ it != event })

        where:
        message       | meta     | metakey | metavalue
        'newmessage'  | null     | null    | null
        'origmessage' | [a: 'b'] | null    | null
        'origmessage' | null     | 'a'     | 'b'
    }

    @Unroll
    def "event control with null orginal metadata"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)

        def event = LogUtil.event('log', LogLevel.NORMAL, 'a message', null)

        def plugin = new MessageFilterPlugin(metadata: meta,)

        writer.addPlugin(plugin)

        when:
        writer.addEvent(event)

        then:
        1 * sink.addEvent({ it.metadata == meta })

        where:
        meta     | _
        [a: 'b'] | _
    }

    @Unroll
    def "plugin event control action #action"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)

        def event = LogUtil.event(type, level, message, meta)

        LogFilterPlugin plugin1 = [
                handleEvent: { PluginLoggingContext context1, LogEventControl event1 ->
                    event1."$action"()
                },
                init       : {}
        ] as LogFilterPlugin

        def plugin2 = Mock(LogFilterPlugin)
        //new MessageFilterPlugin(message: 'test')

        writer.addPlugin(plugin1)
        writer.addPlugin(plugin2)

        when:
        writer.addEvent(event)

        then:
        if (action == 'quell') {
            1 * plugin2.handleEvent(_, { it.message == 'blah' })
            0 * sink.addEvent(_)
        } else if (action == 'remove') {
            0 * plugin2.handleEvent(_, { it.message == 'blah' })
            0 * sink.addEvent(_)
        } else if (action == 'emit') {
            1 * plugin2.handleEvent(_, { it.message == 'blah' })
            1 * sink.addEvent(_)
        } else if (action == 'quiet') {
            1 * plugin2.handleEvent(_, { it.message == 'blah' && it.loglevel == LogLevel.NORMAL })
            1 * sink.addEvent({ it.loglevel == LogLevel.VERBOSE })
        }

        where:
        action << ['quell', 'emit', 'remove', 'quiet']
        type = 'log'
        level = LogLevel.NORMAL
        message = 'blah'
        meta = [:]

    }

    @Unroll
    def "add inits each plugin"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)


        LogFilterPlugin plugin1 = Mock(LogFilterPlugin)
        LogFilterPlugin plugin2 = Mock(LogFilterPlugin)
        LogFilterPlugin plugin3 = Mock(LogFilterPlugin)

        when:
        writer.addPlugin(plugin1)
        writer.addPlugin(plugin2)
        writer.addPlugin(plugin3)

        then:
        1 * plugin1.init(!null)
        1 * plugin2.init(!null)
        1 * plugin3.init(!null)

    }

    @Unroll
    def "close completes all plugins"() {
        given:
        def sink = Mock(StreamingLogWriter)
        def context = Mock(ExecutionContext)
        def logger = Mock(ExecutionLogger)
        def writer = new PluginFilteredStreamingLogWriter(sink, context, logger)


        LogFilterPlugin plugin1 = Mock(LogFilterPlugin)
        LogFilterPlugin plugin2 = Mock(LogFilterPlugin)
        LogFilterPlugin plugin3 = Mock(LogFilterPlugin)


        writer.addPlugin(plugin1)
        writer.addPlugin(plugin2)
        writer.addPlugin(plugin3)

        when:
        writer.close()

        then:
        1 * plugin1.complete(!null)
        1 * plugin2.complete(!null)
        1 * plugin3.complete(!null)

    }
}
