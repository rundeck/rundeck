package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@APITest
class JobExecutionSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }



}
