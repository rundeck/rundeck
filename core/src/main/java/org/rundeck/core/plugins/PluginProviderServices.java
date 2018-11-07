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

package org.rundeck.core.plugins;

import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;

import java.util.ServiceLoader;

/**
 * A java {@link ServiceLoader} service type for Rundeck "provider service" implementations sorry for the confusion.
 */
public interface PluginProviderServices {

    /**
     * @param <T>
     * @param serviceType
     * @param serviceName
     * @return true if a service for the provider type is available
     */
    <T> boolean hasServiceFor(Class<T> serviceType, final String serviceName);

    /**
     * @param serviceType
     * @param loader
     * @param <T>
     * @return service for the type
     */
    <T> PluggableProviderService<T> getServiceProviderFor(
        Class<T> serviceType,
        String serviceName,
        final ServiceProviderLoader loader
    );
}
