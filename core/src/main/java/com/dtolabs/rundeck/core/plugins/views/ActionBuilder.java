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

package com.dtolabs.rundeck.core.plugins.views;

public class ActionBuilder {
    private String id;
    private String title;
    private String description;
    private String iconName;

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

    public ActionBuilder iconName(final String iconName) {
        this.iconName = iconName;
        return this;
    }

    public static Action from(Action action) {
        return new ActionImpl(action.getId(), action.getTitle(), action.getDescription(), action.getIconName());
    }

    public static Action action(String id, String title, String description) {
        return new ActionImpl(id, title, description, null);
    }

    public static Action action(String id, String title, String description, String iconName) {
        return new ActionImpl(id, title, description, iconName);
    }

    public Action build() {
        return new ActionImpl(id, title, description, iconName);
    }
}