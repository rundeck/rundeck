package org.rundeck.tests.functional.schedule

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.tests.functional.api.ResponseModels.CreateJobResponse
import org.rundeck.tests.functional.api.ResponseModels.JobsExportResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class ScheduleSpec extends BaseContainer{

    def setupSpec() {
        startEnvironment()
    }

    def "TEST: when schedule is flipped, job remains scheduled"(){
        given:
        def projectName = "test-job-flip-scheduleEnabled-scheduler-bug.sh"
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()
        Object projectJsonMap = [
                "name": projectName.toString(),
                "description": "test-job-flip-scheduleEnabled-scheduler-bug.sh",
                "config": [
                        "test.property": "test value",
                        "project.execution.history.cleanup.enabled": "true",
                        "project.execution.history.cleanup.retention.days": "1",
                        "project.execution.history.cleanup.batch": "500",
                        "project.execution.history.cleanup.retention.minimum": "0",
                        "project.execution.history.cleanup.schedule": "0 0/1 * 1/1 * ? *"
                ]
        ]

        def responseProject = client.doPost("/projects", projectJsonMap)
        assert responseProject.successful

        def jobName = "test-job-flip-scheduleEnabled-scheduler-bug"
        def jobXml = "<joblist>\n" +
                "   <job>\n" +
                "      <name>${jobName}</name>\n" +
                "      <group>api-test</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <context>\n" +
                "          <project>${projectName}</project>\n" +
                "      </context>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <schedule>\n" +
                "        <month month='*' />\n" +
                "        <time hour='23' minute='22' seconds='0' />\n" +
                "        <weekday day='MON,SUN,THU,TUE,WED' />\n" +
                "        <year year='*' />\n" +
                "      </schedule>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>echo hello there</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        assert job1CreatedResponse.successful
        CreateJobResponse job1CreatedParsedResponse = mapper.readValue(job1CreatedResponse.body().string(), CreateJobResponse.class)
        def job1Id = job1CreatedParsedResponse.succeeded[0]?.id

        def getExportedData = () -> {
            def response = client.doGetAcceptAll("/project/${projectName}/jobs/export")
            JobsExportResponse[] jobExport = mapper.readValue(response.body().string(), JobsExportResponse[].class)
            return jobExport[0]
        }

        when:
        def exportedData = getExportedData()

        then:
        exportedData.schedule?.month != null
        exportedData.schedule?.time != null
        exportedData.schedule?.year != null
        exportedData.schedule?.weekday != null

        when:
        def disableSchedulesResponse = client.doPostWithoutBody("/job/${job1Id}/schedule/disable")
        assert disableSchedulesResponse.successful
        def exportedDataAfterDisable = getExportedData()

        then:
        exportedDataAfterDisable.scheduleEnabled == "false"
        exportedDataAfterDisable.schedule?.month != null
        exportedDataAfterDisable.schedule?.time != null
        exportedDataAfterDisable.schedule?.year != null
        exportedDataAfterDisable.schedule?.weekday != null

        when:
        def enableSchedulesResponse = client.doPostWithoutBody("/job/${job1Id}/schedule/enable")
        assert enableSchedulesResponse.successful
        def exportedDataAfterEnable = getExportedData()

        then:
        exportedDataAfterEnable.scheduleEnabled == "true"
        exportedDataAfterEnable.schedule?.month != null
        exportedDataAfterEnable.schedule?.time != null
        exportedDataAfterEnable.schedule?.year != null
        exportedDataAfterEnable.schedule?.weekday != null

    }

}
