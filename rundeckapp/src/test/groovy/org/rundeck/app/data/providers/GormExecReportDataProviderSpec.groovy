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
import spock.lang.Unroll
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

    @Unroll
    def "getExecutionReports with optionFilter - #description"() {
        given: "standard test executions with different argStrings"
        def executionData = setupExecutions()
        executionData.each { execData ->
            provider.saveReport(ExecReportUtil.buildSaveReportRequest(execData.execution))
        }

        when:
        def query = new ExecQuery()
        query.optionFilter = searchTerm
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == expectedCount
        if (expectedExecutionIds) {
            results.collect { it.executionId }.sort() == expectedExecutionIds.call(executionData).sort()
        }

        where:
        description                | searchTerm                    | expectedCount | expectedExecutionIds
        "single exact match"       | "-APP otherapp"               | 1            | { data -> [data[1].execution.id] }
        "single partial match"     | "-SLEEP"                      | 3            | { data -> [data[0].execution.id, data[2].execution.id, data[3].execution.id] }
        "no matches"              | "-NONEXISTENT_OPTION value"   | 0            | null
        "case insensitive match"   | "-app otherapp"               | 1            | { data -> [data[1].execution.id] }
        "value only match"         | "otherapp"                    | 1            | { data -> [data[1].execution.id] }
    }

    @Unroll
    def "getExecutionReports with optionFilter OR logic - #description"() {
        given: "executions with varying option combinations"
        def executionData = setupExecutions()
        executionData.each { execData ->
            provider.saveReport(ExecReportUtil.buildSaveReportRequest(execData.execution))
        }

        when:
        def query = new ExecQuery()
        query.optionFilter = searchTerm
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == expectedCount
        if (expectedMatches) {
            def actualIds = results.collect { it.executionId }.sort()
            def expectedIds = expectedMatches.call(executionData).sort()
            actualIds == expectedIds
        }

        where:
        description                                     | searchTerm                        | expectedCount | expectedMatches
        "multiple options, everything match due to OR"  | "-APP myapp -ENV production"      | 4            | { data -> [data[0].execution.id, data[1].execution.id, data[2].execution.id, data[3].execution.id] }
        "exact option-value pair"                       | "-SLEEP 10"                       | 2            | { data -> [data[0].execution.id, data[2].execution.id] }
        "option name only, partial match"               | "-ENV"                            | 5            | { data -> [data[0].execution.id, data[1].execution.id, data[2].execution.id, data[3].execution.id, data[4].execution.id] }
        "multiple options, independent of order"        | "-FIRST value1 -SECOND value2"    | 2            | { data -> [data[1].execution.id, data[2].execution.id] }
        "standalone value with non-related optionName"  | "value1 -APP"                     | 3            | { data -> [data[0].execution.id, data[1].execution.id, data[2].execution.id] }
        "mixed standalone values"                       | "option 30"                       | 1            | { data -> [data[3].execution.id] }
    }

    private List setupExecutions() {
        return [
            [execution: TestDomainFactory.createExecution(argString: "-APP myapp -ENV production -SLEEP 10", status: 'succeeded', dateCompleted: new Date())],
            [execution: TestDomainFactory.createExecution(argString: "-APP otherapp -ENV production -FIRST value1 -SECOND value2", status: 'succeeded', dateCompleted: new Date())],
            [execution: TestDomainFactory.createExecution(argString: "-APP myapp -ENV staging -SLEEP 10 -SECOND value2 -FIRST value1", status: 'succeeded', dateCompleted: new Date())],
            [execution: TestDomainFactory.createExecution(argString: "-ENV production -OTHER option -DIFFERENT value -SLEEP 30", status: 'succeeded', dateCompleted: new Date())],
            [execution: TestDomainFactory.createExecution(argString: "-ENVIRONMENT staging", status: 'succeeded', dateCompleted: new Date())]
        ]
    }

    def "getExecutionReports with optionFilter - combined with other filters"() {
        given:
        def executionData = setupExecutions()
        // Add user field to first two executions for testing
        executionData[0].execution.user = 'testuser'
        executionData[1].execution.user = 'otheruser'

        executionData.each { execData ->
            provider.saveReport(ExecReportUtil.buildSaveReportRequest(execData.execution))
        }

        when:
        def query = new ExecQuery()
        query.optionFilter = "-APP myapp"
        query.userFilter = "testuser"
        def results = provider.getExecutionReports(query, true, null, [])

        then:
        results.size() == 1
        results[0].executionId == executionData[0].execution.id
    }


    @Unroll
    def "getExecutionReports with optionFilter - edge cases: #description"() {
        given:
        def executionData = setupExecutions()
        executionData.each { execData ->
            provider.saveReport(ExecReportUtil.buildSaveReportRequest(execData.execution))
        }

        when:
        def query = new ExecQuery()
        query.optionFilter = filterValue
        def results = provider.getExecutionReports(query, true, null, [])

        then: "should return all results without filtering"
        results.size() == 5

        where:
        description        | filterValue
        "empty string"     | ""
        "null value"       | null
        "whitespace only"  | "   "
    }
}
