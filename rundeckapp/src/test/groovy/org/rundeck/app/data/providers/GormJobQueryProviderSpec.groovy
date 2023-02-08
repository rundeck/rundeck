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
            expectedRowCount * isScheduled(_) >> true
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
        1 * crit.eq(propName, _)

        where:
        idlist | propName
        [1L]   | "id"
        ["fake-uuid"]   | "uuid"
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

}
