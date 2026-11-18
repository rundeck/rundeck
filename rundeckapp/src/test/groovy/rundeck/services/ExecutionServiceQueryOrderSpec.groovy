package rundeck.services

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Verifies the ordering applied by {@link ExecutionService#executionOrder} against real criteria queries.
 */
class ExecutionServiceQueryOrderSpec extends Specification implements DataTest {

    def setupSpec() { mockDomains Execution, Workflow, CommandExec, ScheduledExecution }

    /** Saved executions keyed by label, created in id order a, b, c, d. */
    Map<String, Execution> execs = [:]

    def setup() {
        def base = 1_700_000_000_000L
        execs.a = seed('carol', base + 3000)
        execs.b = seed('alice', base + 1000)
        execs.c = seed('bob', base + 2000)
        execs.d = seed('alice', base + 1000)
    }

    private static Execution seed(String user, long completed) {
        new Execution(
            project      : 'proj',
            user         : user,
            status       : 'succeeded',
            dateStarted  : new Date(completed - 500),
            dateCompleted: new Date(completed),
            workflow     : new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo hi')])
        ).save(flush: true, failOnError: true)
    }

    /** Runs the ordering as queryExecutions does, with optional paging. */
    private static List<Long> orderedIds(ExecutionQuery query, int offset = 0, int max = -1) {
        Execution.createCriteria().list {
            if (offset) {
                firstResult(offset)
            }
            if (max > 0) {
                maxResults(max)
            }
            Closure orderClos = ExecutionService.executionOrder(query)
            orderClos.delegate = delegate
            orderClos()
        }*.id
    }

    private List<Long> ids(String labels) {
        labels.collect { execs[it].id }
    }

    @Unroll
    def "sortBy #sortBy #sortOrder orders by the field with id as tie-breaker"() {
        expect:
        orderedIds(new ExecutionQuery(sortBy: sortBy, sortOrder: sortOrder)) == ids(expected)

        where:
        sortBy          | sortOrder    | expected
        'id'            | 'ascending'  | 'abcd'
        'id'            | 'descending' | 'dcba'
        'dateCompleted' | 'ascending'  | 'bdca'
        'dateCompleted' | 'descending' | 'acdb'
        'user'          | 'ascending'  | 'bdca'
        'user'          | 'descending' | 'acdb'
    }

    @Unroll
    def "unsupported or absent sortBy '#sortBy' keeps the default completion-date order"() {
        expect:
        orderedIds(new ExecutionQuery(sortBy: sortBy, sortOrder: 'ascending')) == ids('acdb')

        where:
        sortBy << [null, '', 'description', 'duration', 'dateCompleted; drop table']
    }

    def "the id tie-breaker keeps page boundaries stable"() {
        given:
        def query = new ExecutionQuery(sortBy: 'user', sortOrder: 'ascending')

        expect:
        orderedIds(query, 0, 2) + orderedIds(query, 2, 2) == ids('bdca')
    }
}
