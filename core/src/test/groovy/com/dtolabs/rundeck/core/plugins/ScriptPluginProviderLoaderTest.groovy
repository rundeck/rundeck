/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INCOMPATIBLE
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INVALID
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.VALID

class ScriptPluginProviderLoaderTest extends Specification {

    def "LoadMetadataYaml with null date"() {
        setup:
        File tmpCacheDir = File.createTempDir()

        when:
        String pluginName = "Test script plugin"
        ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
        def pluginYaml = createPluginYaml(pluginName,"1.2")
        def meta = loader.loadMetadataYaml(pluginYaml)
        def dte = loader.getPluginDate()

        then:
        noExceptionThrown()
        meta.name == pluginName
        meta.rundeckPluginVersion == "1.2"
        dte == null
    }

    def "LoadMetadataYaml"() {
        setup:
        File tmpCacheDir = File.createTempDir()

        when:
        String pluginName = "Test script plugin"
        ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
        def pluginYaml = createPluginYaml(pluginName,"1.2")
        def meta = loader.loadMetadataYaml(pluginYaml)

        then:
        meta.name == pluginName
        meta.rundeckPluginVersion == "1.2"
        meta.date == null
    }

    @Unroll
    def "ValidatePluginMeta"() {
        setup:
        File fakeFile = new File("/tmp/fake-plugin.yaml")
        when:

        def validation = ScriptPluginProviderLoader.validatePluginMeta(new PluginMeta(pluginMeta),fakeFile)

        then:
        logResult == validation.messages
        validation.state.valid == expectation
        validation.state == state

        where:
        expectation |state                                                                             | pluginMeta | logResult
        false       |INVALID| [rundeckPluginVersion: "1.2", pluginDefs: []] | ["'name' not found in metadata", "'version' not found in metadata"]
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "1.2", pluginDefs: []]                | ["'version' not found in metadata"]
        true        |VALID| [name:"Test script",rundeckPluginVersion: "1.2", version:"1.0", pluginDefs: []] | []
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", pluginDefs: []] | ["No targetHostCompatibility property specified in metadata","rundeckCompatibilityVersion cannot be null in metadata"]
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", pluginDefs: []] | ["rundeckCompatibilityVersion cannot be null in metadata"]
        false       |INCOMPATIBLE| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"1.0", pluginDefs: []] | ["Plugin is not compatible with this version of Rundeck"]
        true        |VALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"3.x", pluginDefs: []] | []
        true        |VALID| [name:"test-script",display:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"3.x", pluginDefs: []] | []

    }


    ByteArrayInputStream createPluginYaml(String pluginName, String pluginVersion, Map props = [:]) {
        StringBuilder yaml = new StringBuilder()
        yaml.append("name: ${pluginName}\n")
        yaml.append("rundeckPluginVersion: ${pluginVersion}\n")
        props.each { k, v ->
            yaml.append("$k : ${v}\n")
        }
        new ByteArrayInputStream(yaml.toString().bytes)
    }
}
