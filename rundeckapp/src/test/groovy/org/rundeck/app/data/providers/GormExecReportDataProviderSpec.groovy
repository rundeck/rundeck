package org.rundeck.app.data.providers

import grails.testing.gorm.DataTest
import org.springframework.context.MessageSource
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.data.report.SaveReportRequestImpl
import rundeck.data.util.ExecReportUtil
import spock.lang.Specification
import testhelper.TestDomainFactory

import javax.persistence.EntityNotFoundException

class GormExecReportDataProviderSpec extends Specification implements DataTest {
    GormExecReportDataProvider provider = new GormExecReportDataProvider()

    def setupSpec() {
        mockDomains(Execution, ExecReport, Workflow, CommandExec, PluginStep, JobExec)

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
}
