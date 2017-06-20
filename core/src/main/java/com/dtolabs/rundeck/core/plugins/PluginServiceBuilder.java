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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.List;

/**
 * Created by greg on 10/24/16.
 */
public class PluginServiceBuilder {

    <T> PluggableProviderService<T> buildPlugin(Class<T> type,final ServiceProviderLoader loader) {
        return new BasePluggableProviderService<T>(type.getName(), type) {
            @Override
            public ServiceProviderLoader getPluginManager() {
                return loader;
            }
        };
    }
}
