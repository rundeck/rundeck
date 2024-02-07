package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import org.testcontainers.shaded.org.apache.commons.io.IOUtils

@APITest
class ExportSpec extends BaseContainer{

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-project-export"(){
        given:
        def client = getClient()

        when:
        def exportResponse = client.doGetAcceptAll("/project/${PROJECT_NAME}/export")
        def archiveBytes = IOUtils.toByteArray(exportResponse.body().byteStream()).length
        assert exportResponse.successful

        then:
        exportResponse.headers('Content-Disposition')[0].contains("attachment")
        exportResponse.headers('Content-Disposition')[0].contains("rdproject.jar")
        archiveBytes > 100
    }

}
