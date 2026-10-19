package com.dtolabs.utils

import com.dtolabs.rundeck.core.utils.ResourceAcceptanceTimeoutException
import com.dtolabs.rundeck.core.utils.WaitUtils
import spock.lang.Specification

import java.time.Duration

class WaitUtilsSpec extends Specification {

    def testWaitForThatNeverAccepts() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = { ->
            closure_loop_counter++
            return 0
        }

        when:
        WaitUtils.waitFor(retriever, { !!it }, Duration.ofMillis(500), Duration.ofMillis(110))

        then:
        def e = thrown(ResourceAcceptanceTimeoutException)
        e.message == "Timeout reached (500ms) waiting for value: 0 to reach the desired state"
        // The attempt count is wall-clock driven, so it is a range and not a number. Reaching five
        // needs four sleeps (4 x 110ms = 440ms) to finish with the clock still under 500ms: 60ms of
        // slack spread over four sleeps, 15ms each. Thread.sleep overshoots by a few ms on an idle
        // machine and by far more on a loaded CI container, which is where this asserted 5 and got
        // 4. What waitFor actually promises is that it retries, and that it stops once the budget
        // is spent -- five is the ceiling that proves the second part, not a guarantee.
        closure_loop_counter >= 2
        closure_loop_counter <= 5
    }

    def testWaitForWithZeroTimeout() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = { ->
            closure_loop_counter++
            return 0
        }

        when:
        WaitUtils.waitFor(retriever, { !!it }, Duration.ofMillis(0), Duration.ofMillis(0))

        then:
        def e = thrown(ResourceAcceptanceTimeoutException)
        e.message == "Timeout reached (0ms) waiting for value: 0 to reach the desired state"
        closure_loop_counter == 1
    }

    def testWaitForThatAcceptsRightAway() {
        given:

        int closure_loop_counter = 0
        Closure<Integer> retriever = { ->
            closure_loop_counter++
            return 1
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it }, Duration.ofSeconds(1), Duration.ofMillis(100))

        then:
        result == 1
        closure_loop_counter == 1
    }

    def testWaitForThatAcceptsEventually() {
        given:

        int closure_loop_counter = 0
        def vals = [null, 1]
        Closure<Integer> retriever = { ->
            closure_loop_counter++
            return vals[closure_loop_counter - 1]
        }

        when:
        Integer result = WaitUtils.waitFor(retriever, { !!it }, Duration.ofSeconds(1), Duration.ofMillis(100))

        then:
        result == 1
        closure_loop_counter == 2
    }
}
