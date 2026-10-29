package rundeck

import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.execution.ExecutionTypes
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

/**
 * {@link ExecutionQuery#countExecutions()} builds HQL by hand, separately from the criteria the
 * listing uses, so the paging total and the rows listed can drift apart. This runs against a real
 * datastore because DataTest's GORM rejects string-based queries, which is why the unit spec
 * covers the criteria path only.
 */
@Integration
@Rollback
class ExecutionQueryExcludeTypeIntegrationSpec extends Specification {

    static final String PROJECT = 'test-exclude-type-integration'

    private Execution seed(String executionType) {
        new Execution(
            project      : PROJECT,
            user         : 'alice',
            status       : 'succeeded',
            dateStarted  : new Date(),
            dateCompleted: new Date(),
            executionType: executionType,
            workflow     : new Workflow(commands: [new CommandExec(adhocRemoteString: 'echo hi')])
        ).save(flush: true, failOnError: true)
    }

    private List runCriteria(ExecutionQuery query) {
        def clos = { isCount ->
            def queryCriteria = query.createCriteria(delegate, null)
            queryCriteria()
        }
        return Execution.createCriteria().list(clos.curry(false))
    }

    def "the count leaves out the excluded type and keeps a null type"() {
        given:
        seed(ExecutionTypes.ADHOC_STEP)
        seed(ExecutionTypes.SCHEDULED)
        seed(null)

        def unfiltered = new ExecutionQuery(projFilter: PROJECT)
        def filtered = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP]
        )

        expect:
        unfiltered.countExecutions() == 3
        filtered.countExecutions() == 2

        and: 'the paging total must agree with the rows the listing returns'
        filtered.countExecutions() == runCriteria(filtered).size()
        unfiltered.countExecutions() == runCriteria(unfiltered).size()
    }

    def "the count keeps rows written before the execution type column existed"() {
        given: 'only null-typed rows'
        seed(null)
        seed(null)

        def filtered = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP]
        )

        expect: 'a plain inequality in SQL would count zero here'
        filtered.countExecutions() == 2
    }
}
