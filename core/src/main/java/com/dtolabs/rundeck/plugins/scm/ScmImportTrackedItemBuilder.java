package com.dtolabs.rundeck.plugins.scm;

public class ScmImportTrackedItemBuilder {
    private String id;
    private String title;
    private String iconName;
    private boolean selected;

    private ScmImportTrackedItemBuilder() {
    }

    public static ScmImportTrackedItemBuilder builder() {
        return new ScmImportTrackedItemBuilder();
    }

    public ScmImportTrackedItemBuilder id(final String id) {
        this.id = id;
        return this;
    }

    public ScmImportTrackedItemBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public ScmImportTrackedItemBuilder iconName(final String iconName) {
        this.iconName = iconName;
        return this;
    }


    public ScmImportTrackedItemBuilder selected(final boolean selected) {
        this.selected = selected;
        return this;
    }

    public ScmImportTrackedItem build() {
        return new ScmImportTrackedItemImpl(id, title, iconName, selected);
    }
}