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
}
