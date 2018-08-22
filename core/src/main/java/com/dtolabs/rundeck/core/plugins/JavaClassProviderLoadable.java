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

import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;

/**
 * Can load a provider from a java class
 *
 * @param <T>
 */
public interface JavaClassProviderLoadable<T> {
    /**
     * @param clazz the class
     * @return true if the class is a valid provider class for the service
     */
    public boolean isValidProviderClass(Class clazz);

    /**
     * @param clazz the class
     * @param name  the provider name
     * @param <X>   subtype of T
     * @return Create provider instance from a class
     * @throws PluginException           if the plugin has an error
     * @throws ProviderCreationException if creating the instance has an error
     */
    public <X extends T> T createProviderInstance(Class<X> clazz, final String name) throws PluginException,
                                                                                            ProviderCreationException;

}
