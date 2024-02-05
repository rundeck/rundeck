package org.rundeck.tests.functional.api.project

import org.apache.commons.compress.utils.IOUtils
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ExportSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "test-project-export"(){
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
