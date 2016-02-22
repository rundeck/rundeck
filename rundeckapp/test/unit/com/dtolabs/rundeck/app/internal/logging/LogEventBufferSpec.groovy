package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import spock.lang.Specification

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
}
