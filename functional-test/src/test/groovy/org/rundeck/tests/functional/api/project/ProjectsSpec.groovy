package org.rundeck.tests.functional.api.project

import okhttp3.Headers
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class ProjectsSpec extends BaseContainer {

    final static String PROJECT_NAME_1 = "test-project_1"

    final static String PROJECT_NAME_2 = "test-project_2"

    final static String PROJECT_NAME_3 = "test-project_3"


    def setupSpec() {
        setupProject(PROJECT_NAME_1)
        setupProject(PROJECT_NAME_2)
        setupProject(PROJECT_NAME_3)
    }

    def "get all projects"() {
        when:
        def response = client.doGet("/projects")

        then:
        verifyAll {
            response.code()== 200
            response.message() == "OK"
            def json = jsonValue(response.body(), Object)

            json[0].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_1)
            json[0].name == (PROJECT_NAME_1)
            json[0].description == ""
            json[0].created != ""

            json[1].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_2)
            json[1].name == (PROJECT_NAME_2)
            json[1].description == ""
            json[1].created != ""

            json[2].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_3)
            json[2].name == (PROJECT_NAME_3)
            json[2].description == ""
            json[2].created != ""
        }
    }

    def "get all projects with json header"() {
        when:
        Headers header = new Headers.Builder()
                .add("Accept", "application/json")
                .build()
        def response = client.doGetAddHeaders("/projects",header)

        then:
        verifyAll {
            response.code()== 200
            response.message() == "OK"
            def json = jsonValue(response.body(), Object)

            json[0].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_1)
            json[0].name == (PROJECT_NAME_1)
            json[0].description == ""
            json[0].created != ""

            json[1].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_2)
            json[1].name == (PROJECT_NAME_2)
            json[1].description == ""
            json[1].created != ""

            json[2].url.containsIgnoreCase(client.finalApiVersion.toString()+"/project/"+PROJECT_NAME_3)
            json[2].name == (PROJECT_NAME_3)
            json[2].description == ""
            json[2].created != ""
        }
    }


}

