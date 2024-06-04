package org.rundeck.tests.functional.integration

import org.rundeck.util.annotations.BlocklistTest
import org.rundeck.util.container.BaseContainer

@BlocklistTest
class BlocklistSpec extends BaseContainer {


    public static final int EXPECTED_PLUGIN_LIST_SIZE = 65
    static List<String> BLOCKED_NAMES = [
        'cyberark',
        'openssh',
        'ansible',
        'copyfile',
        'localexec',
        'rundeck-script'
    ]

    def "test blocklist does not contain blocked plugins"() {
        when:
            List<Map> response = get("/plugin/list", List)
            def artifactNames = response*.artifactName.collect { it.toString().toLowerCase() }
        then:
            verifyAll {
                for (String name : BLOCKED_NAMES) {
                    artifactNames.find { it.contains(name) } == null
                }
            }
    }

    def "test expected plugin count"() {
        when:
            List<Map> response = get("/plugin/list", List)
        then:
            response.size() == EXPECTED_PLUGIN_LIST_SIZE
    }
}
