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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;

/**
 * can register classes and instances for providers
 *
 * @param <T>
 */
public interface ProviderRegistryService<T>
        extends ProviderService<T>
{
    /**
     * Register a class for a provider
     *
     * @param name  provider name
     * @param clazz class
     */
    void registerClass(String name, Class<? extends T> clazz);

    /**
     * @param name
     * @return true if provider name is registered
     */
    boolean isRegistered(String name);

    /**
     * Register an instance for a provider
     *
     * @param name   provider
     * @param object object
     */
    void registerInstance(String name, T object);

    /**
     * @return true if instances are used
     */
    boolean isCacheInstances();

    /**
     * Set to true to enable instance registration cache
     *
     * @param cacheInstances
     */
    void setCacheInstances(boolean cacheInstances);
}
