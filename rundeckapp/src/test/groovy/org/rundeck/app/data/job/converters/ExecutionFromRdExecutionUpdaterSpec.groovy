package org.rundeck.app.data.job.converters

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.execution.RdExecution
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import spock.lang.Specification
import testhelper.TestDomainFactory

class ExecutionFromRdExecutionUpdaterSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains(Execution, ScheduledExecution, Workflow, CommandExec)
    }

    def "should update Execution from RdExecution"() {
        given:
        ScheduledExecution job = TestDomainFactory.createJob()
        Execution e = new Execution()
        RdExecution re = new RdExecution()

        def serverUUID = UUID.randomUUID().toString()
        def startDate = new Date(123,06,15, 5,0)
        def completeDate = new Date(123,06,15, 5,0)
        re.uuid = "test-uuid"
        re.jobUuid = job.uuid
        re.status = "test-status"
        re.dateStarted = startDate
        re.dateCompleted = completeDate
        re.outputfilepath = "file://log/file"
        re.failedNodeList = "node1"
        re.succeededNodeList = "node2"
        re.abortedby = "tester"
        re.cancelled = true
        re.timedOut = true
        re.executionType = "user"
        re.userRoles = ["admin","user"]
        re.serverNodeUUID = serverUUID
        re.user = "tester"
        re.nodeThreadcount = 5
        re.retryOriginalId = 123L
        re.retryPrevId = 122L
        re.extraMetadataMap = [msg:"ok"]
        re.workflow = new RdWorkflow(steps: [new RdWorkflowStep(pluginType: WorkflowStepConstants.TYPE_COMMAND, configuration: [exec: "echo hello"])])

        when:
        ExecutionFromRdExecutionUpdater.update(e, re)

        then:
        e.uuid == "test-uuid"
        e.scheduledExecution == job
        e.jobUuid == job.uuid
        e.status == "test-status"
        e.dateStarted == startDate
        e.dateCompleted == completeDate
        e.workflow.steps.size() == 1
        e.failedNodeList == "node1"
        e.succeededNodeList == "node2"
        e.abortedby == "tester"
        e.cancelled
        e.timedOut
        e.user == "tester"
        e.userRoleList == '["admin","user"]'
        e.serverNodeUUID == serverUUID
        e.nodeThreadcount == 5
        e.retryOriginalId == 123L
        e.retryPrevId == 122L
        e.extraMetadataMap == re.extraMetadataMap
    }

}
