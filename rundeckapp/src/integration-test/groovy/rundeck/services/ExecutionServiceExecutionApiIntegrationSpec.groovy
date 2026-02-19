package rundeck.services

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * Integration tests for ExecutionService count cache functionality.
 * These tests require a real database to test HQL queries.
 */
@Integration
@Rollback
class ExecutionServiceExecutionApiIntegrationSpec extends Specification {

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigurationService configurationService

    def setup() {
        // Clear any existing cache using static method
        ExecutionService.ExecutionCountCacheKey.invalidateAll(executionService.executionCountCache)
    }

    private void enableCountPerformance() {
        configurationService.setBoolean('api.executionQueryConfig.countPerformance.enabled', true)
    }

    private void disableCountPerformance() {
        configurationService.setBoolean('api.executionQueryConfig.countPerformance.enabled', false)
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

    // ==================== countExecutions Tests ====================

    def "countExecutions returns correct count for project filter"() {
        given:
        createTestExecutions("ProjectA", 5)
        createTestExecutions("ProjectB", 3)

        when:
        def query = new ExecutionQuery(projFilter: "ProjectA")
        def count = query.countExecutions()

        then:
        count == 5
    }

    def "countExecutions returns correct count for user filter"() {
        given:
        createTestExecutions("TestProject", 6) // users: user0, user1, user2, user0, user1, user2

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", userFilter: "user0")
        def count = query.countExecutions()

        then:
        count == 2 // user0 appears at index 0 and 3
    }

    def "countExecutions returns correct count for status filter succeeded"() {
        given:
        createTestExecutions("TestProject", 5) // statuses: succeeded, failed, succeeded, failed, succeeded

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "succeeded")
        def count = query.countExecutions()

        then:
        count == 3 // indices 0, 2, 4 are succeeded
    }

    def "countExecutions returns correct count for status filter failed"() {
        given:
        createTestExecutions("TestProject", 5) // statuses: succeeded, failed, succeeded, failed, succeeded

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "failed")
        def count = query.countExecutions()

        then:
        count == 2 // indices 1, 3 are failed
    }

    def "countExecutions returns 0 for non-existent project"() {
        given:
        createTestExecutions("ExistingProject", 5)

        when:
        def query = new ExecutionQuery(projFilter: "NonExistentProject")
        def count = query.countExecutions()

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
        enableCountPerformance()

        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test")) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", statusFilter: "succeeded")) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", userFilter: "admin")) == true

        disableCountPerformance()
    }

    def "canUseSimpleCount returns true for job UUID queries"() {
        expect:
        enableCountPerformance()

        // UUID job IDs can use execution.job_uuid column (no JOIN)
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["550e8400-e29b-41d4-a716-446655440000"])) == true
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["uuid-1", "uuid-2"])) == true

        disableCountPerformance()
    }


    def "canUseSimpleCount returns false for queries requiring JOIN"() {
        expect:
        // These require JOIN with scheduled_execution
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobIdListFilter: ["12345"])) == false // Long ID
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", groupPath: "path")) == false
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", jobFilter: "job")) == false
        executionService.canUseSimpleCount(new ExecutionQuery(projFilter: "Test", adhoc: true)) == false
    }

    def "countExecutions with job UUID filter"() {
        given:
        def executions = createTestExecutions("TestProject", 5)
        def jobUuid = executions[0].scheduledExecution.uuid

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", jobIdListFilter: [jobUuid])
        def count = query.countExecutions()

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

    // ==================== queryJobExecutions Tests (migrated from ExecutionServiceTests) ====================

    def "queryJobExecutions returns empty for job with no executions"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: 'emptyJob',
                project: 'Test',
                groupPath: 'test',
                description: 'a job with no executions',
                argString: '-a b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        job.save(flush: true, failOnError: true)

        when:
        def result = executionService.queryJobExecutions(job, null)

        then:
        result.total == 0
    }

    def "queryJobExecutions returns execution for job with one execution"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: 'singleExecJob',
                project: 'Test',
                groupPath: 'test',
                description: 'a job with one execution',
                argString: '-a b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        job.save(flush: true, failOnError: true)

        Execution exec = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'succeeded',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec.save(flush: true, failOnError: true)

        when:
        def result = executionService.queryJobExecutions(job, null)

        then:
        result.total == 1
    }

    def "queryJobExecutions filters by succeeded status"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: 'statusFilterJob',
                project: 'Test',
                groupPath: 'test',
                description: 'a job for status filtering',
                argString: '-a b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        job.save(flush: true, failOnError: true)

        Execution exec = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'succeeded',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec.save(flush: true, failOnError: true)

        when:
        def result = executionService.queryJobExecutions(job, 'succeeded')

        then:
        result.total == 1
    }

    def "queryJobExecutions filters by failed status"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: 'failedStatusJob',
                project: 'Test',
                groupPath: 'test',
                description: 'a job for failed status filtering',
                argString: '-a b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        job.save(flush: true, failOnError: true)

        // Create succeeded execution first
        Execution exec1 = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'succeeded',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec1.save(flush: true, failOnError: true)

        // Create failed execution
        Execution exec2 = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'failed',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec2.save(flush: true, failOnError: true)

        when:
        def result = executionService.queryJobExecutions(job, 'failed', 0, 20)

        then:
        result.total == 1
    }

    def "queryJobExecutions filters by custom status"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: 'customStatusJob',
                project: 'Test',
                groupPath: 'test',
                description: 'a job for custom status filtering',
                argString: '-a b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        job.save(flush: true, failOnError: true)

        // Create succeeded execution first
        Execution exec1 = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'succeeded',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec1.save(flush: true, failOnError: true)

        // Create custom status execution
        Execution exec2 = new Execution(
                scheduledExecution: job,
                project: 'Test',
                status: 'custom status',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'testuser',
                jobUuid: job.uuid,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'echo test')]
                ).save()
        )
        exec2.save(flush: true, failOnError: true)

        when:
        def result = executionService.queryJobExecutions(job, 'custom status', 0, 20)

        then:
        result.total == 1
    }

    // ==================== Additional Status Filter Tests ====================

    def "countExecutions returns correct count for status filter running"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'runningJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create running execution (dateCompleted is null)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: null,
            dateStarted: new Date(),
            dateCompleted: null,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "running")
        def count = query.countExecutions()

        then:
        count == 1
    }

    def "countExecutions returns correct count for status filter scheduled"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'scheduledJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create scheduled execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "scheduled",
            dateStarted: new Date() + 1,
            dateCompleted: null,
            user: "scheduler",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed execution
        createTestExecutions("TestProject", 2)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "scheduled")
        def count = query.countExecutions()

        then:
        count == 1
    }

    def "countExecutions returns correct count for status filter aborted"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'abortedJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create aborted execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "aborted",
            cancelled: true,
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed execution
        createTestExecutions("TestProject", 2)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "aborted")
        def count = query.countExecutions()

        then:
        count == 1
    }

    def "countExecutions returns correct count for status filter timedout"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'timedoutJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create timedout execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "timedout",
            timedOut: true,
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed execution
        createTestExecutions("TestProject", 2)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "timedout")
        def count = query.countExecutions()

        then:
        count == 1
    }

    def "countExecutions returns correct count for status filter failed-with-retry"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'retryJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create failed-with-retry execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "failed-with-retry",
            willRetry: true,
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed execution
        createTestExecutions("TestProject", 2)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "failed-with-retry")
        def count = query.countExecutions()

        then:
        count == 1
    }

    // ==================== Execution Type Filter Tests ====================

    def "countExecutions returns correct count for executionTypeFilter"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'execTypeJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create user execution
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            executionType: "user",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create scheduled execution type
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            executionType: "scheduled",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "scheduler",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", executionTypeFilter: "user")
        def count = query.countExecutions()

        then:
        count == 1
    }

    // ==================== Aborted By Filter Tests ====================

    def "countExecutions returns correct count for abortedbyFilter"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'abortedByJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution aborted by admin
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "aborted",
            cancelled: true,
            abortedby: "admin",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution aborted by user2
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "aborted",
            cancelled: true,
            abortedby: "user2",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", abortedbyFilter: "admin")
        def count = query.countExecutions()

        then:
        count == 1
    }

    // ==================== Date Range Filter Tests ====================

    def "countExecutions returns correct count for date completed filter"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'dateFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        def now = new Date()
        def yesterday = new Date(now.time - 24 * 60 * 60 * 1000)
        def lastWeek = new Date(now.time - 7 * 24 * 60 * 60 * 1000)

        // Create recent execution (yesterday)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: yesterday,
            dateCompleted: yesterday,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create old execution (last week)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: lastWeek,
            dateCompleted: lastWeek,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def twoDaysAgo = new Date(now.time - 2 * 24 * 60 * 60 * 1000)
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            doendafterFilter: true,
            endafterFilter: twoDaysAgo
        )
        def count = query.countExecutions()

        then:
        count == 1 // Only yesterday's execution
    }

    def "countExecutions returns correct count for date started filter"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'dateStartedJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        def now = new Date()
        def yesterday = new Date(now.time - 24 * 60 * 60 * 1000)
        def lastWeek = new Date(now.time - 7 * 24 * 60 * 60 * 1000)

        // Create recent execution (yesterday)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: yesterday,
            dateCompleted: yesterday,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create old execution (last week)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: lastWeek,
            dateCompleted: lastWeek,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def threeDaysAgo = new Date(now.time - 3 * 24 * 60 * 60 * 1000)
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            dostartbeforeFilter: true,
            startbeforeFilter: threeDaysAgo
        )
        def count = query.countExecutions()

        then:
        count == 1 // Only last week's execution
    }

    // ==================== Exclude Running Filter Tests ====================

    def "countExecutions returns correct count with excludeRunning"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'excludeRunningJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create running execution (dateCompleted is null)
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: null,
            dateStarted: new Date(),
            dateCompleted: null,
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create completed executions
        createTestExecutions("TestProject", 3)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", excludeRunning: true)
        def count = query.countExecutions()

        then:
        count == 3 // Only completed executions
    }

    // ==================== Multiple Job UUIDs Tests ====================

    def "countExecutions returns correct count for multiple job UUIDs"() {
        given:
        def job1 = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'job1',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job 1',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        def job2 = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'job2',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job 2',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        def job3 = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'job3',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job 3',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create 2 executions for job1
        2.times {
            new Execution(
                scheduledExecution: job1,
                jobUuid: job1.uuid,
                project: "TestProject",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
        }

        // Create 3 executions for job2
        3.times {
            new Execution(
                scheduledExecution: job2,
                jobUuid: job2.uuid,
                project: "TestProject",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
        }

        // Create 4 executions for job3
        4.times {
            new Execution(
                scheduledExecution: job3,
                jobUuid: job3.uuid,
                project: "TestProject",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
        }

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", jobIdListFilter: [job1.uuid, job2.uuid])
        def count = query.countExecutions()

        then:
        count == 5 // 2 from job1 + 3 from job2
    }

    // ==================== Combined Filters Tests ====================

    def "countExecutions returns correct count with combined filters"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'combinedFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution matching all filters
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            executionType: "user",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "admin",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with different user
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            executionType: "user",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "developer",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with different status
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "failed",
            executionType: "user",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "admin",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            statusFilter: "succeeded",
            userFilter: "admin",
            executionTypeFilter: "user"
        )
        def count = query.countExecutions()

        then:
        count == 1 // Only the first execution matches all filters
    }

    // ==================== Cross-Project Job Reference Tests (shouldUseUnionQuery path) ====================

    def "countExecutions with includeJobRef counts direct and referenced executions"() {
        given:
        // Create target job in ProjectA (the job being referenced)
        def targetJob = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'targetJob',
            project: 'ProjectA',
            groupPath: 'test',
            description: 'the job being referenced',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo target'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create parent job in ProjectB (references targetJob)
        def parentJob = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'parentJob',
            project: 'ProjectB',
            groupPath: 'test',
            description: 'job that references targetJob',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo parent'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create 3 direct executions of targetJob in ProjectA
        3.times {
            new Execution(
                scheduledExecution: targetJob,
                jobUuid: targetJob.uuid,
                project: "ProjectA",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
        }

        // Create 2 executions in ProjectB that reference targetJob (cross-project reference)
        2.times {
            def parentExec = new Execution(
                scheduledExecution: parentJob,
                jobUuid: parentJob.uuid,
                project: "ProjectB",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)

            // Create ReferencedExecution linking parent execution to targetJob
            new ReferencedExecution(
                jobUuid: targetJob.uuid,
                status: "succeeded",
                execution: parentExec
            ).save(flush: true, failOnError: true)
        }

        when:
        def query = new ExecutionQuery(
            projFilter: "ProjectA",
            jobIdListFilter: [targetJob.uuid],
            includeJobRef: true,
            execProjects: ["ProjectA", "ProjectB"]
        )
        def count = query.countExecutions()

        then:
        query.shouldUseUnionQuery() == true
        count == 5 // 3 direct + 2 referenced
    }

    def "countExecutions with includeJobRef and status filter"() {
        given:
        // Create target job in ProjectA
        def targetJob = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'targetJobWithStatus',
            project: 'ProjectA',
            groupPath: 'test',
            description: 'the job being referenced',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo target'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create parent job in ProjectB
        def parentJob = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'parentJobWithStatus',
            project: 'ProjectB',
            groupPath: 'test',
            description: 'job that references targetJob',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo parent'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create 2 succeeded direct executions
        2.times {
            new Execution(
                scheduledExecution: targetJob,
                jobUuid: targetJob.uuid,
                project: "ProjectA",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: "user1",
                workflow: new Workflow(
                    keepgoing: true,
                    commands: [new CommandExec([adhocRemoteString: 'echo test'])]
                ).save(flush: true)
            ).save(flush: true, failOnError: true)
        }

        // Create 1 failed direct execution
        new Execution(
            scheduledExecution: targetJob,
            jobUuid: targetJob.uuid,
            project: "ProjectA",
            status: "failed",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create 1 succeeded referenced execution in ProjectB
        def parentExec1 = new Execution(
            scheduledExecution: parentJob,
            jobUuid: parentJob.uuid,
            project: "ProjectB",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        new ReferencedExecution(
            jobUuid: targetJob.uuid,
            status: "succeeded",
            execution: parentExec1
        ).save(flush: true, failOnError: true)

        // Create 1 failed referenced execution in ProjectB
        def parentExec2 = new Execution(
            scheduledExecution: parentJob,
            jobUuid: parentJob.uuid,
            project: "ProjectB",
            status: "failed",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        new ReferencedExecution(
            jobUuid: targetJob.uuid,
            status: "failed",
            execution: parentExec2
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(
            projFilter: "ProjectA",
            jobIdListFilter: [targetJob.uuid],
            includeJobRef: true,
            execProjects: ["ProjectA", "ProjectB"],
            statusFilter: "succeeded"
        )
        def count = query.countExecutions()

        then:
        query.shouldUseUnionQuery() == true
        count == 3 // 2 succeeded direct + 1 succeeded referenced
    }

    def "shouldUseUnionQuery returns false when includeJobRef is not set"() {
        when:
        def query = new ExecutionQuery(
            projFilter: "ProjectA",
            jobIdListFilter: ["some-uuid"]
        )

        then:
        query.shouldUseUnionQuery() == false
    }

    def "shouldUseUnionQuery returns false for multiple job UUIDs"() {
        when:
        def query = new ExecutionQuery(
            projFilter: "ProjectA",
            jobIdListFilter: ["uuid-1", "uuid-2"],
            includeJobRef: true,
            execProjects: ["ProjectA", "ProjectB"]
        )

        then:
        query.shouldUseUnionQuery() == false
    }

    // ==================== Node Filter Tests ====================

    def "queryExecutions filters by nodeFilter with name: prefix"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'nodeFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with node1 in succeededNodeList
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            succeededNodeList: "node1,node2,node3",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with different nodes
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            succeededNodeList: "nodeA,nodeB",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "name:node1")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].succeededNodeList.contains("node1")
    }

    def "queryExecutions filters by nodeFilter with name: prefix and leading spaces"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'nodeFilterTrimJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with mynode in succeededNodeList
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            succeededNodeList: "mynode,othernode",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        // Test that trim() properly handles leading/trailing whitespace after name:
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "name:  mynode  ")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].succeededNodeList.contains("mynode")
    }

    def "queryExecutions filters by plain node name without prefix"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'plainNodeFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with server1 in failedNodeList
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "failed",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            failedNodeList: "server1",
            succeededNodeList: "server2,server3",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution without server1
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            succeededNodeList: "serverA,serverB",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        // Plain node name (no prefix, no special chars) should search in node lists
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "server1")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].failedNodeList.contains("server1")
    }

    def "queryExecutions filters by nodeFilter in failedNodeList"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'failedNodeFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with failedNode in failedNodeList
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "failed",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            failedNodeList: "failedNode",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "name:failedNode")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].failedNodeList == "failedNode"
    }

    def "queryExecutions filters by complex nodeFilter with colon searches filter field"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'complexFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with complex filter
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            filter: "tags:production hostname:web.*",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with different filter
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            filter: "tags:staging",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        // Complex filter with colon (not name:) should search in filter field
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "tags:production")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].filter.contains("tags:production")
    }

    def "queryExecutions filters by nodeFilter with regex pattern searches filter field"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
            uuid: UUID.randomUUID().toString(),
            jobName: 'regexFilterJob',
            project: 'TestProject',
            groupPath: 'test',
            description: 'test job',
            argString: '-a b',
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with regex filter
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            filter: "web-server-.*",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        // Create execution with plain filter
        new Execution(
            scheduledExecution: job,
            jobUuid: job.uuid,
            project: "TestProject",
            status: "succeeded",
            dateStarted: new Date(),
            dateCompleted: new Date(),
            user: "user1",
            filter: "db-server",
            workflow: new Workflow(
                keepgoing: true,
                commands: [new CommandExec([adhocRemoteString: 'echo test'])]
            ).save(flush: true)
        ).save(flush: true, failOnError: true)

        when:
        // Filter containing .* (regex pattern) should search in filter field
        def query = new ExecutionQuery(projFilter: "TestProject", nodeFilter: "web-server-.*")
        def result = executionService.queryExecutions(query, 0, 20)

        then:
        result.total == 1
        result.result[0].filter == "web-server-.*"
    }

}
