package org.rundeck.tests.functional.api.export

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared


@APITest
class ProjectExportSpec extends BaseContainer {

    @Shared String projectName

    def setupSpec() {
        startEnvironment()
        setupProject()
        def path = updateFile("job-template-common.xml")
    }

    def "export a whole project"() {
        when:
        def data = doGet("/project/${projectName}/export")
        then:
        verifyAll {
            data.successful
            data.code() == 200
        }
    }
}
