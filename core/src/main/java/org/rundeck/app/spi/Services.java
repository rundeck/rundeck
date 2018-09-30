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

package org.rundeck.app.spi;

/**
 * Provides access to various services within the application from plugin contexts
 */
public interface Services {
    /**
     * @param type service type
     * @return true if the service type is available
     */
    boolean hasService(Class<? extends AppService> type);

    /**
     * Returns the service of the given type if available
     *
     * @param type type class
     * @param <T>  service tye
     * @return service
     * @throws IllegalStateException if the requested service type is not available
     */
    default <T extends AppService> T getService(Class<T> type) {
        throw new IllegalStateException("Required service " + type.getName() + " was not available");
    }
}
