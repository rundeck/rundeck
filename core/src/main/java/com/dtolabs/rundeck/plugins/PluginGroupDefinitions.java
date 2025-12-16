package com.dtolabs.rundeck.plugins;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized definitions for plugin groups with optional explicit icon overrides.
 * If no explicit representative plugin is defined, the group icon will be determined
 * by the first plugin in that group with an icon (dynamically at runtime).
 */
public class PluginGroupDefinitions {

    /**
     * Definition of a plugin group with optional representative plugin.
     */
    @Getter
    public static class GroupDefinition {
        private final String name;
        /**
         * The plugin name to use for the group icon, or null to auto-discover
         */
        private final String representativePluginName;

        public GroupDefinition(String name, String representativePluginName) {
            this.name = name;
            this.representativePluginName = representativePluginName;
        }
    }

    private static final Map<String, GroupDefinition> GROUP_DEFINITIONS;

    static {
        Map<String, GroupDefinition> definitions = new HashMap<>();

        // All groups use auto-discovery (null = use first plugin's icon in group)
        // To override, replace null with a specific plugin name: new GroupDefinition(GROUP_AWS, "aws-ec2-step")
        
        // Cloud Providers
        definitions.put(PluginGroupConstants.GROUP_AWS,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS, null));
        definitions.put(PluginGroupConstants.GROUP_AWS_S3,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS_S3, null));
        definitions.put(PluginGroupConstants.GROUP_AWS_CLOUDWATCH,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS_CLOUDWATCH, null));
        definitions.put(PluginGroupConstants.GROUP_AWS_LAMBDA,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS_LAMBDA, null));
        definitions.put(PluginGroupConstants.GROUP_AWS_RDS,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS_RDS, null));
        definitions.put(PluginGroupConstants.GROUP_AWS_VM,
            new GroupDefinition(PluginGroupConstants.GROUP_AWS_VM, null));
        definitions.put(PluginGroupConstants.GROUP_AZURE,
            new GroupDefinition(PluginGroupConstants.GROUP_AZURE, null));
        definitions.put(PluginGroupConstants.GROUP_GCP,
            new GroupDefinition(PluginGroupConstants.GROUP_GCP, null));
        definitions.put(PluginGroupConstants.GROUP_ORACLE,
            new GroupDefinition(PluginGroupConstants.GROUP_ORACLE, null));

        // Automation & Orchestration
        definitions.put(PluginGroupConstants.GROUP_ANSIBLE,
            new GroupDefinition(PluginGroupConstants.GROUP_ANSIBLE, null));
        definitions.put(PluginGroupConstants.GROUP_KUBERNETES,
            new GroupDefinition(PluginGroupConstants.GROUP_KUBERNETES, null));
        definitions.put(PluginGroupConstants.GROUP_PS1,
            new GroupDefinition(PluginGroupConstants.GROUP_PS1, null));
        definitions.put(PluginGroupConstants.GROUP_VMWARE,
            new GroupDefinition(PluginGroupConstants.GROUP_VMWARE, null));

        // Incident & Service Management
        definitions.put(PluginGroupConstants.GROUP_PAGERDUTY,
            new GroupDefinition(PluginGroupConstants.GROUP_PAGERDUTY, null));
        definitions.put(PluginGroupConstants.GROUP_SERVICENOW_CHANGE,
            new GroupDefinition(PluginGroupConstants.GROUP_SERVICENOW_CHANGE, null));
        definitions.put(PluginGroupConstants.GROUP_SERVICENOW_INCIDENT,
            new GroupDefinition(PluginGroupConstants.GROUP_SERVICENOW_INCIDENT, null));
        definitions.put(PluginGroupConstants.GROUP_JIRA,
            new GroupDefinition(PluginGroupConstants.GROUP_JIRA, null));

        // Monitoring & Observability
        definitions.put(PluginGroupConstants.GROUP_DATADOG,
            new GroupDefinition(PluginGroupConstants.GROUP_DATADOG, null));
        definitions.put(PluginGroupConstants.GROUP_SENSU,
            new GroupDefinition(PluginGroupConstants.GROUP_SENSU, null));
        definitions.put(PluginGroupConstants.GROUP_SUMO_LOGIC,
            new GroupDefinition(PluginGroupConstants.GROUP_SUMO_LOGIC, null));

        // Default
        definitions.put(PluginGroupConstants.GROUP_OTHER,
            new GroupDefinition(PluginGroupConstants.GROUP_OTHER, null));

        GROUP_DEFINITIONS = Collections.unmodifiableMap(definitions);
    }

    /**
     * Get group definition by name.
     * @param groupName The group name from PluginGroupConstants
     * @return GroupDefinition or default "Other" group if not found
     */
    public static GroupDefinition getGroupDefinition(String groupName) {
        return GROUP_DEFINITIONS.getOrDefault(groupName,
            GROUP_DEFINITIONS.get(PluginGroupConstants.GROUP_OTHER));
    }

    /**
     * Get representative plugin name for a group (if explicitly defined).
     * @param groupName The group name
     * @return Plugin name to use for group icon, or null for auto-discovery
     */
    public static String getRepresentativePluginName(String groupName) {
        return getGroupDefinition(groupName).getRepresentativePluginName();
    }
}
