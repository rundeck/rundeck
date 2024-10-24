package com.dtolabs.utils

import com.dtolabs.rundeck.core.utils.ResourceAcceptanceTimeoutException
import com.dtolabs.rundeck.core.utils.WaitUtils
import spock.lang.Specification

import java.time.Duration

class WaitUtilsSpec extends Specification {

    def testWaitForThatNeverAccepts() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return 0
        }

        when:
        WaitUtils.waitFor(retriever, { !!it}, Duration.ofMillis(500), Duration.ofMillis(110) )

        then:
        def e = thrown(ResourceAcceptanceTimeoutException)
        e.message == "Timeout reached (500ms) waiting for value: 0 to reach the desired state"
        closure_loop_counter == 5
    }

    def testWaitForThatAcceptsRightAway() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return 1
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it}, Duration.ofSeconds(1), Duration.ofMillis(100) )

        then:
        result == 1
        closure_loop_counter == 1
    }

    def testWaitForThatAcceptsEventually() {
        given:

        int closure_loop_counter = 0
        def vals = [null, 1]
        Closure<Integer> retriever = {  ->
            closure_loop_counter++
            return vals[closure_loop_counter - 1]
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it}, Duration.ofSeconds(1), Duration.ofMillis(100) )

        then:
        result == 1
        closure_loop_counter == 2
    }
}
