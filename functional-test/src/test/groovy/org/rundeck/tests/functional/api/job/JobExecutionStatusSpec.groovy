package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

@APITest
@Stepwise
class JobExecutionStatusSpec extends BaseContainer {

    @Shared String jobId
    @Shared int execId

    def setupSpec() {
        startEnvironment()
        setupProject()
        def pathFile = JobUtils.updateJobFileToImport("api-test-execution-state-2.xml", PROJECT_NAME)
        jobId = JobUtils.jobImportFile(PROJECT_NAME,pathFile,client).succeeded[0].id
    }

    def "job/id/run should succeed"() {
        when:
            def jobRun = JobUtils.executeJobWithArgs(jobId, client, "-opt2 a")
            execId = jsonValue(jobRun.body()).id as Integer
        then:
            verifyAll {
                execId > 0
            }
    }

    def "job/id/executions?status=test+status+code with 1 results"() {
        when:
            sleep 5000
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

}
