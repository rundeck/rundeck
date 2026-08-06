package org.rundeck.app.data.providers

import grails.testing.gorm.DataTest
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.JobQuery
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.job.query.RdJobQueryInput
import rundeck.data.paging.RdSortOrder
import rundeck.services.JobSchedulesService
import spock.lang.Specification

class GormJobQueryProviderSpec extends Specification implements DataTest {
        GormJobQueryProvider provider = new GormJobQueryProvider()

    Closure doWithSpring() {
        { ->
            rundeckJobDefinitionManager(InstanceFactoryBean, Mock(RundeckJobDefinitionManager))
        }
    }
    def setup() {
        mockDomains(ScheduledExecution, Workflow, CommandExec)
    }
    def "QueryJobs"() {
        given:
        provider.applicationContext = applicationContext
        provider.jobSchedulesService = Mock(JobSchedulesService) {
            0 * isScheduled(_) >> true
        }
        ScheduledExecution job1 = new ScheduledExecution(jobName: "one", project:"test1",workflow: new Workflow(commands:[new CommandExec(adhocLocalString: "echo hello")]))
        job1.save()
        ScheduledExecution job2 = new ScheduledExecution(jobName: "two",groupPath: "two", project:"test1",workflow: new Workflow(commands:[new CommandExec(adhocLocalString: "echo hello")]))
        job2.save()

        when:
        def page = provider.queryJobs(input)

        then:
        page.results.size() == expectedRowCount
        page.total == expectedTotal

        where:
        expectedRowCount | expectedTotal | input
        2                | 2             | new RdJobQueryInput()
        2                | 2             | new RdJobQueryInput(projFilter: "test1")
        1                | 2             | new RdJobQueryInput(projFilter: "test1", max: 1)
        1                | 1             | new RdJobQueryInput(projFilter: "test1",groupPath: "two")
    }

    def "ApplyJobComponentCriteria"() {
        given:
        provider.applicationContext = Mock(ApplicationContext) {
            getBeansOfType(JobQuery) >> ["cmp1": Mock(JobQuery) {
                1 * extendCriteria(_,_,_)
            }]
        }

        when:
        provider.applyJobComponentCriteria(new RdJobQueryInput(), Mock(BuildableCriteria))

        then:
        noExceptionThrown()
    }

    def "ApplySort"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applySort(query, crit)

        then:
        expectedCount * crit.order(expectKey, expectOrder)

        where:
        expectedCount | expectKey       | expectOrder | query
        1             | "jobName"       | "asc"     | new RdJobQueryInput()
        1             | "project"       | "desc"    | new RdJobQueryInput(projFilter: "one", sortOrders: [new RdSortOrder(column: "proj",direction:"desc")])

    }

    def "ApplyIdCriteria"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applyIdCriteria(idlist, crit)

        then:
        1 * crit.or(_) >> { args ->
            args[0].delegate = crit
            args[0].call()
            return crit
        }
        1 * crit.'in'(propName, _) >> crit
        0 * crit.eq(propName, _)

        where:
        idlist        | propName
        [1L]          | "id"
        ["fake-uuid"] | "uuid"
    }

    def "ApplyTxtFiltersCriteria"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applyTxtFiltersCriteria(query, crit)

        then:
        expectedCount * crit.ilike(expectKey, expectVal)

        where:
        expectedCount | expectKey       | expectVal | query
        1             | "jobName"       | "%one%"     | new RdJobQueryInput(jobFilter: "one")
        1             | "description"   | "%test%"    | new RdJobQueryInput(descFilter: "test")

    }

    def "ApplyEqFiltersCriteria"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applyEqFiltersCriteria(query, crit)

        then:
        expectedCount * crit.eq(expectKey, expectVal)

        where:
        expectedCount | expectKey        | expectVal        | query
        1             | "jobName"        | "job1"           | new RdJobQueryInput(jobExactFilter: "job1")
        1             | "project"        | "p1"             | new RdJobQueryInput(projFilter: "p1")
        1             | "loglevel"       | "NORMAL"         | new RdJobQueryInput(loglevelFilter: "NORMAL")
        1             | "serverNodeUUID" | "serveruuid"     | new RdJobQueryInput(serverNodeUUIDFilter: "serveruuid")
    }

    def "ApplyBoolFiltersCriteria"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applyBoolFiltersCriteria(query, crit)

        then:
        expectedCount * crit.eq(expectKey, expectVal)

        where:
        expectedCount | expectKey               | expectVal         | query
        1             | "executionEnabled"      | true              | new RdJobQueryInput(executionEnabledFilter: true)
        1             | "scheduleEnabled"       | false             | new RdJobQueryInput(scheduleEnabledFilter: false)
    }

    def "ApplyGroupPathCriteria"() {
        given:
        def crit = Mock(BuildableCriteria)

        when:
        provider.applyGroupPathCriteria(input, crit)

        then:
        1 * crit.or(_) >> { args ->
            args[0].delegate = crit
            args[0].call()
            return crit
        }
        expEq * crit.eq(propName, propVal)
        expLike * crit.like(propName, propVal+"/%")
        expNull * crit.isNull(propName)

        where:
        expEq | expLike | expNull | propName    | propVal | input
        1     | 0       | 0       | "groupPath" | "grp1"  | new RdJobQueryInput(groupPathExact: "grp1")
        1     | 0       | 1       | "groupPath" | ""      | new RdJobQueryInput(groupPathExact: "-")
        1     | 1       | 0       | "groupPath" | "grp1"  | new RdJobQueryInput(groupPath: "grp1")

    }

    def "applyIdCriteria with 1001 UUIDs emits chunked 'in' calls not individual eq calls"() {
        given: "1001 UUIDs and a mock criteria that captures 'in' call sizes"
        def crit = Mock(BuildableCriteria)
        def inCallSizes = []
        def uuids = (1..1001).collect { "uuid-${it}" }

        when:
        provider.applyIdCriteria(uuids, crit)

        then:
        // or { } called once
        1 * crit.or(_) >> { args ->
            args[0].delegate = crit
            args[0].call()
            return crit
        }
        // 'in'("uuid", ...) called twice (chunk of 1000 + chunk of 1) — not 1001 individual eq calls
        2 * crit.'in'("uuid", _) >> { args ->
            inCallSizes << (args[1] as Collection).size()
            return crit
        }
        // no individual eq("uuid") calls
        0 * crit.eq("uuid", _)
        // all 1001 UUIDs covered across the two chunks
        inCallSizes.sum() == 1001
        inCallSizes.every { it <= 1000 }
    }

    def "applyIdCriteria splits Long ids and String uuids into separate chunked 'in' calls"() {
        given: "a mix of 1001 Long ids and 1001 String uuids"
        def crit = Mock(BuildableCriteria)
        def idSizes = []
        def uuidSizes = []
        def ids = (1L..1001L).collect { it } + (1..1001).collect { "uuid-${it}".toString() }

        when:
        provider.applyIdCriteria(ids, crit)

        then:
        1 * crit.or(_) >> { args ->
            args[0].delegate = crit
            args[0].call()
            return crit
        }
        // Long ids -> 'in'("id", ...) chunked (1000 + 1)
        2 * crit.'in'("id", _) >> { args ->
            idSizes << (args[1] as Collection).size()
            return crit
        }
        // String uuids -> 'in'("uuid", ...) chunked (1000 + 1)
        2 * crit.'in'("uuid", _) >> { args ->
            uuidSizes << (args[1] as Collection).size()
            return crit
        }
        // no individual eq calls for either type
        0 * crit.eq(_, _)
        idSizes.sum() == 1001
        uuidSizes.sum() == 1001
        idSizes.every { it <= 1000 }
        uuidSizes.every { it <= 1000 }
    }

    def "queryJobs with 1001 idlist entries returns correct results without error"() {
        given: "1001 jobs — functional correctness test for the idlist path"
        provider.applicationContext = applicationContext
        provider.jobSchedulesService = Mock(JobSchedulesService) {
            0 * isScheduled(_) >> true
        }
        def jobs = (1..1001).collect { i ->
            new ScheduledExecution(
                jobName: "bulk-job-${i}",
                project: "test-bulk",
                workflow: new Workflow(commands: [new CommandExec(adhocLocalString: "echo hello")])
            ).save(flush: false)
        }
        ScheduledExecution.withSession { it.flush() }
        def idlist = jobs*.id.join(',')

        when:
        def page = provider.queryJobs(new RdJobQueryInput(idlist: idlist, max: 0))

        then:
        page.results.size() == 1001
        page.total == 1001
    }
}
