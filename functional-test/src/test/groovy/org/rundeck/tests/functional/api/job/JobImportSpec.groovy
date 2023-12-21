package org.rundeck.tests.functional.api.job

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobImportSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }
    static final List JOB_JSON_MAP = [
        [
            "executionEnabled"  : false,
            "loglevel"          : "INFO",
            "name"              : "JobImportSpec-testjob1",
            "nodeFilterEditable": false,
            "options"           : [
                [
                    "label"   : "Option 1",
                    "name"    : "opt1",
                    "required": true
                ],
            ],
            "scheduleEnabled"   : true,
            "schedules"         : [],
            "sequence"          : [
                "commands" : [
                    [
                        "script": 'echo "${option.opt1}"'
                    ],
                ],
                "keepgoing": false,
                "strategy" : "node-first"
            ],
        ]
    ]

    def "import job json format fails for version<44"() {
        given:
            def client = getClient()
            client.apiVersion = version
        when:
            def response = client.doPost(
                "/project/${PROJECT_NAME}/jobs/import?dupeOption=update&uuidOption=remove",
                JOB_JSON_MAP
            )
        then:
            verifyAll {
                !response.successful
                response.code() == 400
                def json = client.jsonValue(response.body(), Map)
                json.errorCode == 'api.error.api-version.unsupported'
            }
        where:
            version << [14, 43]
    }

    def "import job json format succeeds for version>=44"() {
        given:
            def client = getClient()
            client.apiVersion = version
        when:
            def response = client.doPost(
                "/project/${PROJECT_NAME}/jobs/import?dupeOption=update&uuidOption=remove",
                JOB_JSON_MAP
            )
        then:
            verifyAll {
                response.successful
                response.code() == 200
                def json = client.jsonValue(response.body(), Map)
                json.succeeded != null
                json.succeeded.size() == 1
            }

        where:
            version << [44, 46]
    }
}
