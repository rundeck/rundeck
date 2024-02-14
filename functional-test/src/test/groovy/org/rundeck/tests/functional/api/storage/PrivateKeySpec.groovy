package org.rundeck.tests.functional.api.storage

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import spock.lang.Stepwise

@APITest
@Stepwise
class PrivateKeySpec extends BaseContainer {

    def setupSpec() {
        setupProject()
    }
    final String PRIVATE_KEY_PATH = "functional-test/src/test/resources/test-files/privateKey.private"

    final String CONTENT_TYPE = "application/octet-stream"

    final String RUNDECK_KEY_TYPE = "private"

    final String PRIVATE_KEYS_DIRECTORY = "functional-test/src/test/resources/test-files"

    def "test POST storage/keys"() {

        when:
        def response = client.doPostWithContentTypeWithoutBody("/storage/keys/"+PRIVATE_KEY_PATH,CONTENT_TYPE)

        then:
        verifyAll {
            response.successful
            response.code() == 201
            def json = jsonValue(response.body(), Map)
            json.path == "keys/"+PRIVATE_KEY_PATH
            json.type == "file"
            json.url.toString().containsIgnoreCase("/storage/keys/"+PRIVATE_KEY_PATH)
            json.name == "privateKey.private"
            json.meta.getAt("Rundeck-content-type") == CONTENT_TYPE
            json.meta.getAt("Rundeck-key-type") == RUNDECK_KEY_TYPE
            json.meta.findAll().size() == 7
        }
    }

    def "test GET storage/keys"() {

        when:
        def response = client.doGet("/storage/keys/"+PRIVATE_KEY_PATH)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.path == "keys/"+PRIVATE_KEY_PATH
            json.type == "file"
            json.url.toString().containsIgnoreCase("/storage/keys/"+PRIVATE_KEY_PATH)
            json.name == "privateKey.private"
            json.meta.getAt("Rundeck-content-type") == CONTENT_TYPE
            json.meta.getAt("Rundeck-key-type") == RUNDECK_KEY_TYPE
            json.meta.findAll().size() == 7
        }

    }

    def "test GET storage/keys wrong api token"() {
        given:
        def client = clientWithToken("wrongToken")
        when:
        def response = client.doGet("/storage/keys/"+PRIVATE_KEY_PATH)

        then:
        verifyAll {
            !response.successful
            response.code() == 403
            def json = jsonValue(response.body(), Map)
            json.error == true
            json.errorCode == "unauthorized"
            json.message.toString().containsIgnoreCase("(Token:wrong****)")
        }
    }

    def "test GET storage/keys list private keys"() {

        when:
        def response = client.doGet("/storage/keys/"+PRIVATE_KEYS_DIRECTORY)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.path=="keys/functional-test/src/test/resources/test-files"
            json.url.toString().containsIgnoreCase("/storage/keys/"+PRIVATE_KEYS_DIRECTORY)
            json.type == "directory"
            json.meta.findAll().size()==0
            verifyAll {
                json.resources[0].path == "keys/"+PRIVATE_KEY_PATH
                json.resources[0].type == "file"
                json.resources[0].url.toString().containsIgnoreCase("storage/keys/"+PRIVATE_KEY_PATH)
                json.resources[0].name == "privateKey.private"
                json.resources[0].meta.getAt("Rundeck-content-type") == CONTENT_TYPE
                json.resources[0].meta.getAt("Rundeck-key-type") == RUNDECK_KEY_TYPE
                json.resources[0].meta.findAll().size() == 7
            }
        }
    }

    def "test DELETE storage/key"() {
        when:
        def response = client.doDelete("/storage/keys/"+PRIVATE_KEY_PATH)

        then:
        verifyAll {
            response.code()== 204
            response.message() == "No Content"
        }
        def newRequest = client.doGet("/storage/keys/"+PRIVATE_KEYS_DIRECTORY)
        newRequest.code() == 404
        def json = jsonValue(newRequest.body(), Map)
        json.error.containsIgnoreCase("resource not found: /keys/functional-test/src/test/resources/test-files")
    }
}

