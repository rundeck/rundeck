package rundeck

import spock.lang.Specification

/**
 * Created by greg on 10/21/15.
 */
class ScheduledExecutionSpec extends Specification {
    def "has nodes selected by default"() {
        given:
        def se = new ScheduledExecution(nodesSelectedByDefault: value)

        when:
        def result = se.hasNodesSelectedByDefault()

        then:
        result == expected

        where:
        value | expected
        null  | true
        true  | true
        false | false
    }

    def "evaluate timeout duration"() {
        expect:
        result == ScheduledExecution.evaluateTimeoutDuration(value)

        where:
        value | result
        "1s"  | 1L
        "1m"  | 60
        "1h"  | 3600
        "1d"  | 24 * 3600
    }

    def "evaluate timeout duration multiple unit"() {
        expect:
        result == ScheduledExecution.evaluateTimeoutDuration(value)

        where:
        value       | result
        "1d1h1m1s"  | 24 * 3600 + 3600 + 60 + 1L
        "1d1h1m2s"  | 24 * 3600 + 3600 + 60 + 2
        "1d1h17m1s" | 24 * 3600 + 3600 + (17 * 60) + 1
        "1d9h1m1s"  | 24 * 3600 + (9 * 3600) + (1 * 60) + 1
        "3d1h1m1s"  | (3 * 24 * 3600) + (1 * 3600) + (1 * 60) + 1
    }

    def "evaluate timeout duration ignored text"() {
        expect:
        result == ScheduledExecution.evaluateTimeoutDuration(value)

        where:
        value                     | result
        "1d 1h 1m 1s"             | 24 * 3600 + 3600 + 60 + 1L
        "1d 1h 1m 2s"             | 24 * 3600 + 3600 + 60 + 2
        "1d 1h 17m 1s"            | 24 * 3600 + 3600 + (17 * 60) + 1
        "1d 9h 1m 1s"             | 24 * 3600 + (9 * 3600) + (1 * 60) + 1
        "3d 1h 1m 1s"             | (3 * 24 * 3600) + (1 * 3600) + (1 * 60) + 1
        "3d 12h #usual wait time" | (3 * 24 * 3600) + (12 * 3600) + (0 * 60) + 0
    }

    def "evaluate timeout duration default seconds"() {
        expect:
        result == ScheduledExecution.evaluateTimeoutDuration(value)

        where:
        value     | result
        "0"       | 0L
        "123"     | 123
        "10000"   | 10000
        "5858929" | 5858929
    }

    def "evaluate timeout duration invalid"() {
        expect:
        result == ScheduledExecution.evaluateTimeoutDuration(value)

        where:
        value  | result
        "asdf" | 0L
        "123z" | 0
    }
}
