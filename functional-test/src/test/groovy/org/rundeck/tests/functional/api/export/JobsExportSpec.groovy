package org.rundeck.tests.functional.api.export

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class JobsExportSpec extends BaseContainer {

    def setupSpec() {
        startEnvironment()
        setupProject()
    }

    def "export RunDeck Jobs in jobs.json format"() {
        when:
            def export = doGet("/project/${PROJECT_NAME}/jobs/export?format=json")
        then:
            verifyAll {
                def header = export.headers().get("Content-Type")
                header != null
                header == "application/json" || "application/json;charset=utf-8"
                def json = jsonValue(export.body(), List)
                json.size() > 0
            }
    }

    def "export RunDeck Jobs in jobs default (json) format"() {
        when:
            def export = doGet("/project/${PROJECT_NAME}/jobs/export")
        then:
            verifyAll {
                def header = export.headers().get("Content-Type")
                header != null
                header == "application/json" || "application/json;charset=utf-8"
                def json = jsonValue(export.body(), List)
                json.size() > 0
            }
    }

    def "export RunDeck Jobs in jobs.xml format"() {
        when:
            def export = doGet("/project/${PROJECT_NAME}/jobs/export?format=xml")
        then:
        verifyAll {
            def header = export.headers().get("Content-Type")
            header != null
            header == "text/xml" || "text/xml;charset=utf-8"
            def xml = export.body().string()
            xml.contains("<joblist>")
            xml.count("<job>") > 0
        }
    }

    def "export RunDeck Jobs in jobs.yaml format"() {
        when:
            def export = doGet("/project/${PROJECT_NAME}/jobs/export?format=yaml")
        then:
            verifyAll {
                def header = export.headers().get("Content-Type")
                header != null
                header == "text/yaml" || "text/yaml;charset=utf-8"
                def xml = export.body().string()
                xml.count(" id:") > 0
            }
    }

}
