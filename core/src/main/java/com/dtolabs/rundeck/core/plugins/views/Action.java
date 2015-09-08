package com.dtolabs.rundeck.core.plugins.views;

/**
 * A reference to an action
 */
public interface Action {
    /**
     * @return action ID
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
}
