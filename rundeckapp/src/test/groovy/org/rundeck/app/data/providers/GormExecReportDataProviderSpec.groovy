package org.rundeck.app.data.providers

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import spock.lang.Specification
import testhelper.TestDomainFactory

import javax.persistence.EntityNotFoundException

class GormExecReportDataProviderSpec extends Specification implements DataTest {
    GormExecReportDataProvider provider = new GormExecReportDataProvider()

    def setupSpec() {
        mockDomains(Execution, ExecReport, Workflow, CommandExec, PluginStep, JobExec)
    }

    def "CreateReportFromExecutionId"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid, status: 'succeeded', dateCompleted: new Date())

        when:
        def actual = provider.createReportFromExecution(e.id)
        def created = provider.get(actual.report.id)

        then:
        actual.isSaved
        !actual.errors
        created
    }

    def "CreateReportFromExecutionUuid"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid, status: 'succeeded', dateCompleted: new Date())

        when:
        def actual = provider.createReportFromExecution(e.uuid)
        def created = provider.get(actual.report.id)

        then:
        actual.isSaved
        !actual.errors
        created
    }

    def "CreateReportFromExecutionShouldThrowErrorIfExceptionDoesNotExist"() {
        when:
        provider.createReportFromExecution(execId)

        then:
        thrown(EntityNotFoundException)

        where:
        execId | _
        -1011L | 'Execution with id -1011 does not exist'
        "some-uuid" | 'Execution with uuid some-uuid does not exist'
    }
}
