package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.common.jobs.JobUtils
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

@APITest
class JobSpec extends BaseContainer {

    private static final def MAPPER = new ObjectMapper()

    def setupSpec() {
        startEnvironment()
        setupProjectArchiveDirectoryResource("TestJobs", "/projects-import/TestJobs")
    }

    def "Runs workflow steps" () {
        given:
            def r = JobUtils.executeJobWithOptions('9b43e4ab-7ff2-4159-9fc7-7437901914f7', client, ["options":["opt2": "a"]])
            assert r.successful
            Execution execution = MAPPER.readValue(r.body().string(), Execution.class)

            waitForExecutionStatus(execution.id)
            String fullLog = JobUtils.getExecutionOutput(execution.id, client)
        expect:
            [
                    'hello there',
                    'option opt1: testvalue',
                    'option opt1: testvalue',
                    'option opt2: a',
                    'this is script 2, opt1 is testvalue',
                    'hello there',
                    'this is script 1, opt1 is testvalue',
            ].every({fullLog.contains(it)})
    }
}
