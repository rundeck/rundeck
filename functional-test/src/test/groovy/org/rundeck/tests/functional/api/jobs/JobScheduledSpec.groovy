package org.rundeck.tests.functional.api.jobs

import org.rundeck.util.annotations.APITest
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
            def path = generatePath()
            def jobId = jobImportFile(path).succeeded[0].id
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
            def path = generatePath1()
            def jobId = jobImportFile(path).succeeded[0].id
        when:
            def execId = runJob(jobId, ["options":["opt2": "a"]])
            waitExecution(execId)
            def responseExec = get("/execution/${execId}", Map)
        then:
            verifyAll {
                responseExec.status == 'timedout'
            }
    }

    def "job/id/run should succeed with retry"() {
        setup:
            def path = generatePath2()
            def jobId = jobImportFile(path).succeeded[0].id
        when:
            def execId = runJob(jobId, ["options":["opt2": "a"]])
            waitExecution(execId)
            def responseExec = get("/execution/${execId}", Map)
        then:
            verifyAll {
                responseExec.status == 'failed-with-retry'
                responseExec.retriedExecution.id != null
            }
        when:
            def responseExec1 = get("/execution/${responseExec.retriedExecution.id}", Map)
        then:
            verifyAll {
                responseExec1.status == 'timedout'
                responseExec.retriedExecution != null
            }
    }

    def generatePath() {
        def xmlJob = "<joblist>\n" +
                "   <job>\n" +
                "      <name>scheduled job</name>\n" +
                "      <group>api-test/job-run-scheduled</group>\n" +
                "      <uuid>api-test-job-run-scheduled</uuid>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <context>\n" +
                "          <project>xml-project-name</project>\n" +
                "      </context>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <schedule>\n" +
                "        <time hour='P_NH' seconds='P_NS' minute='P_NM' />\n" +
                "        <month month='P_NMO'  day='P_ND' />\n" +
                "        <year year='P_NY' />\n" +
                "      </schedule>\n" +
                "      <timeZone>GMT</timeZone>\n" +
                "\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>xml-args</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def nowDate = LocalDateTime.now(ZoneId.of("GMT"))
        def upDate = nowDate.plusSeconds(10)

        def ny = upDate.year
        def nmo = upDate.monthValue
        def nd = upDate.dayOfMonth
        def nh = upDate.hour
        def nm = upDate.minute
        def ns = upDate.second

        def xmlJobAux = xmlJob.replaceAll('xml-args', 'echo hello there')
                .replaceAll('xml-project-name', PROJECT_NAME)
                .replaceAll('P_NY', ny.toString())
                .replaceAll('P_NMO', String.format('%02d', nmo))
                .replaceAll('P_ND', String.format('%02d', nd))
                .replaceAll('P_NH', String.format('%02d', nh))
                .replaceAll('P_NM', String.format('%02d', nm))
                .replaceAll('P_NS', String.format('%02d', ns))
        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlJobAux
        tempFile.deleteOnExit()
        tempFile.path
    }

    def generatePath1() {
        def xmlJob = "<joblist>\n" +
                "   <job>\n" +
                "      <name>cli job</name>\n" +
                "      <group>api-test/job-run-timeout</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <timeout>3s</timeout>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>xml-args</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def xmlJobAux = xmlJob.replaceAll('xml-args', 'echo hello there ; sleep 30')

        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlJobAux
        tempFile.deleteOnExit()
        tempFile.path
    }

    def generatePath2() {
        def xmlJob = "<joblist>\n" +
                "   <job>\n" +
                "      <name>cli job</name>\n" +
                "      <group>api-test/job-run-timeout-retry</group>\n" +
                "      <description></description>\n" +
                "      <loglevel>INFO</loglevel>\n" +
                "      <timeout>3s</timeout>\n" +
                "      <retry>1</retry>\n" +
                "      <dispatch>\n" +
                "        <threadcount>1</threadcount>\n" +
                "        <keepgoing>true</keepgoing>\n" +
                "      </dispatch>\n" +
                "      <sequence>\n" +
                "        <command>\n" +
                "        <exec>xml-args</exec>\n" +
                "        </command>\n" +
                "      </sequence>\n" +
                "   </job>\n" +
                "</joblist>"

        def xmlJobAux = xmlJob.replaceAll('xml-args', 'echo hello there ; sleep 30')

        def tempFile = File.createTempFile("temp", ".xml")
        tempFile.text = xmlJobAux
        tempFile.deleteOnExit()
        tempFile.path
    }
}
