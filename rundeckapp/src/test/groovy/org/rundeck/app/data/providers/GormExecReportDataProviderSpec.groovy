package org.rundeck.app.data.providers

import com.dtolabs.rundeck.app.support.ExecQuery
import grails.testing.gorm.DataTest
import org.springframework.context.MessageSource
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.report.SaveReportRequestImpl
import rundeck.data.util.ExecReportUtil
import spock.lang.Specification
import testhelper.TestDomainFactory

import javax.persistence.EntityNotFoundException

class GormExecReportDataProviderSpec extends Specification implements DataTest {
    GormExecReportDataProvider provider = new GormExecReportDataProvider()

    def setupSpec() {
        mockDomains(Execution, ExecReport, Workflow, CommandExec, PluginStep, JobExec, ScheduledExecution)
    }
    
    def setup() {
        provider.configurationService = Mock(rundeck.services.ConfigurationService) {
            getInteger("pagination.default.max", 20) >> 20
        }


    def "CreateReportFromExecution"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid, status: 'succeeded', dateCompleted: new Date())

        when:
        def actual = provider.saveReport(ExecReportUtil.buildSaveReportRequest(e))
        def created = provider.get(actual.report.id)

        then:
        actual.isSaved
        !actual.errors
        created
    }

    def "CreateReportFromWithEmptyRequestShouldReturnError"() {
        when:

        provider.messageSource = Mock(MessageSource) {
            getMessage(_,_) >> "Error saving report"
        }

        def response = provider.saveReport(new SaveReportRequestImpl())

        then:
        !response.isSaved
        response.errors != null

    }

    def "ShouldDeleteExecReportsByExecutionUuid"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid, status: 'succeeded', dateCompleted: new Date())
        def report = provider.saveReport(ExecReportUtil.buildSaveReportRequest(e))

        when:
        def created = provider.get(report.report.id)
        provider.deleteAllByExecutionUuid(report.report.executionUuid)
        def deleted = provider.get(report.report.id)

        then:
        created
        deleted == null

    }
    def "deleteAllByExecutionId should delete exec reports by execution id"() {
        given:
        Execution e = TestDomainFactory.createExecution(uuid: null, status: 'succeeded', dateCompleted: new Date())
        def report = provider.saveReport(ExecReportUtil.buildSaveReportRequest(e))

        when:
        def created = provider.get(report.report.id)
        provider.deleteAllByExecutionId(report.report.executionId)
        def deleted = provider.get(report.report.id)

        then:
        created
        deleted == null

    }

    def "getExecutionReports with optionFilter - single option"() {
        given:
        // Create executions with different argStrings
        def exec1 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV production",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec2 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app2 -ENV staging",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec3 = TestDomainFactory.createExecution(
            argString: "-OTHER_OPTION value -ENVIRONMENT test",
            status: 'succeeded',
            dateCompleted: new Date()
        )

        // Create corresponding ExecReports
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec2))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec3))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-APPLICATION_NAME app1"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 1
        results[0].executionId == exec1.id
    }

    def "getExecutionReports with optionFilter - multiple options order independent"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV production -VERBOSE true",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec2 = TestDomainFactory.createExecution(
            argString: "-ENV production -APPLICATION_NAME app2 -DEBUG false",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec3 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV staging",
            status: 'succeeded',
            dateCompleted: new Date()
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec2))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec3))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-APPLICATION_NAME app1 -ENV production"  // Multiple options
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 1
        results[0].executionId == exec1.id
    }

    def "getExecutionReports with optionFilter - exact option value search"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-SLEEP 10 -ENV production -VERBOSE true",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec2 = TestDomainFactory.createExecution(
            argString: "-SLEEP 5 -ENV production -DEBUG false",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec3 = TestDomainFactory.createExecution(
            argString: "-OTHER option -SLEEP 10 -MORE stuff",
            status: 'succeeded',
            dateCompleted: new Date()
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec2))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec3))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-SLEEP 10"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 2
        results.collect { it.executionId }.sort() == [exec1.id, exec3.id].sort()
    }

    def "getExecutionReports with optionFilter - no matches"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV production",
            status: 'succeeded',
            dateCompleted: new Date()
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-NONEXISTENT_OPTION value"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 0
    }


    def "getExecutionReports with optionFilter - case insensitive matching"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME MyApp -ENV Production",
            status: 'succeeded',
            dateCompleted: new Date()
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-APPLICATION_NAME myapp"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 1
        results[0].executionId == exec1.id
    }

    def "getExecutionReports with optionFilter - different argument order matches"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-FIRST value1 -SECOND value2 -THIRD value3",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec2 = TestDomainFactory.createExecution(
            argString: "-SECOND value2 -THIRD different -FIRST value1",
            status: 'succeeded',
            dateCompleted: new Date()
        )
        def exec3 = TestDomainFactory.createExecution(
            argString: "-FIRST value1 -THIRD different",  // Missing SECOND value2
            status: 'succeeded',
            dateCompleted: new Date()
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec2))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec3))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-FIRST value1 -SECOND value2"  // Search for both options
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 2  // Should match exec1 and exec2, not exec3
        results.collect { it.executionId }.sort() == [exec1.id, exec2.id].sort()
    }

    def "getExecutionReports with optionFilter - combined with other filters"() {
        given:
        def exec1 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV production",
            status: 'succeeded',
            dateCompleted: new Date(),
            user: 'testuser'
        )
        def exec2 = TestDomainFactory.createExecution(
            argString: "-APPLICATION_NAME app1 -ENV staging",
            status: 'succeeded',
            dateCompleted: new Date(),
            user: 'otheruser'
        )

        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec1))
        provider.saveReport(ExecReportUtil.buildSaveReportRequest(exec2))

        when:
        def query = new ExecQuery()
        query.optionFilter = "-APPLICATION_NAME app1"
        query.userFilter = "testuser"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 1
        results[0].executionId == exec1.id
    }
}
