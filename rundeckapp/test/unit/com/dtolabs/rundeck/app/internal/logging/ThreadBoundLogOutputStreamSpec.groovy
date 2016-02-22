package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import spock.lang.Specification

/**
 * Created by greg on 2/18/16.
 */
class ThreadBoundLogOutputStreamSpec extends Specification {
    def "write without newline"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)


        when:
        stream.write('abc no newline'.bytes)

        then:
        0 * writer.addEvent(_)
        1 * context.getContext()

    }

    def "write multiple without newline"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)


        when:
        stream.write('abc no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)

        then:
        0 * writer.addEvent(_)
        1 * context.getContext()

    }

    def "write multiple with flush without newline"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)


        when:
        stream.write('abc no newline'.bytes)
        stream.flush()
        stream.write(' still not'.bytes)
        stream.flush()
        stream.write(' more not'.bytes)
        stream.flush()

        then:
        0 * writer.addEvent(_)
        1 * context.getContext()

    }

    def "write with newline"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)

        when:
        stream.write('abc yes newline\n'.bytes)

        then:
        1 * writer.addEvent(_)
        1 * context.getContext()
    }

    def "write multi then newline"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)

        when:
        stream.write('no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)
        stream.write(' then\n'.bytes)

        then:
        1 * writer.addEvent(_)
        1 * context.getContext()
    }

    def "write without newline then close"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)

        when:
        stream.write('no newline'.bytes)
        stream.close()

        then:
        1 * writer.addEvent(_)
        1 * context.getContext()
    }

    def "write multi without newline then close"() {
        given:
        StreamingLogWriter writer = Mock(StreamingLogWriter)
        Contextual context = Mock(Contextual)
        ThreadBoundLogOutputStream stream = new ThreadBoundLogOutputStream(writer, LogLevel.DEBUG, context)

        when:
        stream.write('no newline'.bytes)
        stream.write(' still not'.bytes)
        stream.write(' more not'.bytes)
        stream.close()

        then:
        1 * writer.addEvent(_)
        1 * context.getContext()
    }

}
