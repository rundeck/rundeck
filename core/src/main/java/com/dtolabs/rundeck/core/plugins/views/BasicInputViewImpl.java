package com.dtolabs.rundeck.core.plugins.views;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;

/**
 * Created by greg on 9/8/15.
 */
public class BasicInputViewImpl implements BasicInputView {
    private String title;
    private String description;
    private String actionId;
    private List<Property> properties;
    private String buttonTitle;

    public BasicInputViewImpl() {

    }

    public BasicInputViewImpl(
            final String title,
            final String actionId,
            final List<Property> properties,
            final String buttonTitle
    )
    {
        this.title = title;
        this.actionId = actionId;
        this.properties = properties;
        this.buttonTitle = buttonTitle;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    @Override
    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    @Override
    public String getButtonTitle() {
        return buttonTitle;
    }

    public void setButtonTitle(String buttonTitle) {
        this.buttonTitle = buttonTitle;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
