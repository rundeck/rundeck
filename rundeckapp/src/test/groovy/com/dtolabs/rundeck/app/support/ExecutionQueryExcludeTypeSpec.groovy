package com.dtolabs.rundeck.app.support

import com.dtolabs.rundeck.core.execution.ExecutionTypes
import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import spock.lang.Specification

/**
 * {@link ExecutionQuery#excludeExecutionTypeFilter} has to be NULL-safe: rows written before the
 * execution_type column existed hold null, and a plain inequality would drop them from every
 * listing. These run the two builders against real rows rather than asserting on generated SQL,
 * because the criteria path and the HQL of countExecutions() are written separately and can drift.
 *
 * The HQL of countExecutions() cannot be exercised here: DataTest's GORM implementation rejects
 * string-based queries. It is covered by ExecutionQueryExcludeTypeIntegrationSpec instead.
 */
class ExecutionQueryExcludeTypeSpec extends Specification implements DataTest {

    static final String PROJECT = 'test-exclude'

    def setupSpec() { mockDomains Execution, Workflow, CommandExec, ScheduledExecution }

    def setup() {
        seed(ExecutionTypes.ADHOC_STEP)
        seed(ExecutionTypes.SCHEDULED)
        seed(null)
    }

    private static Execution seed(String executionType) {
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

    /** Mirrors how ExecutionService.queryExecutions drives the builder. */
    private static List runCriteria(ExecutionQuery query) {
        def clos = { isCount ->
            def queryCriteria = query.createCriteria(delegate, null)
            queryCriteria()
        }
        return Execution.createCriteria().list(clos.curry(false))
    }

    def "criteria: without the filter every execution is listed"() {
        given:
        def query = new ExecutionQuery(projFilter: PROJECT)

        expect:
        runCriteria(query).size() == 3
    }

    def "criteria: the excluded type is dropped but a null type is kept"() {
        given:
        def query = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP]
        )

        when:
        def results = runCriteria(query)

        then: 'rows predating the column must survive the exclusion'
        results.size() == 2
        results*.executionType.toSet() == [ExecutionTypes.SCHEDULED, null].toSet()
    }

    def "criteria: several types can be excluded at once"() {
        given:
        def query = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP, ExecutionTypes.SCHEDULED]
        )

        when:
        def results = runCriteria(query)

        then: 'only the null-typed row is left'
        results.size() == 1
        results[0].executionType == null
    }

    def "an explicit executionTypeFilter still selects the excluded type"() {
        given: 'a caller asking for exactly the type the default would hide'
        def query = new ExecutionQuery(
            projFilter         : PROJECT,
            executionTypeFilter: ExecutionTypes.ADHOC_STEP
        )

        when:
        def results = runCriteria(query)

        then:
        results.size() == 1
        results[0].executionType == ExecutionTypes.ADHOC_STEP
    }


    def "the count cache key distinguishes queries that differ only by the exclusion"() {
        given: 'two queries a cached total must not be shared between'
        def unfiltered = new ExecutionQuery(projFilter: PROJECT)
        def filtered = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP]
        )

        expect: 'otherwise whichever ran first would seed the paging total for both'
        ExecutionService.ExecutionCountCacheKey.fromQuery(unfiltered) !=
            ExecutionService.ExecutionCountCacheKey.fromQuery(filtered)
    }

    def "the count cache key is stable regardless of the order the exclusions are given in"() {
        given:
        def one = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.ADHOC_STEP, ExecutionTypes.USER]
        )
        def other = new ExecutionQuery(
            projFilter                : PROJECT,
            excludeExecutionTypeFilter: [ExecutionTypes.USER, ExecutionTypes.ADHOC_STEP]
        )

        expect:
        ExecutionService.ExecutionCountCacheKey.fromQuery(one) ==
            ExecutionService.ExecutionCountCacheKey.fromQuery(other)
    }
}
