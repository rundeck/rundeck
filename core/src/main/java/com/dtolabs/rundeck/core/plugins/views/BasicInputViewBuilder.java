package com.dtolabs.rundeck.core.plugins.views;

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.List;

public class BasicInputViewBuilder {
    private BasicInputViewImpl impl;

    private BasicInputViewBuilder() {
        impl = new BasicInputViewImpl();
    }

    public static BasicInputViewBuilder forActionId(String actionId) {
        return new BasicInputViewBuilder().actionId(actionId);
    }

    public BasicInputViewBuilder title(final String title) {
        impl.setTitle(title);
        return this;
    }

    public BasicInputViewBuilder actionId(final String actionId) {
        impl.setActionId(actionId);
        return this;
    }

    public BasicInputViewBuilder properties(final List<Property> properties) {
        impl.setProperties(properties);
        return this;
    }

    public BasicInputViewBuilder buttonTitle(final String buttonTitle) {
        impl.setButtonTitle(buttonTitle);
        return this;
    }

    public BasicInputViewBuilder description(final String description) {
        impl.setDescription(description);
        return this;
    }

    public BasicInputView build() {
        return impl;
    }

}