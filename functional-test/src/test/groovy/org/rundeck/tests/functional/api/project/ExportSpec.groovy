package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class ExportSpec extends BaseContainer{

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-project-export.sh"(){
        given:
        def apiVersion = 40
        client.apiVersion = apiVersion
        def client = getClient()
        ObjectMapper mapper = new ObjectMapper()

        when:
        def exportResponse = client.doGetAcceptAll("/project/${PROJECT_NAME}/export")
        assert exportResponse.successful

        then:
        exportResponse.headers('Content-Disposition')[0].contains("attachment")
        exportResponse.headers('Content-Disposition')[0].contains("rdproject.jar")
    }

}
