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

/**
 * Input or view defined by a plugin has a title, a set of input properties and a custom action button name
 */
public interface BasicInputView {
    /**
     * @return title for the input view
     */
    public String getTitle();

    /**
     * @return Description of the action
     */
    public String getDescription();

    /**
     * @return action ID
     */
    public String getActionId();

    /**
     * @return the sections
     */
    public List<Property> getProperties();

    /**
     * @return title for the button to perform the action
     */
    public String getButtonTitle();
}
