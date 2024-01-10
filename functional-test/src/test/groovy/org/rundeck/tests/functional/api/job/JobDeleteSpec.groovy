package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobDeleteSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "DELETE for /api/job/{id}"() {
        given:
            def client = getClient()
            def pathXmlFile = getClass().getResource("/temp-files/api-test-execution-state.xml").getPath()
            def xmlContent = new File(pathXmlFile).text
                    .replaceAll('xml-project-name', PROJECT_NAME)
                    .replaceAll('xml-args', "echo hello there")
            new File(pathXmlFile).text = xmlContent
        when:
            def jobId = jobImportFile(pathXmlFile).succeeded[0].id
        then:
            def responseDelete = doDelete("/job/${jobId}")
        when:
            def responseGet = doGet("/job/${jobId}")
        then:
            verifyAll {
                responseDelete.code() == 204
                responseGet.code() == 404
                def json = client.jsonValue(responseGet.body(), Map)
                json.message == "Job ID does not exist: ${jobId}"
            }
    }

}
