package org.rundeck.app.data.providers.logstorage

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData
import org.rundeck.app.data.providers.GormExecReportDataProvider
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.JobExec
import rundeck.LogFileStorageRequest
import rundeck.PluginStep
import rundeck.Workflow
import spock.lang.Specification

class GormLogFileStorageRequestProviderSpec extends Specification implements DataTest {
    def provider = new GormLogFileStorageRequestProvider()

    def setupSpec() {
        mockDomains(Execution, LogFileStorageRequest, Workflow, CommandExec, PluginStep, JobExec)
    }

    def "Delete by execid no execution"() {
        given:

            def execUuid = UUID.randomUUID().toString()
        when:
            provider.delete(execUuid)
        then:
            noExceptionThrown()
    }

    def "Delete by execid no request"() {
        given:
            def execUuid = UUID.randomUUID().toString()
            def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user1',
                project: 'test',
                serverNodeUUID: null,
                outputfilepath: '/tmp/test/logs/blah/1.rdlog',
                uuid: execUuid
            ).save()


        when:
            provider.delete(execUuid)
        then:
            noExceptionThrown()
    }

    def "Delete by execid valid"() {
        given:
            def execUuid = UUID.randomUUID().toString()
            def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user1',
                project: 'test',
                serverNodeUUID: null,
                outputfilepath: '/tmp/test/logs/blah/1.rdlog',
                uuid: execUuid
            ).save()


            def request = provider.create(
                Mock(LogFileStorageRequestData) {
                    getExecutionUuid() >> execUuid
                    getExecutionId() >> exec.id
                    getPluginName() >> 'test'
                    getFiletype() >> 'test'
                    getCompleted() >> false

                }
            )
        when:
            def found = Execution.get(exec.id).logFileStorageRequest
        then:
            found != null
        when:
            provider.delete(execUuid)
        then:
            Execution.get(exec.id).logFileStorageRequest == null
            LogFileStorageRequest.get(request.id) == null
            noExceptionThrown()
    }
}
