package org.rundeck.tests.functional.api.basic

import okhttp3.OkHttpClient
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.RdClient

@APITest
class UnauthorizedSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
    }

    def "unauthorized simple request (json)"() {
        given:
            def client = clientProvider.client
            client = new RdClient(
                    client.baseUrl,
                    new OkHttpClient.Builder().
                            build()
            )
        when:
            def result = client.doGet("/projects")
        then:
        verifyAll {
            !result.successful
            result.code() == 403
            def json = client.jsonValue(result.body(), Map)
            json.errorCode == 'unauthorized'
            json.message == "(unauthenticated) is not authorized for: /api/${client.apiVersion}/projects"
        }
    }

    def "unauthorized token request (header) (json)"() {
        given:
            def client = clientProvider.clientWithToken('invalidtoken')
        when:
            def result = client.doGet("/projects")
        then:
            verifyAll {
                !result.successful
                result.code() == 403
                def json = client.jsonValue(result.body(), Map)
                json.errorCode == 'unauthorized'
                json.message == "(Token:inval****) is not authorized for: /api/${client.apiVersion}/projects"
            }
    }

    def " unauthorized token request (param) (json)"() {
        given:
            def client = clientProvider.client
            client = new RdClient(
                    client.baseUrl,
                    new OkHttpClient.Builder().
                            build()
            )
        when:
            def result = client.doGet("/projects?authtoken=invalidtoken")
        then:
            verifyAll {
                !result.successful
                result.code() == 403
                def json = client.jsonValue(result.body(), Map)
                json.errorCode == 'unauthorized'
                json.message == "(Token:inval****) is not authorized for: /api/${client.apiVersion}/projects"
            }
    }

}
