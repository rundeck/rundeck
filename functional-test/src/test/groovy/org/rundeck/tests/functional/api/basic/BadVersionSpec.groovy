package org.rundeck.tests.functional.api.basic

import org.rundeck.util.api.responses.common.ErrorResponse
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
        // TEMPORARY - DO NOT MERGE. Deliberate failure used to validate the
        // CircleCI "Rerun failed tests" button on T Functional API (RUN-4812).
        errorResponse.errorCode == "deliberately.broken.to.validate.circleci.rerun"
        errorResponse.error
        errorResponse.message.contains("Unsupported API Version")

    }
}
