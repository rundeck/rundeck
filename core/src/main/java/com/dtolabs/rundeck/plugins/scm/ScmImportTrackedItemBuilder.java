/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public ScmImportTrackedItemBuilder deleted(final boolean deleted) {
        proto.deleted = deleted;
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
        private boolean deleted;

        public Implementation() {
        }

        public Implementation(ScmImportTrackedItem item) {
            this.id = item.getId();
            this.title = item.getTitle();
            this.iconName = item.getIconName();
            this.jobId = item.getJobId();
            this.selected = item.isSelected();
            this.deleted = item.isDeleted();
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

        @Override
        public boolean isDeleted(){
            return deleted;
        }

    }
}