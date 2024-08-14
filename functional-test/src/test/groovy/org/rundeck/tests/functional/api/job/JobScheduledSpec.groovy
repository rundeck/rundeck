package org.rundeck.tests.functional.api.job

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.BaseContainer

import java.time.LocalDateTime
import java.time.ZoneId

@APITest
class JobScheduledSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "scheduled job run should succeed (sleep 20 sec)"(){
        setup:
            def nowDate = LocalDateTime.now(ZoneId.of("GMT"))
            def upDate = nowDate.plusSeconds(10)

            def ny = upDate.year
            def nmo = upDate.monthValue
            def nd = upDate.dayOfMonth
            def nh = upDate.hour
            def nm = upDate.minute
            def ns = upDate.second

            def xml = """
                <joblist>
                   <job>
                      <name>scheduled job</name>
                      <group>api-test/job-run-scheduled</group>
                      <uuid>api-test-job-run-scheduled</uuid>
                      <description></description>
                      <loglevel>INFO</loglevel>
                      <context>
                          <project>${PROJECT_NAME}</project>
                      </context>
                      <dispatch>
                        <threadcount>1</threadcount>
                        <keepgoing>true</keepgoing>
                      </dispatch>
                      <schedule>
                        <time hour='P_NH' seconds='P_NS' minute='P_NM' />
                        <month month='P_NMO'  day='P_ND' />
                        <year year='P_NY' />
                      </schedule>
                      <timeZone>GMT</timeZone>
                      <sequence>
                        <command>
                        <exec>echo hello there</exec>
                        </command>
                      </sequence>
                   </job>
                </joblist>
            """
            xml = xml
                .replaceAll('P_NY', ny.toString())
                .replaceAll('P_NMO', String.format('%02d', nmo))
                .replaceAll('P_ND', String.format('%02d', nd))
                .replaceAll('P_NH', String.format('%02d', nh))
                .replaceAll('P_NM', String.format('%02d', nm))
                .replaceAll('P_NS', String.format('%02d', ns))

            def path = JobUtils.generateFileToImport(xml, 'xml')
            def jobId = JobUtils.jobImportFile(PROJECT_NAME,path,client).succeeded[0].id
        when:
            def response = doGet("/job/${jobId}/executions?status=succeeded")
            def count = jsonValue(response.body()).executions.size()
        then:
            verifyAll {
                response.successful
                response.code() == 200
            }
        when:
            sleep 20000
            def response1 = doGet("/job/${jobId}/executions?status=succeeded")
        then:
            verifyAll {
                response1.successful
                response1.code() == 200
                def json = jsonValue(response1.body())
                def count2 = json.executions.size()
                def testVal = count + 1
                testVal == count2 && count != null && count2 != null
            }
    }

    def "job/id/run should succeed"() {
        setup:
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
            def jobId = JobUtils.jobImportFile(PROJECT_NAME,path,client).succeeded[0].id
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            def execId = jsonValue(jobRun.body()).id
            def responseExec = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.TIMEDOUT.state,
                    execId as String,
                    new ObjectMapper(),
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
        then:
            verifyAll {
                responseExec.status == 'timedout'
            }
    }

    def "job/id/run should succeed with retry"() {
        setup:
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
            def jobId = JobUtils.jobImportFile(PROJECT_NAME,path,client).succeeded[0].id
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            def execId = jsonValue(jobRun.body()).id
            def response = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.FAILED_WITH_RETRY.state,
                    execId as String,
                    new ObjectMapper(),
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
        then:
            verifyAll {
                response.status == 'failed-with-retry'
                response.retriedExecution.id != null
            }
        when:
            def responseExec1 = JobUtils.waitForExecutionToBe(
                    ExecutionStatus.TIMEDOUT.state,
                    response.retriedExecution.id as String,
                    new ObjectMapper(),
                    client,
                    WaitingTime.LOW,
                    WaitingTime.LOW
            )
        then:
            verifyAll {
                responseExec1.status == 'timedout'
                response.retriedExecution != null
            }
    }
}
