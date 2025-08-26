package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

@APITest
class JobExecutionStatusSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "job/id/run should succeed"() {
        setup:
        def projectName =  UUID.randomUUID().toString()
        setupProject(projectName)
        def pathFile = JobUtils.updateJobFileToImport("api-test-execution-state-2.xml", projectName)
        def jobId = JobUtils.jobImportFile(projectName,pathFile,client).succeeded[0].id
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            int execId = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId > 0
            }
        when:
        // Waits for all executions to finish
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
        def response = doGet("/job/${jobId}/executions?status=test+status+code")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id == execId
        }
    }

    def "job/id/run should get into timedout status"() {
        setup:
        def projectName =  UUID.randomUUID().toString()
        setupProject(projectName)
        def xml = """
                <joblist>
                   <job>
                      <name>cli job</name>
                      <group>api-test/job-run-timeout</group>
                      <description></description>
                      <loglevel>INFO</loglevel>
                      <timeout>3s</timeout>
                      <dispatch>
                        <threadcount>1</threadcount>
                        <keepgoing>true</keepgoing>
                      </dispatch>
                      <sequence>
                        <command>
                        <exec>echo hello there ; sleep 30</exec>
                        </command>
                      </sequence>
                   </job>
                </joblist>
            """
        def path = JobUtils.generateFileToImport(xml, 'xml')
        def jobId = JobUtils.jobImportFile(projectName,path,client).succeeded[0].id
        when:
        def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
        def execId = jsonValue(jobRun.body()).id
        def responseExec = JobUtils.waitForExecution(
                ExecutionStatus.TIMEDOUT.state,
                execId as String,
                client)

        then:
        verifyAll {
            responseExec.status == 'timedout'
        }
    }

    def "job/id/run should get into failed-with-retry and timedout status"() {
        setup:
        def projectName =  UUID.randomUUID().toString()
        setupProject(projectName)
        def xml = """
                    <joblist>
                       <job>
                          <name>cli job</name>
                          <group>api-test/job-run-timeout-retry</group>
                          <description></description>
                          <loglevel>INFO</loglevel>
                          <timeout>3s</timeout>
                          <retry>1</retry>
                          <dispatch>
                            <threadcount>1</threadcount>
                            <keepgoing>true</keepgoing>
                          </dispatch>
                          <sequence>
                            <command>
                            <exec>echo hello there ; sleep 30</exec>
                            </command>
                          </sequence>
                       </job>
                    </joblist>
                """
        def path = JobUtils.generateFileToImport(xml, 'xml')
        def jobId = JobUtils.jobImportFile(projectName,path,client).succeeded[0].id
        when:
        def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
        def execId = jsonValue(jobRun.body()).id
        def response = JobUtils.waitForExecution(
                ExecutionStatus.FAILED_WITH_RETRY.state,
                execId as String,
                client)

        then:
        verifyAll {
            response.status == 'failed-with-retry'
            response.retriedExecution.id != null
        }
        when:
        def responseExec1 = JobUtils.waitForExecution(
                ExecutionStatus.TIMEDOUT.state,
                response.retriedExecution.id as String,
                client)

        then:
        verifyAll {
            responseExec1.status == 'timedout'
            response.retriedExecution != null
        }
    }
}
