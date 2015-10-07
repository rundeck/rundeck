package com.dtolabs.rundeck.core.plugins.views;

/**
 * A reference to an action provided by a plugin
 */
public interface Action {
    /**
     * @return action ID, or '-' to result in a divider among a list
     */
    public String getId();

    /**
     * @return Title for the action (shown in the button/link)
     */
    public String getTitle();

    /**
     * @return description
     */
    public String getDescription();

    /**
     * @return name of an icon to use, or null
     */
    public String getIconName();
}
