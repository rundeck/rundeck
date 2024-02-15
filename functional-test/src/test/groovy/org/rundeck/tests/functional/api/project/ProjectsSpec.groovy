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
            response.code() == 200
            response.message() == "OK"
            def json = jsonValue(response.body(), Object)
            json.size() >= 3
            int coincidence
            json.each { project ->
                if (project.name == PROJECT_NAME_1){
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_1))
                        coincidence++
                }
                if (project.name == PROJECT_NAME_2) {
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_2))
                        coincidence++
                }
                if (project.name == PROJECT_NAME_3) {
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_3))
                        coincidence++
                }
            }
            if (coincidence != 3) false

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
            response.code() == 200
            response.message() == "OK"
            def json = jsonValue(response.body(), Object)
            json.size() >= 3
            int coincidence
            json.each { project ->
                if (project.name == PROJECT_NAME_1){
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_1))
                        coincidence++
                }
                if (project.name == PROJECT_NAME_2) {
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_2))
                        coincidence++
                }
                if (project.name == PROJECT_NAME_3) {
                    if (project.url.containsIgnoreCase(client.finalApiVersion.toString() + "/project/" + PROJECT_NAME_3))
                        coincidence++
                }
            }
            if (coincidence != 3) false

        }
    }


}

