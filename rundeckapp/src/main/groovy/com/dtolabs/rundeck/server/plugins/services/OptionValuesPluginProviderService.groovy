/*
 * Copyright 2018 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin;
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin;
import org.rundeck.core.plugins.PluginProviderServices;

/**
 * Provider service for OptionSourcePlugins
 * Created by stephen
 * Date: 5/29/18
 * Time: 6:05 AM
 */
public class OptionValuesPluginProviderService implements PluginProviderServices {
    @Override
    def <T> boolean hasServiceFor(final Class<T> serviceType, final String serviceName) {
        return serviceType == OptionValuesPlugin.class && serviceName.equals(ServiceNameConstants.OptionValues)
    }

    @Override
    def <T> PluggableProviderService<T> getServiceProviderFor(
            final Class<T> serviceType,
            final String serviceName,
            final ServiceProviderLoader loader
    ) {
        if(serviceType == OptionValuesPlugin.class && ServiceNameConstants.OptionValues.equals(serviceName))
            return (PluggableProviderService<T>)new OptionValueProviderService(loader)
        return null;
    }
}
