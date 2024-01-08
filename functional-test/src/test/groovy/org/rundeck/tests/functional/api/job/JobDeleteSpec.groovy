package org.rundeck.tests.functional.api.job

import org.rundeck.util.container.BaseContainer

class JobDeleteSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "DELETE for /api/job/{id}"() {
        given:
            def client = getClient()
            def pathXmlFile = getClass().getResource("/temp-files/api-test-execution-state.xml").getPath()
            def xmlProjectContent = new File(pathXmlFile).text
            def xmlProject = xmlProjectContent.replaceAll('xml-project-name', PROJECT_NAME)
                    .replaceAll('xml-args', "echo hello there")
            new File(pathXmlFile).text = xmlProject
        when:
            def jobId = jobImportFile(PROJECT_NAME, pathXmlFile).succeeded[0].id
        then:
            def response = doDelete("/job/${jobId}")
    }

}
