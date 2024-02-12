package org.rundeck.tests.functional.api.storage

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class PublicKey extends BaseContainer {

    def setupSpec() {
        setupProject()
    }
    def publicKeyPath = "functional-test/src/test/resources/test-files/publicKey.public"

    def contentType = "application/pgp-keys"

    def rundeckKeyType = "public"

    def "test POST storage/keys"() {

        when:
        def response = client.doPostWithContentTypeWithoutBody("/storage/keys/"+publicKeyPath,contentType)

        then:
        verifyAll {
            response.code() == 201
            response.message() == "Created"
        }
    }

    def "test GET storage/keys"() {

        when:
        def response = client.doGet("/storage/keys/"+publicKeyPath)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.path == "keys/"+publicKeyPath
            json.type == "file"
            json.url.toString().containsIgnoreCase("/storage/keys/"+publicKeyPath)
            json.name == "publicKey.public"
            json.meta.getAt("Rundeck-content-type") == contentType
            json.meta.getAt("Rundeck-key-type") == rundeckKeyType
            json.meta.findAll().size() == 7
        }

    }

    def "test GET storage/keys wrong api token"() {
        given:
        def client = clientWithToken("wrongToken")
        when:
        def response = client.doGet("/storage/keys/"+publicKeyPath)

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
        given:
        def privateKeyPath = "functional-test/src/test/resources/test-files"
        when:
        def response = client.doGet("/storage/keys/"+privateKeyPath)

        then:
        verifyAll {
            response.successful
            response.code() == 200
            def json = jsonValue(response.body(), Map)
            json.path=="keys/"+publicKeyPath
            json.url.toString().containsIgnoreCase("/storage/keys/"+publicKeyPath)
            json.type == "directory"
            json.meta.findAll().size()==0
            verifyAll {
                json.resources[0].path == "keys/"+publicKeyPath
                json.resources[0].type == "file"
                json.resources[0].url.toString().containsIgnoreCase("/storage/keys/"+publicKeyPath)
                json.resources[0].name == "publicKey.public"
                json.resources[0].meta.getAt("Rundeck-content-type") == contentType
                json.resources[0].meta.getAt("Rundeck-key-type") == rundeckKeyType
                json.resources[0].meta.findAll().size() == 7
            }
        }
    }

    def "test DELETE storage/key"() {
        when:
        def response = client.doDelete("/storage/keys/"+publicKeyPath)

        then:
        verifyAll {
            response.code()== 204
            response.message() == "No Content"
        }
    }
}