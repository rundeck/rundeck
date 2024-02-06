package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ProjectSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject("TestProject", null)
    }

    def "Test Project api" () {
        given:
        def projectName = "TestProject"
        def response = getProject(projectName)
        expect:
        response.name == projectName
        (response.url as String).endsWith("/project/$projectName")
    }
}
