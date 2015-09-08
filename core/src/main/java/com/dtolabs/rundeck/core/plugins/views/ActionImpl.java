package com.dtolabs.rundeck.core.plugins.views;

/**
 * Created by greg on 9/8/15.
 */
public class ActionImpl implements Action {
    private String id;
    private String title;
    private String description;

    public ActionImpl(final String id, final String title, final String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
