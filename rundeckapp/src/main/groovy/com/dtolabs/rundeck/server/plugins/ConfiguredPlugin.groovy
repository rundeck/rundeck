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

/**
 * ConfiguredPlugin holds a plugin instance and configuration map
 * @author greg
 * @since 2014-02-19
 */
class ConfiguredPlugin<T> {
    ConfiguredPlugin(T instance, Map<String, Object> configuration) {
        this.instance = instance
        this.configuration = configuration
    }

    ConfiguredPlugin(final T instance, final Map<String, Object> configuration, final Closeable closeable) {
        this.instance = instance
        this.configuration = configuration
        this.closeable = closeable
    }
    T instance
    Map<String,Object> configuration
    Closeable closeable
}
