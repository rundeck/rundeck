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
import com.dtolabs.rundeck.core.logging.LogUtil
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.Charset

/**
 * Created by greg on 2/22/16.
 */
class LogEventBufferSpec extends Specification {

    def "clear buffer is empty"() {
        given:
        def buff = new LogEventBuffer([:])
        when:
        buff.clear()
        then:
        null != buff.baos
        0 == buff.baos.size()
        null == buff.context
        null == buff.time
        !buff.crchar
        buff.isEmpty()
    }

    def "new buffer not empty"() {
        given:
        def buff = new LogEventBuffer([:])
        expect:
        null != buff.baos
        0 == buff.baos.size()
        null != buff.context
        null != buff.time
        !buff.crchar
        !buff.isEmpty()
    }

    def "clear after modify is empty"() {
        given:
        def buff = new LogEventBuffer([:])
        buff.baos.write('abc'.bytes)
        buff.crchar = true
        when:
        buff.clear()
        then:
        null != buff.baos
        0 == buff.baos.size()
        null == buff.context
        null == buff.time
        !buff.crchar
        buff.isEmpty()
    }

    def "create event with data"() {

        def buff = new LogEventBuffer([abc: 'xyz'])
        buff.baos.write('abc'.bytes)

        when:
        def log = buff.createEvent(LogLevel.DEBUG)

        then:
        null != log
        log.message == 'abc'
        log.datetime != null
        log.loglevel == LogLevel.DEBUG
        log.metadata == [abc: 'xyz']
        log.eventType == LogUtil.EVENT_TYPE_LOG

    }

    def "compare dates"() {
        expect:

        result == LogEventBuffer.compareDates(dateA, dateB)

        where:
        dateA         | dateB         | result
        new Date(123) | new Date(125) | -1
        new Date(123) | new Date(122) | 1
        new Date(123) | new Date(123) | 0
        null          | new Date(123) | 1
        new Date(123) | null          | -1
        null          | null          | 0
    }
    static final String multibyteString = 'možnej'
    static final def isobytes = multibyteString.getBytes('ISO-8859-2')

    @Unroll
    def "create event with charset #charset"() {

        given:
        def buff = new LogEventBuffer([:], charset ? Charset.forName(charset) : null)
        buff.baos.write(bytes)

        when:
        def log = buff.createEvent(LogLevel.DEBUG)

        then:
        null != log
        log.message == result
        log.message != badresult

        where:
        charset      | bytes                                  | result                        | badresult
        'ISO-8859-2' | multibyteString.getBytes('ISO-8859-2') |
                multibyteString                                                               |
                new String(isobytes, 'UTF-8')
        'UTF-8'      | multibyteString.getBytes('ISO-8859-2') | new String(isobytes, 'UTF-8') | multibyteString
        null         | multibyteString.getBytes()             |
                multibyteString                                                               |
                new String(isobytes, 'UTF-8')
    }
}
