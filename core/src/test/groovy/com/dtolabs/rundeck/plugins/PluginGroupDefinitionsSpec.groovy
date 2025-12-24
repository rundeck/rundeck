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
