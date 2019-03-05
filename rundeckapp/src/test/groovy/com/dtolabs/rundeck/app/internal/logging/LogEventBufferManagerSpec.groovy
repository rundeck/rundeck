/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import spock.lang.Specification

/**
 * Created by greg on 2/22/16.
 */
class LogEventBufferManagerSpec extends Specification {
    def "create buffer"() {
        given:
        def manager = new LogEventBufferManager(listener: {[abc: 'xyz']})

        when:
        def buffer = manager.create()

        then:
        buffer != null
        !buffer.isEmpty()
        buffer.context == [abc: 'xyz']
        buffer.baos != null
        buffer.baos.size() == 0
        manager.buffers.size() == 1
    }

    def "create multiple buffers"() {
        given:
        def manager = new LogEventBufferManager(listener: {[abc: 'xyz']})

        when:
        def buffer = manager.create()
        def buffer2 = manager.create()
        def buffer3 = manager.create()

        then:
        manager.buffers.size() == 3
    }

    def "flush buffers"() {
        given:
        def manager = new LogEventBufferManager(listener: {[abc: 'xyz']})
        def buffer = manager.create()
        def buffer2 = manager.create()
        def buffer3 = manager.create()
        def writer = Mock(StreamingLogWriter)
        when:
        buffer.baos.write('abc'.bytes)

        manager.flush({writer.addEvent(it)})
        then:
        3 * writer.addEvent(_)
        manager.buffers.isEmpty()
    }

    def "flush buffers after clear"() {
        given:
        def manager = new LogEventBufferManager(listener: {[abc: 'xyz']})
        def buffer = manager.create()
        def buffer2 = manager.create()
        def buffer3 = manager.create()
        def writer = Mock(StreamingLogWriter)
        when:
        buffer.clear()
        buffer2.clear()
        buffer3.clear()
        manager.flush({writer.addEvent(it.get())})
        then:
        0 * writer.addEvent(_)
        manager.buffers.isEmpty()
    }
}
