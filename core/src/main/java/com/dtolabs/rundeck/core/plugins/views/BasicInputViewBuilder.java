package com.dtolabs.rundeck.core.plugins.views;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;

public class BasicInputViewBuilder {
    private String title;
    private String actionId;
    private List<Property> properties;
    private String buttonTitle;

    private BasicInputViewBuilder() {
    }

    public static BasicInputViewBuilder forActionId(String actionId) {
        return new BasicInputViewBuilder().actionId(actionId);
    }

    public BasicInputViewBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public BasicInputViewBuilder actionId(final String actionId) {
        this.actionId = actionId;
        return this;
    }

    public BasicInputViewBuilder properties(final List<Property> properties) {
        this.properties = properties;
        return this;
    }

    public BasicInputViewBuilder buttonTitle(final String buttonTitle) {
        this.buttonTitle = buttonTitle;
        return this;
    }

    public BasicInputView build() {
        return new BasicInputViewImpl(title, actionId, properties, buttonTitle);
    }
}