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

import com.dtolabs.rundeck.app.internal.logging.DefaultLogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import spock.lang.Specification
import spock.lang.Unroll

class StepLabellingStreamingLogWriterSpec extends Specification {
    @Unroll
    def "add label stepctx #stepctx"() {
        given:
        def writer = Mock(StreamingLogWriter)
        def sut = new StepLabellingStreamingLogWriter(writer, labels)

        when:
        sut.addEvent(
            new DefaultLogEvent(
                loglevel: LogLevel.NORMAL, datetime: new Date(), message: 'blah', eventType: 'log',
                metadata: [
                    a      : 'b',
                    stepctx: stepctx,
                ]
            )
        )

        then:
        1 * writer.addEvent(
            {
                it.metadata['a'] == 'b' &&
                it.metadata['step.label'] == expect
            }
        )


        where:
        stepctx      | labels     | expect
        '1'          | [:]        | null
        '1'          | ['1': 'z'] | 'z'
        '2'          | ['1': 'z'] | null
        '2'          | ['2': 'w'] | 'w'
        '1/2'        | ['2': 'x'] | null
        '1/2'        | ['1': 'x'] | 'x'
        '1@node=z/2' | ['1': 'x'] | 'x'
        'invalid'    | ['1': 'x'] | null
        '  '         | ['1': 'x'] | null
        null         | ['1': 'x'] | null

    }
}
