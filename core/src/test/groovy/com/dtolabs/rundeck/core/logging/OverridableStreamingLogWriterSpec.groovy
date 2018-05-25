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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 10/25/17
 */
class OverridableStreamingLogWriterSpec extends Specification {
    def "test basic addEvent"() {
        given:
        def writer = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)
        LogEvent event = LogUtil.logDebug("a test")


        when:
        override.addEvent(event)

        then:
        1 * writer.addEvent(event)
    }

    @Unroll
    def "test basic #action"() {
        given:
        def writer = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)


        when:
        override."$action"()

        then:
        1 * writer."$action"()
        0 * writer._(*_)

        where:
        action       | _
        'openStream' | _
        'close'      | _
    }

    def "test override"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)
        LogEvent event = LogUtil.logDebug("a test")


        when:
        override.setOverride(writer2)
        override.addEvent(event)

        then:
        0 * writer.addEvent(event)
        1 * writer2.addEvent(event)
    }

    def "test removeOverride"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)
        LogEvent event = LogUtil.logDebug("a test")

        override.setOverride(writer2)

        when:
        def a = override.removeOverride()
        override.addEvent(event)

        then:
        a == writer2
        1 * writer.addEvent(event)
        0 * writer2.addEvent(event)
    }

    def "test getOverride"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)
        override.setOverride(writer)

        when:
        def o = override.getOverride()

        then:
        o == writer
    }

    def "test stacking overrides"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def writer3 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)

        override.setOverride(writer1)
        override.setOverride(writer2)
        override.setOverride(writer3)


        when:
        def a = override.removeOverride()
        def b = override.removeOverride()
        def c = override.removeOverride()
        def d = override.getOverride()

        then:
        a == writer3
        b == writer2
        c == writer1
        d == null
    }

    def "test stacking overrides with null"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def writer3 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)

        override.setOverride(writer1)
        override.pushEmpty()
        override.setOverride(writer2)


        when:
        def a = override.removeOverride()
        def b = override.removeOverride()
        def c = override.removeOverride()
        def d = override.getOverride()

        then:
        a == writer2
        b == null
        c == writer1
        d == null
    }

    def "test override and remove"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)
        LogEvent event = LogUtil.logDebug("a test")


        when:
        override.setOverride(writer2)
        override.removeOverride()
        override.addEvent(event)

        then:
        1 * writer.addEvent(event)
        0 * writer2.addEvent(event)
    }

    def "test stack push with thread"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def writer3 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)

        override.setOverride(writer1)
        override.setOverride(writer2)

        when:
        def a
        def t = new Thread({
            override.setOverride(writer3)
            a = override.getOverride()
        }
        )
        t.start()
        t.join()
        def b = override.getOverride()

        then:
        a == writer3
        b == writer2
    }

    def "test stack pop with thread"() {

        given:
        def writer = Mock(StreamingLogWriter)
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)
        def override = new OverridableStreamingLogWriter(writer)

        override.setOverride(writer1)
        override.setOverride(writer2)

        when:
        def a
        def b
        def t = new Thread({
            a = override.removeOverride()
            b = override.getOverride()
        }
        )
        t.start()
        t.join()
        def c = override.getOverride()

        then:
        a == writer2
        b == writer1
        c == writer2
    }
}
