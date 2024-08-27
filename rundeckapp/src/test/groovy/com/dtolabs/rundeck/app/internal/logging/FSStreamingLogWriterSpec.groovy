package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.internal.DefaultLogEvent
import com.dtolabs.rundeck.core.logging.internal.OutputLogFormat
import spock.lang.Specification

class FSStreamingLogWriterSpec extends Specification {

    def "test writing logs with default meta"() {
        given:
            def output = new ByteArrayOutputStream()
            def outformat = Mock(OutputLogFormat)
            def writer = new FSStreamingLogWriter(output, [a: 'b'], outformat)

        when:
            writer.openStream()
            writer.addEvent(
                Stub(LogEvent) {
                    getEventType() >> 'log'
                    getLoglevel() >> LogLevel.NORMAL
                    getMessage() >> "msg1"
                    getDatetime() >> new Date()
                    getMetadata() >> [c: 'd']
                }
            )
            writer.addEvent(
                Stub(LogEvent) {
                    getEventType() >> 'log'
                    getLoglevel() >> LogLevel.NORMAL
                    getMessage() >> "msg2"
                    getDatetime() >> new Date()
                    getMetadata() >> [x: 'y']
                }
            )
            writer.close()
            def result = new String(output.toByteArray(), "UTF-8")

        then:
            1 * outformat.outputBegin() >> "begin"
            1 * outformat.outputEvent(
                {
                    it instanceof DefaultLogEvent
                    it.getMessage() == 'msg1'
                    it.getMetadata() == [a: 'b', c: 'd']
                }
            ) >> "event1"
            1 * outformat.outputEvent(
                {
                    it instanceof DefaultLogEvent
                    it.getMessage() == 'msg2'
                    it.getMetadata() == [a: 'b', x: 'y']
                }
            ) >> "event2"
            0 * outformat.outputEvent(*_)
            1 * outformat.outputFinish() >> "finish"


            result == 'begin\nevent1\nevent2\nfinish\n'
    }
}
