package com.dtolabs.rundeck.plugins;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized definitions for plugin groups with optional explicit icon URLs.
 * If no explicit groupIconUrl is defined, the group icon will be determined
 * by the first plugin in that group with an icon (dynamically at runtime).
 */
public class PluginGroupDefinitions {

    /**
     * Definition of a plugin group with optional explicit icon URL.
     */
    @Getter
    public static class GroupDefinition {
        private final String name;
        /**
         * The icon URL for the group, or null to use first plugin's icon
         */
        private final String groupIconUrl;

        public GroupDefinition(String name, String groupIconUrl) {
            this.name = name;
            this.groupIconUrl = groupIconUrl;
        }
    }

    private static final Map<String, GroupDefinition> GROUP_DEFINITIONS;

    static {
        Map<String, GroupDefinition> definitions = new HashMap<>();

        // All groups use auto-discovery (null = use first plugin's icon in group)
        // To override with explicit icon URL: new GroupDefinition("MyGroup", "/path/to/icon.svg")

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
     * Get explicit group icon URL for a group (if defined).
     * @param groupName The group name
     * @return Icon URL for the group, or null for auto-discovery
     */
    public static String getGroupIconUrl(String groupName) {
        return getGroupDefinition(groupName).getGroupIconUrl();
    }
}
