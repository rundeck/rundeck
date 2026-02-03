package rundeck.services

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Integration tests for ExecutionService count cache functionality.
 * These tests require a real database to test HQL queries.
 */
@Integration
@Rollback
class ExecutionServiceCountCacheIntegrationSpec extends Specification {

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigurationService configurationService

    def setup() {
        // Clear any existing cache
        if (executionService.executionCountCache != null) {
            executionService.executionCountCache.invalidateAll()
        }
    }

    private List<Execution> createTestExecutions(String project = "TestProject", int count = 5) {
        def executions = []
        
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'testJob',
            project: project,
            groupPath: 'test/group',
            description: 'a test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo hello'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        count.times { i ->
            def exec = new Execution(
                scheduledExecution: job,
                jobUuid: job.uuid,  // Set jobUuid for direct filtering without JOIN
                project: project,
                status: i % 2 == 0 ? "succeeded" : "failed",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user${i % 3}",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
            executions << exec
        }
        
        return executions
    }

    // ==================== countExecutionsSimple Tests ====================

    def "countExecutionsSimple returns correct count for project filter"() {
        given:
        createTestExecutions("ProjectA", 5)
        createTestExecutions("ProjectB", 3)

        when:
        def query = new ExecutionQuery(projFilter: "ProjectA")
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 5
    }

    def "countExecutionsSimple returns correct count for user filter"() {
        given:
        createTestExecutions("TestProject", 6) // users: user0, user1, user2, user0, user1, user2

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", userFilter: "user0")
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 2 // user0 appears at index 0 and 3
    }

    def "countExecutionsSimple returns correct count for status filter succeeded"() {
        given:
        createTestExecutions("TestProject", 5) // statuses: succeeded, failed, succeeded, failed, succeeded

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "succeeded")
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 3 // indices 0, 2, 4 are succeeded
    }

    def "countExecutionsSimple returns correct count for status filter failed"() {
        given:
        createTestExecutions("TestProject", 5) // statuses: succeeded, failed, succeeded, failed, succeeded

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "failed")
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 2 // indices 1, 3 are failed
    }

    def "countExecutionsSimple returns 0 for non-existent project"() {
        given:
        createTestExecutions("ExistingProject", 5)

        when:
        def query = new ExecutionQuery(projFilter: "NonExistentProject")
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 0
    }

    // ==================== Cache Hit/Miss Tests ====================

    def "queryExecutions uses cached count on second call"() {
        given:
        createTestExecutions("TestProject", 5)
        
        // Enable cache
        def originalCacheEnabled = configurationService.getBoolean(
            ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, false
        )
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject")
        
        // First call - cache miss
        def result1 = executionService.queryExecutions(query, 0, 20)
        
        // Get the cache key and check it was cached
        def cacheKey = executionService.buildCountCacheKey(query)
        def cachedValue = executionService.getExecutionCountCache().getIfPresent(cacheKey)

        // Second call - should be cache hit
        def result2 = executionService.queryExecutions(query, 0, 20)

        then:
        result1.total == 5
        result2.total == 5
        cachedValue == 5L

        cleanup:
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, originalCacheEnabled)
    }

    def "queryExecutions does not cache when cache is disabled"() {
        given:
        createTestExecutions("TestProject", 5)
        
        // Disable cache
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, false)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject")
        def result = executionService.queryExecutions(query, 0, 20)
        
        def cacheKey = executionService.buildCountCacheKey(query)
        def cache = executionService.executionCountCache
        def cachedValue = cache?.getIfPresent(cacheKey)

        then:
        result.total == 5
        // Cache should not have the value (either cache is null or value is null)
        (cache == null || cachedValue == null)
    }

    // ==================== Stale Cache Detection Tests ====================

    def "stale cache is detected and refreshed on partial page"() {
        given:
        createTestExecutions("TestProject", 3)
        
        // Enable cache
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, true)
        
        def query = new ExecutionQuery(projFilter: "TestProject")
        def cacheKey = executionService.buildCountCacheKey(query)
        
        // Pre-populate cache with stale (too high) value
        executionService.getExecutionCountCache().put(cacheKey, 100L)

        when:
        // Request with max=20 - will get 3 results but cache says 100
        def result = executionService.queryExecutions(query, 0, 20)
        
        // Check cache was updated
        def newCachedValue = executionService.getExecutionCountCache().getIfPresent(cacheKey)

        then:
        result.result.size() == 3
        result.total == 3 // Should be corrected
        newCachedValue == 3L // Cache should be updated

        cleanup:
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, false)
    }

    def "stale cache is detected and refreshed on empty page"() {
        given:
        createTestExecutions("TestProject", 3)
        
        // Enable cache
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, true)
        
        def query = new ExecutionQuery(projFilter: "TestProject")
        def cacheKey = executionService.buildCountCacheKey(query)
        
        // Pre-populate cache with stale value saying there are 100 items
        executionService.getExecutionCountCache().put(cacheKey, 100L)

        when:
        // Request page at offset 50 - will get 0 results because only 3 exist
        def result = executionService.queryExecutions(query, 50, 20)
        
        // Check cache was updated
        def newCachedValue = executionService.getExecutionCountCache().getIfPresent(cacheKey)

        then:
        result.result.size() == 0
        result.total == 3 // Should be the real count
        newCachedValue == 3L // Cache should be updated

        cleanup:
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, false)
    }

    def "stale cache is detected when cached total is lower than results"() {
        given:
        createTestExecutions("TestProject", 10)
        
        // Enable cache
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, true)
        
        def query = new ExecutionQuery(projFilter: "TestProject")
        def cacheKey = executionService.buildCountCacheKey(query)
        
        // Pre-populate cache with stale (too low) value
        executionService.getExecutionCountCache().put(cacheKey, 5L)

        when:
        // Request will get 10 results but cache says only 5
        def result = executionService.queryExecutions(query, 0, 20)
        
        // Check cache was updated
        def newCachedValue = executionService.getExecutionCountCache().getIfPresent(cacheKey)

        then:
        result.result.size() == 10
        result.total == 10 // Should be corrected
        newCachedValue == 10L // Cache should be updated

        cleanup:
        configurationService.setBoolean(ExecutionService.EXECUTION_COUNT_CACHE_ENABLED, false)
    }

    // ==================== canUseSimpleCount Tests ====================

    def "canUseSimpleCount returns true for simple queries"() {
        expect:
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test")) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", statusFilter: "succeeded")) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", userFilter: "admin")) == true
    }

    def "canUseSimpleCount returns true for job UUID queries"() {
        expect:
        // UUID job IDs can use execution.job_uuid column (no JOIN)
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["550e8400-e29b-41d4-a716-446655440000"])) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["uuid-1", "uuid-2"])) == true
    }

    def "canUseSimpleCount returns false for queries requiring JOIN"() {
        expect:
        // These require JOIN with scheduled_execution
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["12345"])) == false // Long ID
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", groupPath: "path")) == false
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobFilter: "job")) == false
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", adhoc: true)) == false
    }

    def "countExecutionsSimple with job UUID filter"() {
        given:
        def executions = createTestExecutions("TestProject", 5)
        def jobUuid = executions[0].scheduledExecution.uuid

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", jobIdListFilter: [jobUuid])
        def count = executionService.countExecutionsSimple(query)

        then:
        count == 5 // All executions were created with the same job
    }

    // ==================== Cache Key Tests ====================

    def "cache key is consistent for same query"() {
        given:
        def query1 = new ExecutionQuery(projFilter: "Test", statusFilter: "succeeded")
        def query2 = new ExecutionQuery(projFilter: "Test", statusFilter: "succeeded")

        expect:
        executionService.buildCountCacheKey(query1) == executionService.buildCountCacheKey(query2)
    }

    def "cache key is different for different queries"() {
        given:
        def query1 = new ExecutionQuery(projFilter: "ProjectA")
        def query2 = new ExecutionQuery(projFilter: "ProjectB")
        def query3 = new ExecutionQuery(projFilter: "ProjectA", statusFilter: "failed")

        expect:
        executionService.buildCountCacheKey(query1) != executionService.buildCountCacheKey(query2)
        executionService.buildCountCacheKey(query1) != executionService.buildCountCacheKey(query3)
    }

    // ==================== Scheduled Executions Tests ====================

    def "queryExecutions returns scheduled executions"() {
        given:
        // Create a scheduled execution in the future
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 120)
        Date futureDate = cal.getTime()

        ScheduledExecution futureJob = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'futureJob',
            project: 'FutureProject',
            groupPath: 'future',
            description: 'a future job',
            argString: '-a b',
            crontabString: '01 13 1 1 1 2099',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo future'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        Execution scheduledExec = new Execution(
            scheduledExecution: futureJob,
            jobUuid: futureJob.uuid,
            project: "FutureProject",
            status: "scheduled",
            dateStarted: futureDate,
            dateCompleted: null,
            user: 'scheduler',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo scheduled'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Also create some non-scheduled executions
        createTestExecutions("FutureProject", 3)

        when:
        def query = new ExecutionQuery(projFilter: "FutureProject", statusFilter: "scheduled")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result.size() == 1
        result.result[0].status == "scheduled"
    }
}
