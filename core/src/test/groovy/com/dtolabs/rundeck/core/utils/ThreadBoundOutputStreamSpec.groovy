package com.dtolabs.rundeck.core.utils

import spock.lang.Specification

class ThreadBoundOutputStreamSpec extends Specification {
    def "empty close with allow"() {
        given: "stream has no thread bound output stream"
        def sink = Mock(OutputStream)
        def stream = new ThreadBoundOutputStream(sink)
        when: "close is called"
        stream.close()
        then: "sink output stream is closed"
        1 * sink.close()
    }

    def "bound close will close bound outputstream"() {
        given: "stream has a thread bound output stream"
        def sink = Mock(OutputStream)
        def os2 = Mock(OutputStream)
        def stream = new ThreadBoundOutputStream(sink)
        stream.installThreadStream(os2)
        when: "close is called"
        stream.close()
        then: "sink output stream is closed"
        1 * os2.close()
    }

    def "removeThreadStream"() {
        given: "stream has a thread bound output stream"
        def sink = Mock(OutputStream)
        def os2 = Mock(OutputStream)
        def stream = new ThreadBoundOutputStream(sink)
        stream.installThreadStream(os2)
        when: "getThreadStream"
        def res1 = stream.getThreadStream()
        then: "bound stream is returned"
        res1 == os2
        when: "remove is called"
        def res = stream.removeThreadStream()
        then: "bound stream is returned"
        res == os2
        when: "getThreadStream"
        def res2 = stream.getThreadStream()
        then: "sink stream is returned"
        res2 == sink
    }
}
