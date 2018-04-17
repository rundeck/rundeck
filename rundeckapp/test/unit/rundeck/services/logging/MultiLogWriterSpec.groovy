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

package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 10/25/17
 */
class MultiLogWriterSpec extends Specification {
    def "multi add event"() {
        given:
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)


        def writers = [
                writer1,
                writer2
        ]
        def mlw = new MultiLogWriter(writers)
        def event = LogUtil.logDebug("a message")
        when:
        mlw.addEvent(event)
        then:
        1 * writer1.addEvent(event)
        1 * writer2.addEvent(event)
    }

    @Unroll
    def "multi action #action"() {
        given:
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)


        def writers = [
                writer1,
                writer2
        ]
        def mlw = new MultiLogWriter(writers)
        def event = LogUtil.logDebug("a message")
        when:
        mlw.openStream()
        then:
        1 * writer1."$action"()
        0 * writer1._(*_)
        1 * writer2."$action"()
        0 * writer2._(*_)

        where:
        action       | _
        'openStream' | _
    }

    def "no event after close"() {
        given:
        def writer1 = Mock(StreamingLogWriter)
        def writer2 = Mock(StreamingLogWriter)


        def writers = [
                writer1,
                writer2
        ]
        def mlw = new MultiLogWriter(writers)
        def event = LogUtil.logDebug("a message")
        def event2 = LogUtil.logDebug("a message2")
        when:
        mlw.addEvent(event)
        mlw.close()
        mlw.addEvent(event2)
        then:
        1 * writer1.addEvent(event)
        1 * writer1.close()
        1 * writer2.addEvent(event)
        1 * writer2.close()
    }
}
