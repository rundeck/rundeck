package com.dtolabs.rundeck.core.plugins.views;

public class ActionBuilder {
    private String id;
    private String title;
    private String description;

    public ActionBuilder id(final String id) {
        this.id = id;
        return this;
    }

    public ActionBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public ActionBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public static Action action(String id, String title, String description) {
        return new ActionImpl(id, title, description);
    }

    public Action build() {
        return new ActionImpl(id, title, description);
    }
}