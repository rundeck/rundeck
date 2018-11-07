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
import org.apache.log4j.Logger
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class ScriptPluginProviderLoaderTest extends Specification {
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
    }

    def "ValidatePluginMeta"() {
        setup:
        File fakeFile = new File("/tmp/fake-plugin.yaml")
        when:
        def logger = new FakeLogger()
        Field logField = ScriptPluginProviderLoader.getDeclaredField("log")
        logField.accessible = true
        removeFinalModifier(logField)
        logField.set(null,logger)
        boolean isvalid = ScriptPluginProviderLoader.validatePluginMeta(new PluginMeta(pluginMeta),fakeFile)

        then:
        logResult == logger.logs
        isvalid == expectation

        where:
        expectation | pluginMeta | logResult
        false       | [rundeckPluginVersion: "1.2", pluginDefs: []] | ["name not found in metadata: /tmp/fake-plugin.yaml", "version not found in metadata: /tmp/fake-plugin.yaml"]
        false       | [name:"Test script",rundeckPluginVersion: "1.2", pluginDefs: []] | ["version not found in metadata: /tmp/fake-plugin.yaml"]
        true        | [name:"Test script",rundeckPluginVersion: "1.2", version:"1.0", pluginDefs: []] | []
        false       | [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", pluginDefs: []] | ["No targetHostCompatibility property specified in metadata: /tmp/fake-plugin.yaml","rundeckCompatibilityVersion cannot be null in metadata: /tmp/fake-plugin.yaml"]
        false       | [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", pluginDefs: []] | ["rundeckCompatibilityVersion cannot be null in metadata: /tmp/fake-plugin.yaml"]
        false       | [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"1.0", pluginDefs: []] | ["Plugin is not compatible with this version of Rundeck in metadata: /tmp/fake-plugin.yaml"]
        true        | [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"3.x", pluginDefs: []] | []

    }

    class FakeLogger extends Logger {

        def logs = []

        FakeLogger() {
            super("fake")
        }


        protected FakeLogger(final String name) {
            super(name)
        }

        @Override
        void error(final Object message) {
            logs.add(message)
        }
    }

    void removeFinalModifier(Field sourceField) {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(sourceField, sourceField.getModifiers() & ~Modifier.FINAL);
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
