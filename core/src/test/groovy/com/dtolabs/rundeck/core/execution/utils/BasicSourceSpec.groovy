package com.dtolabs.rundeck.core.execution.utils

import spock.lang.Specification

/**
 * Created by greg on 10/6/15.
 */
class BasicSourceSpec extends Specification {
    def "clear"(String password,_) {
        given:
        def basic = new BasicSource((String)password)
        when:
        basic.clear()
        then:
        basic.getPassword() == null

        where:

        password         | _
        null             | _
        "abc"            | _
    }
}
