/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rundeckapp.init

import spock.lang.Specification


class RundeckInitializerSpec extends Specification {
    def "init server uuid - serverId file exists"() {
        setup:
        File configDir = File.createTempDir()
        configDir.deleteOnExit()
        File serverIdFile = new File(configDir,"serverId")
        serverIdFile.createNewFile()
        if(currentServerUuid) {
            serverIdFile << currentServerUuid
        }
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR,configDir.absolutePath)

        when:
        RundeckInitializer initializer = new RundeckInitializer()
        initializer.initServerUuid()

        then:
        sysPropServerIdShouldEqual == (currentServerUuid == System.getProperty("rundeck.server.uuid"))

        cleanup:
        System.clearProperty("rundeck.server.uuid")
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)

        where:
        currentServerUuid                       | sysPropServerIdShouldEqual
        null                                    | false
        "684512b2-ce16-490f-9652-b9f8b9a6937b"  | true

    }

    def "init server uuid - no serverId file migrate from framework props"() {
        setup:
        String serverUuid = "aaaabbbb-cccc-dddd-eeee-ffffffffffff"
        File rdBaseDir = File.createTempDir()
        File etcDir = new File(rdBaseDir,"etc")
        etcDir.mkdir()
        File fwkProps = new File(etcDir,"framework.properties")
        fwkProps.createNewFile()
        fwkProps.withPrintWriter {
            it.println("rundeck.server.uuid="+serverUuid)
            it.flush()
        }
        File configDir = File.createTempDir()


        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR,rdBaseDir.absolutePath)
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR,configDir.absolutePath)

        when:
        RundeckInitializer initializer = new RundeckInitializer()
        initializer.initServerUuid()
        File serverId = new File(configDir,"serverId")

        then:
        serverId.exists()
        serverId.text.trim() == serverUuid
        serverUuid == System.getProperty("rundeck.server.uuid")

        cleanup:
        System.clearProperty("rundeck.server.uuid")
        rdBaseDir.delete()
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR)
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)

    }

    def "init server uuid - no serverId file, no framework props - populate with new random"() {
        setup:
        File configDir = File.createTempDir()
        configDir.deleteOnExit()

        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR,configDir.absolutePath)
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR,configDir.absolutePath)

        when:
        RundeckInitializer initializer = new RundeckInitializer()
        initializer.initServerUuid()
        File serverId = new File(configDir,"serverId")

        then:
        serverId.exists()
        serverId.text.trim().size() == 36
        System.getProperty("rundeck.server.uuid").size() == 36

        cleanup:
        System.clearProperty("rundeck.server.uuid")
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR)
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)

    }
}
