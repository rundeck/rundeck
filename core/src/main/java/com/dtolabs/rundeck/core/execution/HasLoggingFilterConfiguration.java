/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.plugins.PluginConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for providing logging plugin configurations
 *
 * @author greg
 * @since 5/16/17
 */
public interface HasLoggingFilterConfiguration {
    /**
     * @return the list of filter configurations, each configuration
     */
    public List<PluginConfiguration> getFilterConfigurations();

    /**
     * @param o object
     *
     * @return an optional which contains a {@link HasLoggingFilterConfiguration} if the object implements it, or is
     * empty otherwise
     */
    static Optional<HasLoggingFilterConfiguration> of(Object o) {
        return o instanceof HasLoggingFilterConfiguration
               ? Optional.of((HasLoggingFilterConfiguration) o)
               : Optional.empty();
    }
}
