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
package com.dtolabs.rundeck.server.plugins.services

import com.dtolabs.rundeck.core.plugins.*
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.audit.AuditEventListenerPlugin
import org.rundeck.core.plugins.PluginProviderServices


class AuditEventsHandlerPluginProviderService implements PluginProviderServices {

    public static final String SERVICE_NAME = ServiceNameConstants.AuditEventListener


    @Override
    <T> boolean hasServiceFor(Class<T> serviceType, String serviceName) {
        return serviceType == AuditEventListenerPlugin.class && serviceName.equals(SERVICE_NAME)
    }

    @Override
    <T> PluggableProviderService<T> getServiceProviderFor(Class<T> serviceType, String serviceName, ServiceProviderLoader loader) {
        if (serviceType == AuditEventListenerPlugin.class && SERVICE_NAME.equals(serviceName)) {
            return (PluggableProviderService<T>) new AuditEventsHandlerProviderService(loader)
        }
        return null
    }

    class AuditEventsHandlerProviderService
            extends BasePluggableProviderService<AuditEventListenerPlugin> {

        private ServiceProviderLoader pluginManager

        AuditEventsHandlerProviderService(final ServiceProviderLoader pluginManager) {
            super(SERVICE_NAME, AuditEventListenerPlugin.class)
            this.pluginManager = pluginManager
        }

        @Override
        ServiceProviderLoader getPluginManager() {
            return pluginManager
        }

    }

}
