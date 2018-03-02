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

import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.List;
import java.util.function.Predicate;

/**
 * Manage enabled/disabled plugins for projects
 */
public interface PluginControlService {
    /**
     * @return list of disabled plugins for the project, in Service:provider format
     */
    List<String> listDisabledPlugins();

    /**
     * @param projectName project
     * @param plugins     descriptions list
     * @param serviceName service name
     * @return list of enabled plugin descriptions
     */
    List<Description> filterEnabledPlugins(
        List<Description> plugins,
        String serviceName
    );

    /**
     * @param serviceName service name
     * @return predicate for testing enabled providers for a service
     */
    Predicate<String> enabledPredicateForService(String serviceName);

    /**
     * @param serviceName service name
     * @return predicate for testing disabled providers for a service
     */
    Predicate<String> disabledPredicateForService(String serviceName);

    /**
     * @param pluginName  provider name
     * @param serviceName service name
     * @return true if given plugin is disabled
     */
    boolean isDisabledPlugin(String pluginName, String serviceName);

    /**
     * @param projectName project
     * @param pluginName  provider name
     * @param serviceName service name
     * @throws PluginDisabledException if the given plugin is disabled
     */
    void checkDisabledPlugin(String pluginName, String serviceName)
        throws PluginDisabledException;
}
