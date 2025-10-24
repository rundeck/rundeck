package org.rundeck.tests.functional.api.nodes

import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer

import java.nio.charset.Charset

@APITest
class NodesSpec extends BaseContainer {

    public static final String PROJECT = 'NodesSpec'

    def setupSpec() {
        startEnvironment()
        setupProject(
            PROJECT,
            [
                "config": [
                    "resources.source.1.type"         : "local",
                    "resources.source.2.config.count" : "2",
                    "resources.source.2.config.prefix": "NodesSpec-",
                    "resources.source.2.config.tags"  : "test,demo",
                    "resources.source.2.config.attrs" : "foo=bar",
                    "resources.source.2.type"         : "stub",
                    "resources.source.3.config.count" : "3",
                    "resources.source.3.config.prefix": "abc-",
                    "resources.source.3.config.tags"  : "abc,common",
                    "resources.source.3.type"         : "stub",
                    "resources.source.4.config.count" : "3",
                    "resources.source.4.config.suffix": "-xyz",
                    "resources.source.4.config.tags"  : "xyz,common",
                    "resources.source.4.type"         : "stub",
                    "resources.source.5.config.count" : "3",
                    "resources.source.5.config.prefix": "blah-",
                    "resources.source.5.config.attrs" : "blah=blahblah,foo=bar",
                    "resources.source.5.config.tags"  : "hijk",
                    "resources.source.5.type"         : "stub",
                ]]
        )
    }

    def cleanupSpec() {
        deleteProject(PROJECT)
    }

    def "get nodes for project"() {
        when:
            def response = client.get("/project/$PROJECT/resources", Map)

        then:
            verifyAll {
                response.size() == 12
            }
    }

    def "get nodes for filtering by tag"() {
        when:
            def response = client
                .get("/project/$PROJECT/resources?filter=${URLEncoder.encode(filter, Charset.defaultCharset())}", Map)

        then:
            verifyAll {
                response.size() == expectedCount
            }
        where:
            filter          | expectedCount
            "tags:test"     | 2
            "tags:demo"     | 2
            "tags:xyz"      | 3
            "tags:common"   | 6
            "tags:abc"      | 3
            "name:xyz.*"    | 0
            "name:.*xyz"    | 3
            "name:blah.*"   | 0
            "name:abc.*"    | 3
            "name:.*abc"    | 0
            "blah:blahblah" | 3
            "foo:bar"       | 5
    }
}
