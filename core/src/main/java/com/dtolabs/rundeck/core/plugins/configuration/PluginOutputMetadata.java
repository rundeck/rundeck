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

package com.dtolabs.rundeck.core.plugins.configuration;

/**
 * Describes a property exposed as an "output" value for conditional-logic reference, as declared via
 * {@code com.dtolabs.rundeck.plugins.descriptions.PluginOutput}.
 */
public class PluginOutputMetadata {
    private final String group;
    private final String name;
    private final String description;

    public PluginOutputMetadata(final String group, final String name, final String description) {
        this.group = group;
        this.name = name;
        this.description = description;
    }

    /**
     * @return the data group the value is exposed under
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the exposed output value name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the output value description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "PluginOutputMetadata{" +
               "group='" + group + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
