package com.dtolabs.rundeck.core.execution.utils

import spock.lang.Specification

/**
 * Created by greg on 10/6/15.
 */
class BasicSourceSpec extends Specification {
    def "clear"() {
        given:
        def basic = new BasicSource(password)
        when:
        basic.clear()
        then:
        basic.getPassword() == null

        where:

        password         | _
        null             | _
        "abc"            | _
        'xyz'.getBytes() | _
    }
}
