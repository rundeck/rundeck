package com.dtolabs.rundeck.core.plugins.views;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;

/**
 * Input or view defined by a plugin has a title, a set of input properties and a custom action button name
 */
public interface BasicInputView {
    /**
     * @return title for the input view
     */
    public String getTitle();

    /**
     * @return Description of the action
     */
    public String getDescription();

    /**
     * @return action ID
     */
    public String getActionId();

    /**
     * @return the sections
     */
    public List<Property> getProperties();

    /**
     * @return title for the button to perform the action
     */
    public String getButtonTitle();
}
