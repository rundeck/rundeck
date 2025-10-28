package org.rundeck.tests.functional.api.job


import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.jobs.JobExecutionsResponse
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

import java.time.LocalDateTime
import java.time.ZoneId

@APITest
class JobScheduledSpec extends BaseContainer {

    String projectName

    def setupSpec() {
        startEnvironment()
    }

    def setup() {
        projectName = UUID.randomUUID().toString()

    }

    def cleanup() {
        deleteProject(projectName)
    }

    def "scheduled job run should succeed"(){
        setup:
            setupProject(projectName)
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
                      <uuid>${UUID.randomUUID()}</uuid>
                      <description></description>
                      <loglevel>INFO</loglevel>
                      <context>
                          <project>${projectName}</project>
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
            def jobId = JobUtils.jobImportFile(projectName,path,client).succeeded[0].id
        when:
            def response = doGet("/job/${jobId}/executions?status=succeeded")
            def count = jsonValue(response.body()).executions.size()
        then:
            verifyAll {
                response.successful
                response.code() == 200
            }
        when:
            // Waits for at least one execution to start
            waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { !!it},
                    WaitingTime.EXCESSIVE)
            // Waits for all executions to finish
            waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                    verifyForAll(ExecutionUtils.Verifiers.executionFinished()))
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


    def "test-job-flip-scheduleEnabled"(){
        given:
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-flip-scheduleEnabled",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)

        def jobName1 = "scheduledJob1"
        def jobXml1 = JobUtils.generateScheduledJobsXml(jobName1)

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id

        when: "assert_job_schedule_enabled for job1"
        def job1Detail = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)
        then:
        job1Detail?.executionEnabled
        job1Detail?.scheduleEnabled

        when: "TEST: when schedule is on, job does execute"
        def disableSchedulesResponse = doPost("/job/${job1Id}/schedule/disable")
        assert disableSchedulesResponse.successful
        def job1DetailAfterDisable = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)

        then:
        !job1DetailAfterDisable?.scheduleEnabled

        when: "TEST: bulk job schedule enable"
        def enableSchedulesResponse = doPost("/job/${job1Id}/schedule/enable")
        assert enableSchedulesResponse.successful
        def job1DetailAfterEnable = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)

        then:
        job1DetailAfterEnable?.scheduleEnabled
    }

    def "test-job-flip-scheduleEnabled-bulk"(){
        given:
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-flip-scheduleEnabled-bulk",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)

        def jobName1 = "scheduledJob1"
        def jobXml1 = JobUtils.generateScheduledJobsXml(jobName1)

        def jobName2 = "scheduledJob2"
        def jobXml2 = JobUtils.generateScheduledJobsXml(jobName2)

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)
        def job2CreatedParsedResponse = JobUtils.createJob(projectName, jobXml2, client)

        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def job2Id = job2CreatedParsedResponse.succeeded[0]?.id

        when: "assert_job_schedule_enabled for job1"
        def job1Detail = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)
        then:
        job1Detail?.executionEnabled

        when: "assert_job_schedule_enabled for job2"
        def job2Detail = JobUtils.getJobDetailsById(job2Id as String, MAPPER, client)
        then:
        job2Detail?.executionEnabled

        Thread.sleep(WaitingTime.LOW.toMillis()) // As the original test says

        when: "TEST: bulk job schedule disable"
        Object idList = [
                "idlist" : List.of(
                        job1Id,
                        job2Id
                )
        ]
        def disableSchedulesResponse = doPost("/jobs/schedule/disable", idList)
        assert disableSchedulesResponse.successful
        def job1DetailAfterDisable = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)
        def job2DetailAfterDisable = JobUtils.getJobDetailsById(job2Id as String, MAPPER, client)

        then:
        !job1DetailAfterDisable?.scheduleEnabled
        !job2DetailAfterDisable?.scheduleEnabled

        when: "TEST: bulk job schedule enable"
        def enableSchedulesResponse = doPost("/jobs/schedule/enable", idList)
        assert enableSchedulesResponse.successful
        def job1DetailAfterEnable = JobUtils.getJobDetailsById(job1Id as String, MAPPER, client)
        def job2DetailAfterEnable = JobUtils.getJobDetailsById(job2Id as String, MAPPER, client)

        then:
        job1DetailAfterEnable?.scheduleEnabled
        job2DetailAfterEnable?.scheduleEnabled

    }

    def "test-job-flip-executionEnabled"(){
        given:
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test1",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateScheduledExecutionXml(jobName1)

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)

        when: "TEST: created job has the executionEnabled property set to true"
        def jobDetails = JobUtils.getJobDetailsById(job1CreatedParsedResponse.succeeded[0]?.id, MAPPER, client)

        then:
        jobDetails.executionEnabled

        when: "TEST: when execution is on, job does execute"
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def executions1 = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1 = MAPPER.readValue(executions1.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution1.executions.size() == 0

        when:
        def jobExecResponseFor1 = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1.successful
        def executions1AfterExecResponse = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterExec = MAPPER.readValue(executions1AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution1AfterExec.executions.size() == 1

        when: "TEST: when execution is off, job doesn't execute"
        def disabledJobsResponse = doPost("/job/${job1Id}/execution/disable")
        assert disabledJobsResponse.successful

        def jobExecResponseFor1AfterDisable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterDisable.code() == 400 // bc execs are disabled
        jobExecResponseFor1AfterDisable.close()

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = MAPPER.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1

        when: "TEST: when execution is off and then on again, job does execute"
        def enabledJobsResponse = doPost("/job/${job1Id}/execution/enable")
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.toMillis())

        def jobExecResponseFor1AfterEnable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterEnable.successful


        def executionsForJob1AfterEnable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterEnable = MAPPER.readValue(executionsForJob1AfterEnable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterEnable.executions.size() == 2
    }

    def "test-job-flip-executionEnabled-bulk"(){
        given:
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test1",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = createSampleProject(projectJsonMap)

        def jobName1 = "xmljob"
        def jobXml1 = JobUtils.generateScheduledExecutionXml(jobName1)

        def jobName2 = "xmljob2"
        def jobXml2 = JobUtils.generateScheduledExecutionXml(jobName2)

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)
        def job2CreatedParsedResponse = JobUtils.createJob(projectName, jobXml2, client)

        when: "assert_job_execution_count with job 1"
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id
        def executions1 = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1 = MAPPER.readValue(executions1.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution1.executions.size() == 0

        when: "assert_job_execution_count with job 2"
        def job2Id = job2CreatedParsedResponse.succeeded[0]?.id
        def executions2 = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2 = MAPPER.readValue(executions2.body().string(), JobExecutionsResponse.class)
        then:
        parsedExecutionsResponseForExecution2.executions.size() == 0

        when: "execute_job 1"
        def jobExecResponseFor1 = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1.successful
        def executions1AfterExecResponse = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterExec = MAPPER.readValue(executions1AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution1AfterExec.executions.size() == 1

        when: "execute_job 2"
        def jobExecResponseFor2 = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2.successful
        def executions2AfterExecResponse = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterExec = MAPPER.readValue(executions2AfterExecResponse.body().string(), JobExecutionsResponse.class)
        then: "assert_job_execution_count job1"
        parsedExecutionsResponseForExecution2AfterExec.executions.size() == 1

        when: "TEST: bulk job execution disable"
        Object idList = [
                "idlist": List.of(
                        job1Id,
                        job2Id
                )
        ]
        def disabledJobsResponse = doPost("/jobs/execution/disable", idList)
        assert disabledJobsResponse.successful

        def jobExecResponseFor1AfterDisable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterDisable.code() == 400 // bc execs are disabled

        def jobExecResponseFor2AfterDisable = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2AfterDisable.code() == 400  // bc execs are disabled

        def executionsForJob1AfterDisable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterDisable = MAPPER.readValue(executionsForJob1AfterDisable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterDisable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterDisable = MAPPER.readValue(executionsForJob2AfterDisable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterDisable.executions.size() == 1
        parsedExecutionsResponseForExecution2AfterDisable.executions.size() == 1

        when: "TEST: bulk job execution enable"
        def enabledJobsResponse = doPost("/jobs/execution/enable", idList)
        assert enabledJobsResponse.successful

        // Necessary since the api needs to breathe after enable execs
        Thread.sleep(WaitingTime.LOW.toMillis())

        def jobExecResponseFor1AfterEnable = JobUtils.executeJob(job1Id, client)
        assert jobExecResponseFor1AfterEnable.successful

        def jobExecResponseFor2AfterEnable = JobUtils.executeJob(job2Id, client)
        assert jobExecResponseFor2AfterEnable.successful

        def executionsForJob1AfterEnable = doGet("/job/${job1Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution1AfterEnable = MAPPER.readValue(executionsForJob1AfterEnable.body().string(), JobExecutionsResponse.class)

        def executionsForJob2AfterEnable = doGet("/job/${job2Id}/executions")
        JobExecutionsResponse parsedExecutionsResponseForExecution2AfterEnable = MAPPER.readValue(executionsForJob2AfterEnable.body().string(), JobExecutionsResponse.class)

        then:
        parsedExecutionsResponseForExecution1AfterEnable.executions.size() == 2
        parsedExecutionsResponseForExecution2AfterEnable.executions.size() == 2
    }

    def "job executes when its definition and scheduled are updated"(){
        setup:
        setupProject(projectName)
        def client = getClient()

        when: "a scheduled job is created"
        def jobId = UUID.randomUUID().toString()
        String jobXmlDefinitionFile = JobUtils.updateJobFileToImport("api-test-executions-running-scheduled.xml",
                projectName,
                ["uuid" : jobId, "job-name" : "j1", "schedule-crontab": "*/3 * * ? * * *"])
        JobUtils.jobImportFile(projectName, jobXmlDefinitionFile, client)

        // Waits for at least one execution to start
        def executionsAfterCreation = waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { !!it},
                WaitingTime.EXCESSIVE)

        then: "the job executes"
        executionsAfterCreation.size() > 0

        when: "the scheduled job definition (but not its schedule) is updated"
        def updatedNameJobXmlDefinitionFile = createTempFile( new File(jobXmlDefinitionFile).text.replace("<name>j1</name>", "<name>Updated j1</name>"))
        JobUtils.jobImportFile(projectName, updatedNameJobXmlDefinitionFile, client, JobUtils.DUPE_OPTION_UPDATE)

        // Wait for more executions to be created
        def executionsAfterNameUpdate = waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { it.size() > executionsAfterCreation.size()},
                WaitingTime.EXCESSIVE)

        then: "the job still executes"
        executionsAfterNameUpdate.collect( { it.id } ).containsAll(executionsAfterCreation.collect( { it.id } ))

        when: "the scheduled job schedule is updated"
        def updatedScheduleJobXmlDefinitionFile = createTempFile(new File(jobXmlDefinitionFile).text.replace("<schedule crontab=\"*/3 * * ? * * *\"/>", "<schedule crontab=\"*/4 * * ? * * *\"/>"))
        JobUtils.jobImportFile(projectName, updatedScheduleJobXmlDefinitionFile, client, JobUtils.DUPE_OPTION_UPDATE)

        // Wait for more executions to be created
        def executionsAfterScheduleUpdate = waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName),
                { it.size() > executionsAfterNameUpdate.size()},
                WaitingTime.EXCESSIVE)

        then: "the job still executes"
        executionsAfterScheduleUpdate.collect( { it.id } ).containsAll(executionsAfterNameUpdate.collect( { it.id } ))
    }

    private def createSampleProject(Object projectJsonMap) {
        return client.post("/projects", projectJsonMap)
    }

    private def createTempFile(String content) {
        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = content
        tempFile.deleteOnExit()
        tempFile.path
    }

}
