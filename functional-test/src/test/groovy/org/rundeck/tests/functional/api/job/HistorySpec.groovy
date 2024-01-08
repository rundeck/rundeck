package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer


@APITest
class HistorySpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "output from /api/project/NAME/history should be valid"() {
        when:
            def history = doGet("/project/${PROJECT_NAME}/history")
            def expectedKeys = ['starttime', 'endtime', 'title', 'status', 'statusString', 'summary', 'node-summary', 'user', 'project', 'date-started', 'date-ended', 'job', 'execution']
            def expectedNodeSummaryFields = ['succeeded', 'failed', 'total']
        then:
            verifyAll {
                history.successful
                history.code() == 200
                def json = getClient().jsonValue(history.body(), Map)
                json.events.every {
                    it.keySet() == expectedKeys as Set
                    it['node-summary'].keySet() == expectedNodeSummaryFields as Set
                }
            }
    }

    def "/api/history using bad \"end\" date format parameter"() {
        when:
            def history = doGet("/project/${PROJECT_NAME}/history?end=invalidDate")
        then:
            verifyAll {
                history.code() == 400
                def json = getClient().jsonValue(history.body(), Map)
                json.message == "The parameter \"end\" did not have a valid time or dateTime format: invalidDate"
            }
    }

    def "/api/history using bad \"begin\" date format parameter"() {
        when:
            def history = doGet("/project/${PROJECT_NAME}/history?begin=invalidDate")
        then:
            verifyAll {
                history.code() == 400
                def json = getClient().jsonValue(history.body(), Map)
                json.message == "The parameter \"begin\" did not have a valid time or dateTime format: invalidDate"
            }
    }

    def "/api/history using valid \"end\" date format parameter"() {
        when:
            def history = doGet("/project/${PROJECT_NAME}/history?end=2011-02-04T21:38:02Z")
        then:
            verifyAll {
                history.successful
                history.code() == 200
            }
    }

    def "/api/history using valid \"begin\" date format parameter"() {
        when:
            def history = doGet("/project/${PROJECT_NAME}/history?begin=2011-02-04T21:38:02Z")
        then:
            verifyAll {
                history.successful
                history.code() == 200
            }
    }

}
