package org.rundeck.tests.functional.api.resourceModel

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer


@APITest
class AnsibleResourceModelSpec extends BaseContainer{

    public static final String TEST_PROJECT = "ansible-resource-model"
    public static final String ARCHIVE_DIR = "/projects-import/ansible-resource-model"

    def setupSpec() {
        setupProjectArchiveDirectory(
                TEST_PROJECT,
                new File(getClass().getResource(ARCHIVE_DIR).getPath()),
                [
                        "importConfig": "true",
                        "importACL": "true",
                        "importNodesSources": "true",
                        "jobUuidOption": "preserve"
                ]
        )
    }

    def "ansible resource model use specified binary path"(){
        when:
        def response = doGet("/project/${TEST_PROJECT}/resources")

        then:
        verifyAll {
            response != null
            response.successful
        }
        def json = client.jsonValue(response.body(), Map)

        then:
        json.size() == 1
        json.get("rundeck-oss-server-node")
        !json.get("ansible-resource-model-test")
    }
}
