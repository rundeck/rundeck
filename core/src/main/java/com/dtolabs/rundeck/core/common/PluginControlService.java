/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.common;

import java.util.function.Predicate;

/**
 * Manage enabled/disabled plugins for projects
 */
public interface PluginControlService {
    /**
     * @param serviceName service name
     * @return predicate for testing enabled providers for a service
     */
    Predicate<String> enabledPredicateForService(String serviceName);

    /**
     * @param pluginName  provider name
     * @param serviceName service name
     * @return true if given plugin is disabled
     */
    boolean isDisabledPlugin(String pluginName, String serviceName);

    /**
     * @param pluginName  provider name
     * @param serviceName service name
     * @throws PluginDisabledException if the given plugin is disabled
     */
    void checkDisabledPlugin(String pluginName, String serviceName)
        throws PluginDisabledException;
}
