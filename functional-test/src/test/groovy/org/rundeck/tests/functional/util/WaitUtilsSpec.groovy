package org.rundeck.tests.functional.util

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.ResourceAcceptanceTimeoutException
import org.rundeck.util.common.WaitUtils
import spock.lang.Specification

import java.time.Duration

@APITest
class WaitUtilsSpec extends Specification {

    static final Map<String, String> TEST_VALUES = ["1": "one", "2": "two"].asImmutable()

    def testWaitForThatNeverWaits() {
        given:
        Closure<String> retriever = { return TEST_VALUES[it] }

        when:
        String result = WaitUtils.waitForResource("1",
                retriever,
                {  true },
                {""},
                Duration.ofMillis(0) )

        then:
        result == "one"
    }

    def testWaitForThatWaitsAndFailsToGetAccepted() {
        given:
        Closure<String> retriever = { return "123" }

        Closure<Boolean> resourceAcceptanceEvaluator = { false }

        Closure<String> acceptanceFailureOutputProducer = { String key ->
            return "Custom output $key".toString()
        }

        when:
        WaitUtils.waitForResource("1", retriever, resourceAcceptanceEvaluator, acceptanceFailureOutputProducer)

        then:
        ResourceAcceptanceTimeoutException e = thrown(ResourceAcceptanceTimeoutException)
        assert e.getMessage().contains("Custom output 1")
    }

    def testWaitForThatWaitsAndGetsAccepted() {
        given:
        int tryCounter = 0

        Closure<String> retriever = { String key ->
            if (tryCounter >= 2) {
                return TEST_VALUES[key].toString()
            }
            tryCounter++
            return null as String
        }

        Closure<Boolean> resourceAcceptanceEvaluator = { String k -> k != null }

        when:
        String result = WaitUtils.waitForResource("1", retriever, resourceAcceptanceEvaluator)

        then:
        result == "one"
    }

    def testWaitForManyThatNeverWaits() {
        given:
        Collection<String> ids = ["1", "2"]

        when:
        Map<String, String> result = WaitUtils.waitForAllResources(ids, { String k -> return TEST_VALUES[k] })

        then:
        result == new HashMap(["1": "one", "2": "two"])
    }

    def testWaitForManyThatWaitsAndFailsOnOneOfTheValues() {
        given:
        Collection<String> ids = ["1", "2"]

        when:
        WaitUtils.waitForAllResources(ids,
                { String k -> k == "2" ? null : TEST_VALUES[k] },
                { it != null }
        )

        then:
        RuntimeException e = thrown(RuntimeException)
        assert e.getMessage().contains("Timeout")
    }
}
