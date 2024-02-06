package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ProjectsSpec extends BaseContainer{
    def setupSpec() {
        startEnvironment()
        setupProject("ProjectsSpec", null)
    }

    def "Test Projects api" () {
        given:
        def response = getProjects()
        expect:
        response.size() > 0
    }

}
