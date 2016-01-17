package com.dtolabs.rundeck.plugins.scm;

/**
 * Builder for {@link ScmImportTrackedItem}
 */
public class ScmImportTrackedItemBuilder {
    private Implementation proto;

    private ScmImportTrackedItemBuilder() {
        proto = new Implementation();
    }

    public static ScmImportTrackedItemBuilder builder() {
        return new ScmImportTrackedItemBuilder();
    }

    public ScmImportTrackedItemBuilder id(final String id) {
        proto.id = id;
        return this;
    }

    public ScmImportTrackedItemBuilder title(final String title) {
        proto.title = title;
        return this;
    }

    public ScmImportTrackedItemBuilder iconName(final String iconName) {
        proto.iconName = iconName;
        return this;
    }

    public ScmImportTrackedItemBuilder jobId(final String jobId) {
        proto.jobId = jobId;
        return this;
    }


    public ScmImportTrackedItemBuilder selected(final boolean selected) {
        proto.selected = selected;
        return this;
    }

    public ScmImportTrackedItem build() {
        return new Implementation(proto);
    }

    /**
     *
     */
    public static class Implementation implements ScmImportTrackedItem {
        private String id;
        private String title;
        private String iconName;
        private String jobId;
        private boolean selected;

        public Implementation() {
        }

        public Implementation(ScmImportTrackedItem item) {
            this.id = item.getId();
            this.title = item.getTitle();
            this.iconName = item.getIconName();
            this.jobId = item.getJobId();
            this.selected = item.isSelected();
        }


        @Override
        public String getId() {
            return id;
        }


        @Override
        public String getTitle() {
            return title;
        }


        @Override
        public String getIconName() {
            return iconName;
        }


        @Override
        public boolean isSelected() {
            return selected;
        }


        @Override
        public String getJobId() {
            return jobId;
        }

    }
}