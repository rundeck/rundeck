package org.rundeck.app.data.job.converters

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Workflow
import rundeck.data.execution.RdExecution
import spock.lang.Specification
import spock.lang.Unroll
import testhelper.TestDomainFactory

class ExecutionToRdExecutionConverterSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains(Execution, Workflow, CommandExec)
    }

    ExecutionToRdExecutionConverter converter = new ExecutionToRdExecutionConverter()

    def "should return null when converting null Execution object"() {
        when:
        RdExecution rdExecution = converter.convert(null)

        then:
        rdExecution == null
    }

    @Unroll
    def "should convert Execution to RdExecution #property"() {
        given:
        Execution execution = TestDomainFactory.createExecution(
                uuid: UUID.randomUUID().toString(),
                jobUuid: UUID.randomUUID().toString(),
                "${property}": value
        )

        when:
        RdExecution rdExecution = converter.convert(execution)

        then:
        rdExecution.uuid == execution.uuid
        rdExecution.jobUuid == execution.jobUuid
        rdExecution.workflow.steps.size() == execution.workflow.commands.size()
        rdExecution."${property}" == value


        where:
        property            | value
        "argString"         | "argValue"
        "user"              | "testuser"
        "dateStarted"       | new Date()
        "dateCompleted"     | new Date()
        "status"            | "running"
        "outputfilepath"    | "file:///the/log/file"
        "failedNodeList"    | "failer"
        "succeededNodeList" | "succeeder"
        "abortedby"         | "tester"
        "cancelled"         | true
        "timedOut"          | true
        "executionType"     | "user"
        "retryAttempt"      | 5
        "willRetry"         | true
        "serverNodeUUID"    | UUID.randomUUID().toString()
        "nodeThreadcount"   | 10
        "retryOriginalId"   | 145L
        "retryPrevId"       | 140L
    }
}
