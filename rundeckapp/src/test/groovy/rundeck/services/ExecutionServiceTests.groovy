/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.testing.gorm.DataTest
import groovy.mock.interceptor.MockFor
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 8/7/13
 * Time: 3:07 PM
 *
 * The ExecutionServiceTests contains tests for the ExecutionService class, focusing on querying job executions
 * Cases that execute HQL directly (like ExecutionQuery.countExecutions) should be covered in integration tests
 */
class ExecutionServiceTests extends Specification implements DataTest {

    def setupSpec() { mockDomains ScheduledExecution, Workflow, CommandExec, Execution }

    private getAppCtxtMock(){
        def mockAppCtxt = new MockFor(ApplicationContext)
        mockAppCtxt.demand.getBeansOfType(1..1){[]}
        return mockAppCtxt.proxyInstance()
    }
    private List createTestExecs() {

        ScheduledExecution se1 = new ScheduledExecution(
                uuid: 'test1',
                jobName: 'red color',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se1.save()
        ScheduledExecution se2 = new ScheduledExecution(
                uuid: 'test2',
                jobName: 'green and red color',
                project: 'Test',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se2.save()
        ScheduledExecution se3 = new ScheduledExecution(
                uuid: 'test3',
                jobName: 'blue green and red color',
                project: 'Test',
                groupPath: 'some/where/else',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se3.save()
        ScheduledExecution fse1 = new ScheduledExecution(
                uuid: 'test4',
                jobName: 'purple',
                project: 'Future',
                groupPath: 'future',
                description: 'a job',
                argString: '-a b -c d',
                crontabString: '01 13 1 1 1 2099',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != fse1.save(failOnError: true)

        Execution e1 = new Execution(
                scheduledExecution: se1,
                project: "Test",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()

        Execution e2 = new Execution(
                scheduledExecution: se2,
                project: "Test",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'bob',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test2 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e2.save()
        Execution e3 = new Execution(
                scheduledExecution: se3,
                project: "Test",
                status: "succeeded",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'chuck',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test3 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e3.save()
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 120)
        Date futureDate = cal.getTime()
        Execution schExec1 = new Execution(
                scheduledExecution: fse1,
                project: "Future",
                status: "scheduled",
                dateStarted: futureDate,
                dateCompleted: null,
                user: 'luke',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test3 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != schExec1.save()

        [e1, e2, e3, schExec1]
    }

    /**
     * Test groupPath
     */
    public void testExecutionsQueryGroupPath() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some"),0,20)

        then:
        assert 3 == result.total
    }

    /**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub1() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some/where"), 0, 20)

        then:
        assert 2 == result.total
    }/**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some/where/else"), 0, 20)

        then:
        assert 1 == result.total
    }
    /**
     * Test excludeGroupPath subpath 3
     */
    public void testApiExecutionsQueryExcludeGroupPathSub3() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some/where/else"), 0, 20)

        then:
        assert 2 == result.total
    }
    /**
     * Test excludeGroupPath subpath 2
     */
    public void testApiExecutionsQueryExcludeGroupPathSub2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some/where"), 0, 20)

        then:
        assert 1 == result.total
    }
    /**
     * Test excludeGroupPath subpath equal
     */
    public void testApiExecutionsQueryExcludeGroupPathSubEqual() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some"), 0, 20)

        then:
        assert 0 == result.total
    }

    /**
     * Test groupPathExact (top)
     */
    public void testApiExecutionsGroupPathExact() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some"), 0, 20)

        then:
        assert 1 == result.total
    }

    /**
     * Test groupPathExact (mid)
     */
    public void testApiExecutionsGroupPathExactSub1() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some/where"), 0, 20)

        then:
        assert 1 == result.total
    }

    /**
     * Test groupPathExact (bot)
     */
    public void testApiExecutionsGroupPathExactSub2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some/where/else"), 0, 20)

        then:
        assert 1 == result.total
    }

    /**
     * Test excludeGroupPathExact level1
     */
    public void testApiExecutionsExcludeGroupPathExact1() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some"), 0, 20)

        then:
        assert 2 == result.total
    }

    /**
     * Test excludeGroupPathExact level2
     */
    public void testApiExecutionsExcludeGroupPathExact2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some/where"), 0, 20)

        then:
        assert 2 == result.total
    }

    /**
     * Test excludeGroupPathExact level3
     */
    public void testApiExecutionsExcludeGroupPathExact3() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some/where/else"), 0, 20)

        then:
        assert 2 == result.total
    }
    /**
     * Test excludeJob wildcard
     */
    public void testApiExecutionsExcludeJob() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%red color"), 0, 20)

        then:
        assert 0 == result.total
    }
    /**
     * Test excludeJob wildcard 2
     */
    public void testApiExecutionsExcludeJob2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%green and red color"), 0, 20)

        then:
        assert 1 == result.total
    }
    /**
     * Test excludeJob wildcard 3
     */
    public void testApiExecutionsExcludeJob3() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%blue green and red color"), 0, 20)

        then:
        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 1
     */
    public void testApiExecutionsExcludeJobExact1() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobExactFilter: "red color"), 0, 20)

        then:
        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 2
     */
    public void testApiExecutionsExcludeJobExact2() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobExactFilter: "green and red color"), 0, 20)

        then:
        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 3
     */
    public void testApiExecutionsExcludeJobExact3() {
        when:
        def svc = new ExecutionService()

        def execs = createTestExecs()
        svc.applicationContext = getAppCtxtMock()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "blue green and red color"), 0, 20)

        then:
        assert 2 == result.total
    }
    // ==================== Execution Count Cache Tests ====================
    // Note: Tests that require HQL (ExecutionQuery.countExecutions) need integration tests
    // These unit tests focus on cache logic that can be tested with mocks

    /**
     * Test that cache is disabled by default (configurationService is null)
     */
    void testExecutionCountCacheDisabledByDefault() {
        given:
        def svc = new ExecutionService()
        svc.configurationService = null

        expect:
        svc.isExecutionCountCacheEnabled() == false
    }

    /**
     * Test that cache is disabled when config says false
     */
    void testExecutionCountCacheDisabledByConfig() {
        given:
        def svc = new ExecutionService()
        def mockConfigService = Mock(ConfigurationService) {
            getBoolean('executionService.countCache.enabled', false) >> false
        }
        svc.configurationService = mockConfigService

        expect:
        svc.isExecutionCountCacheEnabled() == false
    }

    /**
     * Test cache key generation with ExecutionCountCacheKey class
     */
    void testBuildCountCacheKey() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            statusFilter: "succeeded",
            userFilter: "admin"
        )
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.status == "succeeded"
        cacheKey.user == "admin"
    }

    /**
     * Test cache key generation with null values
     */
    void testBuildCountCacheKeyWithNulls() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(projFilter: "TestProject")
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.status == null
        cacheKey.user == null
    }

    /**
     * Test cache key uses relative filter string (recentFilter) when available
     */
    void testBuildCountCacheKeyWithRecentFilter() {
        given:
        def svc = new ExecutionService()
        def now = new Date()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            recentFilter: "60d",
            endafterFilter: now,
            doendafterFilter: true
        )
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.recentFilter == "60d"
    }

    /**
     * Test cache key uses relative filter string (olderFilter) when available
     */
    void testBuildCountCacheKeyWithOlderFilter() {
        given:
        def svc = new ExecutionService()
        def now = new Date()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            olderFilter: "30d",
            endbeforeFilter: now,
            doendbeforeFilter: true
        )
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.olderFilter == "30d"
    }

    /**
     * Test that same relative filter produces same cache key regardless of timestamp
     */
    void testBuildCountCacheKeySameRelativeFilter() {
        given:
        def svc = new ExecutionService()
        def time1 = new Date()
        def time2 = new Date(time1.time + 3600000)  // 1 hour later

        when:
        // Two requests with same relative filter but different computed timestamps
        def query1 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d", endbeforeFilter: time1, doendbeforeFilter: true)
        def query2 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d", endbeforeFilter: time2, doendbeforeFilter: true)
        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)

        then:
        // Keys should be the same because relative filter string is the same
        key1 == key2
    }

    /**
     * Test that different relative filters produce different cache keys
     */
    void testBuildCountCacheKeyDifferentRelativeFilters() {
        given:
        def svc = new ExecutionService()

        when:
        def query1 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "30d")
        def query2 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d")
        def query3 = new ExecutionQuery(projFilter: "TestProject", recentFilter: "24h")
        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)
        def key3 = svc.buildCountCacheKey(query3)

        then:
        key1 != key2
        key1 != key3
        key2 != key3
    }

    /**
     * Test cache key generation with single job ID filter
     */
    void testBuildCountCacheKeyWithSingleJobId() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['uuid-123']
        )
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.jobId == 'uuid-123'
    }

    /**
     * Test cache key with no job ID filter
     */
    void testBuildCountCacheKeyWithNoJobId() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(projFilter: "TestProject")
        def cacheKey = svc.buildCountCacheKey(query)

        then:
        cacheKey.project == "TestProject"
        cacheKey.jobId == null
    }

    /**
     * Test isCacheableQuery returns false for multiple job IDs
     */
    void testIsCacheableQueryWithMultipleJobIds() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['uuid-1', 'uuid-2', 'uuid-3']
        )

        then:
        svc.isCacheableQuery(query) == false
    }

    /**
     * Test isCacheableQuery returns true for single job ID
     */
    void testIsCacheableQueryWithSingleJobId() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['uuid-123']
        )

        then:
        svc.isCacheableQuery(query) == true
    }

    /**
     * Test cache key equals() and hashCode() work correctly for HashMap/Cache lookups
     */
    void testCacheKeyEqualsAndHashCode() {
        given:
        def svc = new ExecutionService()

        when:
        // Create two identical queries
        def query1 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d")
        def query2 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d")
        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)

        then:
        // equals() should return true
        key1 == key2
        key1.equals(key2)
        // hashCode() should be the same
        key1.hashCode() == key2.hashCode()
        // toString() should be meaningful
        key1.toString().contains("TestProject")
        key1.toString().contains("60d")
    }

    /**
     * Test cache key works correctly in a HashMap (simulates Guava Cache behavior)
     */
    void testCacheKeyWorksInHashMap() {
        given:
        def svc = new ExecutionService()
        def map = new HashMap<ExecutionService.ExecutionCountCacheKey, Long>()

        when:
        def query1 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d")
        def query2 = new ExecutionQuery(projFilter: "TestProject", olderFilter: "60d")
        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)
        
        // Store with key1
        map.put(key1, 12345L)
        // Retrieve with key2 (should find it because equals/hashCode match)
        def retrieved = map.get(key2)

        then:
        retrieved == 12345L
    }

    /**
     * Test cache key with single jobId works correctly in HashMap
     */
    void testCacheKeyWithJobIdWorksInHashMap() {
        given:
        def svc = new ExecutionService()
        def map = new HashMap<ExecutionService.ExecutionCountCacheKey, Long>()

        when:
        def query1 = new ExecutionQuery(projFilter: "TestProject", jobIdListFilter: ['uuid-123'])
        def query2 = new ExecutionQuery(projFilter: "TestProject", jobIdListFilter: ['uuid-123'])
        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)
        
        map.put(key1, 999L)
        def retrieved = map.get(key2)

        then:
        key1.jobId == key2.jobId
        key1.hashCode() == key2.hashCode()
        key1 == key2
        retrieved == 999L
    }

    /**
     * Test isCacheableQuery returns true for queries without date filters
     */
    void testIsCacheableQueryNoDateFilters() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(projFilter: "TestProject", statusFilter: "succeeded")

        then:
        svc.isCacheableQuery(query) == true
    }

    /**
     * Test isCacheableQuery returns true for queries with relative filters
     * recentFilter covers doendafterFilter, olderFilter covers doendbeforeFilter
     */
    void testIsCacheableQueryWithRelativeFilters() {
        given:
        def svc = new ExecutionService()

        when:
        // recentFilter sets doendafterFilter - this is covered
        def queryRecent = new ExecutionQuery(projFilter: "TestProject", recentFilter: "60d", doendafterFilter: true)
        // olderFilter sets doendbeforeFilter - this is covered
        def queryOlder = new ExecutionQuery(projFilter: "TestProject", olderFilter: "30d", doendbeforeFilter: true)
        // Both relative filters
        def queryBoth = new ExecutionQuery(projFilter: "TestProject", recentFilter: "7d", olderFilter: "1d", 
                                           doendafterFilter: true, doendbeforeFilter: true)

        then:
        svc.isCacheableQuery(queryRecent) == true
        svc.isCacheableQuery(queryOlder) == true
        svc.isCacheableQuery(queryBoth) == true
    }

    /**
     * Test isCacheableQuery returns false when relative filter has additional absolute dates
     */
    void testIsCacheableQueryWithMixedFilters() {
        given:
        def svc = new ExecutionService()
        def now = new Date()

        when:
        // olderFilter=60d with additional begin date - not cacheable
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            olderFilter: "60d",
            doendbeforeFilter: true,
            // Additional absolute date filter
            endafterFilter: now,
            doendafterFilter: true
        )

        then:
        // Not cacheable because doendafterFilter is set but recentFilter is not
        svc.isCacheableQuery(query) == false
    }

    /**
     * Test isCacheableQuery returns false for queries with absolute dates but no relative filter
     */
    void testIsCacheableQueryWithAbsoluteDatesOnly() {
        given:
        def svc = new ExecutionService()
        def now = new Date()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            endafterFilter: now,
            doendafterFilter: true
            // No recentFilter or olderFilter
        )

        then:
        svc.isCacheableQuery(query) == false
    }

    /**
     * Test isCacheableQuery returns false for queries with start date filters but no relative filter
     */
    void testIsCacheableQueryWithStartDateFiltersOnly() {
        given:
        def svc = new ExecutionService()
        def now = new Date()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            startafterFilter: now,
            dostartafterFilter: true
        )

        then:
        svc.isCacheableQuery(query) == false
    }

    /**
     * Test canUseSimpleCount returns true when no job filters
     */
    void testCanUseSimpleCountNoJobFilters() {
        given:
        def svc = new ExecutionService()
        svc.configurationService = Mock(ConfigurationService){
            getBoolean('api.executionQueryConfig.countPerformance.enabled', false) >> true
        }

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            statusFilter: "succeeded"
        )

        then:
        svc.canUseSimpleCount(query) == true
    }

    /**
     * Test canUseSimpleCount returns true when jobIdListFilter has UUID strings
     * (can use execution.job_uuid column without JOIN)
     */
    void testCanUseSimpleCountWithJobUuidFilter() {
        given:
        def svc = new ExecutionService()
        svc.configurationService = Mock(ConfigurationService){
            getBoolean('api.executionQueryConfig.countPerformance.enabled', false) >> true
        }

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['550e8400-e29b-41d4-a716-446655440000']
        )

        then:
        svc.canUseSimpleCount(query) == true
    }

    /**
     * Test canUseSimpleCount returns true with multiple UUID job IDs
     */
    void testCanUseSimpleCountWithMultipleJobUuids() {
        given:
        def svc = new ExecutionService()
        svc.configurationService = Mock(ConfigurationService){
            getBoolean('api.executionQueryConfig.countPerformance.enabled', false) >> true
        }

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['uuid-1', 'uuid-2', 'uuid-3']
        )

        then:
        svc.canUseSimpleCount(query) == true
    }

    /**
     * Test canUseSimpleCount returns false when jobIdListFilter has Long IDs (legacy)
     * (requires JOIN with scheduled_execution)
     */
    void testCanUseSimpleCountWithLongJobId() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobIdListFilter: ['12345']  // Numeric string = Long ID
        )

        then:
        svc.canUseSimpleCount(query) == false
    }

    /**
     * Test canUseSimpleCount returns false when groupPath present
     */
    void testCanUseSimpleCountWithGroupPath() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            groupPath: "some/path"
        )

        then:
        svc.canUseSimpleCount(query) == false
    }

    /**
     * Test canUseSimpleCount returns false when jobFilter present
     */
    void testCanUseSimpleCountWithJobFilter() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            jobFilter: "myJob"
        )

        then:
        svc.canUseSimpleCount(query) == false
    }

    /**
     * Test canUseSimpleCount returns false when adhoc is set
     */
    void testCanUseSimpleCountWithAdhoc() {
        given:
        def svc = new ExecutionService()

        when:
        def query = new ExecutionQuery(
            projFilter: "TestProject",
            adhoc: true
        )

        then:
        svc.canUseSimpleCount(query) == false
    }

    /**
     * Test cache initialization with custom TTL
     */
    void testCacheInitializationWithCustomTTL() {
        given:
        def svc = new ExecutionService()
        def mockConfigService = Mock(ConfigurationService) {
            getBoolean('executionService.countCache.enabled', false) >> true
            getInteger('executionService.countCache.ttlSeconds', 30) >> 60
        }
        svc.configurationService = mockConfigService

        when:
        def cache = svc.getExecutionCountCache()

        then:
        cache != null
    }

    /**
     * Test cache put and get operations
     */
    void testCachePutAndGet() {
        given:
        def svc = new ExecutionService()
        def mockConfigService = Mock(ConfigurationService) {
            getBoolean('executionService.countCache.enabled', false) >> true
            getInteger('executionService.countCache.ttlSeconds', 30) >> 30
        }
        svc.configurationService = mockConfigService

        when:
        def cache = svc.getExecutionCountCache()
        cache.put("test-key", 42L)
        def result = cache.getIfPresent("test-key")

        then:
        result == 42L
    }

    /**
     * Test cache returns null for missing key
     */
    void testCacheMiss() {
        given:
        def svc = new ExecutionService()
        def mockConfigService = Mock(ConfigurationService) {
            getBoolean('executionService.countCache.enabled', false) >> true
            getInteger('executionService.countCache.ttlSeconds', 30) >> 30
        }
        svc.configurationService = mockConfigService

        when:
        def cache = svc.getExecutionCountCache()
        def result = cache.getIfPresent("non-existent-key")

        then:
        result == null
    }

    /**
     * Test cache invalidation
     */
    void testCacheInvalidation() {
        given:
        def svc = new ExecutionService()
        def mockConfigService = Mock(ConfigurationService) {
            getBoolean('executionService.countCache.enabled', false) >> true
            getInteger('executionService.countCache.ttlSeconds', 30) >> 30
        }
        svc.configurationService = mockConfigService

        when:
        def cache = svc.getExecutionCountCache()
        cache.put("test-key", 42L)
        cache.invalidate("test-key")
        def result = cache.getIfPresent("test-key")

        then:
        result == null
    }

    /**
     * Test that different queries produce different cache keys
     */
    void testDifferentQueriesProduceDifferentCacheKeys() {
        given:
        def svc = new ExecutionService()

        when:
        def query1 = new ExecutionQuery(projFilter: "Project1")
        def query2 = new ExecutionQuery(projFilter: "Project2")
        def query3 = new ExecutionQuery(projFilter: "Project1", statusFilter: "failed")

        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)
        def key3 = svc.buildCountCacheKey(query3)

        then:
        key1 != key2
        key1 != key3
        key2 != key3
    }

    /**
     * Test that same query produces same cache key
     */
    void testSameQueryProducesSameCacheKey() {
        given:
        def svc = new ExecutionService()

        when:
        def query1 = new ExecutionQuery(projFilter: "Project1", statusFilter: "succeeded")
        def query2 = new ExecutionQuery(projFilter: "Project1", statusFilter: "succeeded")

        def key1 = svc.buildCountCacheKey(query1)
        def key2 = svc.buildCountCacheKey(query2)

        then:
        key1 == key2
    }

}
