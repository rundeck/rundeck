/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins

import spock.lang.Specification

class PluginGroupDefinitionsSpec extends Specification {

    def "getGroupDefinition returns definition for OTHER group"() {
        when:
        def result = PluginGroupDefinitions.getGroupDefinition(PluginGroupConstants.GROUP_OTHER)

        then:
        result != null
        result.name == PluginGroupConstants.GROUP_OTHER
        result.groupIconUrl == null // Auto-discovery by default
    }

    def "getGroupDefinition returns OTHER for unknown group"() {
        when:
        def result = PluginGroupDefinitions.getGroupDefinition("UnknownGroup")

        then:
        result != null
        result.name == PluginGroupConstants.GROUP_OTHER
    }

    def "getGroupIconUrl returns null for auto-discovery groups"() {
        when:
        def result = PluginGroupDefinitions.getGroupIconUrl(PluginGroupConstants.GROUP_OTHER)

        then:
        result == null
    }

    def "getGroupIconUrl returns null for unknown groups"() {
        when:
        def result = PluginGroupDefinitions.getGroupIconUrl("UnknownGroup")

        then:
        result == null
    }
}
