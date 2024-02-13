package org.rundeck.tests.functional.api.basic

import org.rundeck.tests.functional.api.ResponseModels.ErrorResponse
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class BadVersionSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "test-project-bad-version"() {
        when:
        def projectName = PROJECT_NAME
        def response = doGet("project/${projectName}")
        ErrorResponse errorResponse = ErrorResponse.fromJson(response.body().string())

        then:
        errorResponse.errorCode == "api.error.api-version.unsupported"
        errorResponse.error
        errorResponse.message.contains("Unsupported API Version")

    }
}
