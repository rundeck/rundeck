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