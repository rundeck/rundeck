/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.core.projects;

/**
 * A bean interface that defines a plugin service type that can be configured as a list of instances for a project
 */
public interface ProjectPluginListConfigurable {
    /**
     * @return the plugin service name
     */
    String getServiceName();

    /**
     * @return the prefix key for saved properties
     */
    String getPropertyPrefix();
}
