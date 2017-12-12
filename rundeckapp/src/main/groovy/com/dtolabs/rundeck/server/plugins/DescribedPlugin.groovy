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

package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * DescribedPlugin holds a plugin instance, name, description and source file if available
 * @author greg
 * @since 2014-02-19
 */
class DescribedPlugin<T> {
    DescribedPlugin(T instance, Description description, String name) {
        this.instance = instance
        this.description = description
        this.name = name
    }

    DescribedPlugin(T instance, Description description, String name, File file) {
        this.name = name
        this.instance = instance
        this.description = description
        this.file = file
    }

    String name
    T instance
    Description description
    File file

    @Override
    public String toString() {
        return "DescribedPlugin{" +
                "name='" + name + '\'' +
                ", instance=" + instance +
                ", description=" + description +
                ", file=" + file +
                "} " + super.toString();
    }
}
