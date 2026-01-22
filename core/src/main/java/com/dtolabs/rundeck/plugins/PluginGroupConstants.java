package com.dtolabs.rundeck.plugins;

/**
 * Constants for plugin grouping metadata.
 * Use with @PluginMetadata to categorize plugins for UI grouping.
 *
 * Example:
 * <pre>
 * @Plugin(name = "my-plugin", service = ServiceNameConstants.WorkflowStep)
 * @PluginDescription(title = "My Plugin", description = "Does stuff")
 * @PluginMetadata(key = PluginGroupConstants.PLUGIN_GROUP_KEY,
 *                 value = "MyGroup")
 * public class MyPlugin implements StepPlugin { ... }
 * </pre>
 */
public class PluginGroupConstants {

    /**
     * The metadata key for plugin grouping.
     * Use this with @PluginMetadata annotation.
     */
    public static final String PLUGIN_GROUP_KEY = "groupBy";

    /** Default group for plugins without explicit groupBy metadata */
    public static final String GROUP_OTHER = "Other";
}
