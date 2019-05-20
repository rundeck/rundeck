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
package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import spock.lang.Specification


class ScriptOptionValuesPluginSpec extends Specification {
    def "GetOptionValues"() {
        when:
        ScriptOptionValuesPlugin scriptPlugin = new ScriptOptionValuesPlugin(
                ["getOptionValues":{ cfg ->
                    def options = []
                    options.add([name:"opt1",value:"o1"])
                    options.add([name:"opt2",value:"o2"])
                    return options
                }], new TestDescription()
        )
        def result = scriptPlugin.getOptionValues([:])

        then:
        result.size() == 2
        result[0].name == "opt1"
        result[0].value == "o1"
        result[1].name == "opt2"
        result[1].value == "o2"

    }

    class TestDescription implements Description {

        @Override
        String getName() {
            return "GroovyScriptOptionValues"
        }

        @Override
        String getTitle() {
            return "Groovy Script Option Values"
        }

        @Override
        String getDescription() {
            return "Groovy Script Option Values"
        }

        @Override
        List<Property> getProperties() {
            return []
        }

        @Override
        Map<String, String> getPropertiesMapping() {
            return [:]
        }

        @Override
        Map<String, String> getFwkPropertiesMapping() {
            return [:]
        }
    }
}
