package com.dtolabs.rundeck.plugins.config;

/**
 * A group of common Plugins, defining a set of common configuration properties.
 */
public interface PluginGroup {

    /**
     * Get the icon URL for this plugin group.
     * @param groupName The group name
     * @return Icon URL for the group, or null to use first plugin's icon as fallback
     */
    default String getGroupIconUrl(String groupName) {
        return null;
    }
}
