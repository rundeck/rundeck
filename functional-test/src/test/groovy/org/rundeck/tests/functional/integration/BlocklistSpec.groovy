package org.rundeck.tests.functional.integration

import org.rundeck.util.annotations.BlocklistTest
import org.rundeck.util.container.BaseContainer

@BlocklistTest
class BlocklistSpec extends BaseContainer {


    public static final int EXPECTED_PLUGIN_LIST_SIZE = 62
    static List<String> BLOCKED_NAMES = [
            'cyberark',
            'openssh',
            'ansible',
            'copyfile',
            'localexec',
            'rundeck-script'
    ]

    static Map<String, List<String>> BLOCKED_PROVIDERS = [
            "NodeExecutor": [
                    "local",
                    "sshj-ssh"
            ],
            "FileCopier": [
                    "script-copy",
                    "sshj-scp"
            ],
            "ResourceModelSource": [
                    "directory",
                    "file",
                    "local",
                    "script"
            ]
    ]

    def "test blocklist does not contain blocked plugins"() {
        given:
        def isBlockedPlugin = { plugin ->
            BLOCKED_PROVIDERS.containsKey(plugin["service"]) &&
                    BLOCKED_PROVIDERS.getOrDefault(plugin["service"], List.of()).contains(plugin["name"])
        }

        when:
        List<Map> response = get("/plugin/list", List)
        def artifactNames = response*.artifactName.collect { it.toString().toLowerCase() }

        then: "plugins blocked by jar file are not listed"
        verifyAll {
            for (String name : BLOCKED_NAMES) {
                artifactNames.find { it.contains(name) } == null
            }
        }

        and: "individual plugins blocked by name are not listed"
        response.forEach { plugin -> assert !isBlockedPlugin(plugin) }
    }

    def "test expected plugin count"() {
        when:
        List<Map> response = get("/plugin/list", List)
        then:
        response.size() == EXPECTED_PLUGIN_LIST_SIZE
    }
}
