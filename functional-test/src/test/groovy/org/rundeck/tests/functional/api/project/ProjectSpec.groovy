package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import spock.lang.Stepwise

@Stepwise
@APITest
class ProjectSpec extends BaseContainer{

    final static String PROJECT_NAME = "test-project"


    def setupSpec() {
        setupProject(PROJECT_NAME)
    }


    def "get project by name"() {
        when:
        def response = client.doGet("/project/"+PROJECT_NAME)

        then:
        verifyAll {
            response.code()== 200
            response.message() == "OK"
            def json = jsonValue(response.body(), Map)
            json
            json.url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME)
            json.name == (PROJECT_NAME)
            json.description == ""
            json.created != ""
            json.config.size() == 5

        }
    }

    def "get project wrong name"() {
        when:
        def fakeProject = "NO_EXIST"
        def response = client.doGet("/project/"+fakeProject)

        then:
        verifyAll {
            response.code()== 404
            response.message() == "Not Found"
            def json = jsonValue(response.body(), Map)
            json.errorCode == "api.error.project.missing"
            json.apiversion == client.finalApiVersion
            json.error == true
            json.message == "Project does not exist: "+fakeProject
        }
    }

    def "delete project by name"() {
        when:

        def response = client.doDelete("/project/"+PROJECT_NAME+"/?deferred=false")

        then:
        response.code()== 204
        response.message() == "No Content"
        def newRequest = client.doGet("/project/"+PROJECT_NAME)
        verifyAll {
            newRequest.code()== 404
            newRequest.message() == "Not Found"
            def json = jsonValue(newRequest.body(), Map)
            json.errorCode == "api.error.project.missing"
            json.apiversion == client.finalApiVersion
            json.error == true
            json.message == "Project does not exist: "+PROJECT_NAME
        }
    }
}




