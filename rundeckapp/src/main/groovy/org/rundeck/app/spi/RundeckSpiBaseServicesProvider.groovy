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

package org.rundeck.app.spi


import groovy.transform.CompileStatic

@CompileStatic
class RundeckSpiBaseServicesProvider implements ServicesProvider, Services {
    Map<Class, Object> services = [:]

    @Override
    boolean hasService(final Class<? extends AppService> type) {
        services[type] != null
    }

    @Override
    def <T extends AppService> T getService(final Class<T> type) {
        if (hasService(type)) {
            return (T) services[type]
        }
        throw new IllegalStateException("Required service " + type.getName() + " was not available");
    }

    @Override
    Services getServices() {
        this
    }
}
