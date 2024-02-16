package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject("TestJobs", "/projects-import/TestJobs.zip")
    }

    def "Runs workflow steps" () {
        given:
            def adhoc = runJobAndWait('9b43e4ab-7ff2-4159-9fc7-7437901914f7', ["options":["opt2": "a"]])
            def entries = adhoc['entries'].collect { it.log }
            def node = adhoc['entries'].findResult { it.node }
        expect:
            entries == [
                    'hello there',
                    'option opt1: testvalue',
                    'option opt1: testvalue',
                     "node: $node",
                    'option opt2: a',
                    'this is script 2, opt1 is testvalue',
                    'hello there',
                    'this is script 1, opt1 is testvalue',
            ]
    }

}
