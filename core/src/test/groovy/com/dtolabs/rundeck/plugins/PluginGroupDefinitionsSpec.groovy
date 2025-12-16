package com.dtolabs.rundeck.plugins

import spock.lang.Specification

class PluginGroupDefinitionsSpec extends Specification {

    def "getGroupDefinition returns definition for valid group"() {
        when:
        def result = PluginGroupDefinitions.getGroupDefinition(PluginGroupConstants.GROUP_AWS_S3)

        then:
        result != null
        result.name == PluginGroupConstants.GROUP_AWS_S3
        result.representativePluginName == null // Auto-discovery by default
    }

    def "getGroupDefinition returns OTHER for unknown group"() {
        when:
        def result = PluginGroupDefinitions.getGroupDefinition("UnknownGroup")

        then:
        result != null
        result.name == PluginGroupConstants.GROUP_OTHER
    }

    def "getRepresentativePluginName returns null for auto-discovery groups"() {
        when:
        def result = PluginGroupDefinitions.getRepresentativePluginName(PluginGroupConstants.GROUP_AWS_S3)

        then:
        result == null
    }

    def "getRepresentativePluginName returns null for unknown groups"() {
        when:
        def result = PluginGroupDefinitions.getRepresentativePluginName("UnknownGroup")

        then:
        result == null
    }

    def "all predefined groups have definitions"() {
        expect:
        PluginGroupDefinitions.getGroupDefinition(groupName) != null

        where:
        groupName << [
            PluginGroupConstants.GROUP_AWS,
            PluginGroupConstants.GROUP_AWS_S3,
            PluginGroupConstants.GROUP_AWS_CLOUDWATCH,
            PluginGroupConstants.GROUP_AWS_LAMBDA,
            PluginGroupConstants.GROUP_AWS_RDS,
            PluginGroupConstants.GROUP_AWS_VM,
            PluginGroupConstants.GROUP_AZURE,
            PluginGroupConstants.GROUP_GCP,
            PluginGroupConstants.GROUP_ORACLE,
            PluginGroupConstants.GROUP_ANSIBLE,
            PluginGroupConstants.GROUP_KUBERNETES,
            PluginGroupConstants.GROUP_PS1,
            PluginGroupConstants.GROUP_VMWARE,
            PluginGroupConstants.GROUP_PAGERDUTY,
            PluginGroupConstants.GROUP_SERVICENOW_CHANGE,
            PluginGroupConstants.GROUP_SERVICENOW_INCIDENT,
            PluginGroupConstants.GROUP_JIRA,
            PluginGroupConstants.GROUP_DATADOG,
            PluginGroupConstants.GROUP_SENSU,
            PluginGroupConstants.GROUP_SUMO_LOGIC,
            PluginGroupConstants.GROUP_OTHER
        ]
    }
}
