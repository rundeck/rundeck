package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobDeleteSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "DELETE for /api/job/{id}"() {
        given:
            def pathFile = updateJobFileToImport("job-template-common.xml")
        when:
            def jobId = jobImportFile(pathFile).succeeded[0].id
        then:
            def responseDelete = doDelete("/job/${jobId}")
        when:
            def responseGet = doGet("/job/${jobId}")
        then:
            verifyAll {
                responseDelete.code() == 204
                responseGet.code() == 404
                def json = jsonValue(responseGet.body(), Map)
                json.message == "Job ID does not exist: ${jobId}"
            }
    }

}
