package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

@APITest
@Stepwise
class JobExecutionSpec extends BaseContainer {

    @Shared String jobId
    @Shared String jobId2
    @Shared String jobId3
    @Shared int execId
    @Shared int execId2
    @Shared int execId3

    def setupSpec() {
        startEnvironment()
        setupProject()
        def pathFile = updateFile("job-template-common.xml", null, "test job", "test/api/executions", "Test the /job/ID/executions API endpoint", "echo testing /job/ID/executions result")
        jobId = jobImportFile(pathFile).succeeded[0].id
        def pathFile2 = updateFile("job-template-common.xml", null, "test job", "test/api/executions 2", "Test the /job/ID/executions API endpoint", "/bin/false this should fail")
        jobId2 = jobImportFile(pathFile2).succeeded[0].id
        def pathFile3 = updateFile("job-template-common-2.xml", null, "test job", "test/api/executions 3", "Test the /job/ID/executions API endpoint", "echo this job will be killed...", "sleep 240")
        jobId3 = jobImportFile(pathFile3).succeeded[0].id
    }

    def "job/jobId/executions should succeed with 0 results"() {
        when:
            def response = doGet("/job/${jobId}/executions")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 0
            }
    }

    def "job/id/run should succeed"() {
        when:
            execId = runJob(jobId, ["options":["opt2": "a"]])
        then:
            verifyAll {
                execId > 0
            }
    }

    def "job/id/executions should succeed with 1 results"() {
        when:
            sleep 5000
            def response = doGet("/job/${jobId}/executions")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId
            }
    }

    def "job/id/executions?status=succeeded should succeed with 1 results"() {
        when:
            def response = doGet("/job/${jobId}/executions?status=succeeded")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id == execId
        }
    }

    def "run again job/id/run should succeed"() {
        when:
            execId2 = runJob(jobId, ["options":["opt2": "a"]])
        then:
            verifyAll {
                execId2 > 0
            }
        when: "job/id/executions all results"
            sleep 5000
            def response = doGet("/job/${jobId}/executions")
        then:
         verifyAll {
             response.successful
             response.code() == 200
             def json = jsonValue(response.body())
             json.executions.size() == 2
         }
    }

    def "job/id/executions max param"() {
        when:
            def response = doGet("/job/${jobId}/executions?max=1")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
        }
    }

    def "job/id/executions offset param"() {
        when:
            def response = doGet("/job/${jobId}/executions?max=1&offset=1")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
            }
    }

    def "job/id/executions arbitrary status param"() {
        when:
            def response = doGet("/job/${jobId}/executions?status=some_status")
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 0
            }
    }

    def "job/id/executions invalid id param"() {
        when:
            def response = doGet("/job/fake/executions")
        then:
            verifyAll {
                response.code() == 404
                def json = jsonValue(response.body())
                json.message == "Job ID does not exist: fake"
            }
    }

    def "job/id/executions?status=failed with 1 results"() {
        when:
            execId2 = runJob(jobId2, ["options":["opt2": "a"]])
        then:
            verifyAll {
                execId2 > 0
            }
        when:
            sleep 5000
            def response = doGet("/job/${jobId2}/executions?status=failed")
        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body())
            json.executions.size() == 1
            json.executions[0].id == execId2
        }
    }

    def "job/id/executions?status=running with 1 results"() {
        when:
            execId3 = runJob(jobId3, ["options":["opt2": "a"]])
        then:
            def response = doGet("/job/${jobId3}/executions?status=running")
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId3
            }
    }

    def "job/jobid/executions?status=aborted with 1 results"() {
        when:
            doPost("/execution/${execId3}/abort")
            sleep 5000
        then:
            def response = doGet("/job/${jobId3}/executions?status=aborted")
            verifyAll {
                response.successful
                response.code() == 200
                def json = jsonValue(response.body())
                json.executions.size() == 1
                json.executions[0].id == execId3
            }
    }

}
