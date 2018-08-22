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

/*
* PluggableService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 2:05 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;

/**
 * PluggableService is a service that supports loading plugins via provider loaders.
 *
 * @author Greg Schueler <a href="mailto:greg@rundeck.com">greg@rundeck.com</a>
 */
public interface PluggableService<T>
    extends FrameworkSupportService
{
    /**
     * @param loader loader
     * @return true if the loader can be used for this service, by default delegates to the loader's
     * {@link ProviderLoader#canLoadForService(FrameworkSupportService)}
     */
    default boolean canLoadWithLoader(ProviderLoader loader) {
        return loader.canLoadForService(this);
    }

    /**
     * Load provider with the given loader
     *
     * @param providerName provider name
     * @param loader       loader
     * @return loaded provider instance, by default delegates to the loader's
     * {@link ProviderLoader#load(PluggableService,
     *         String)}
     * @throws ProviderLoaderException if an error occurs
     */
    default T loadWithLoader(String providerName, ProviderLoader loader) throws ProviderLoaderException {
        return loader.load(this, providerName);
    }

    /**
     * Load a closeable provider with the given loader
     *
     * @param providerName provider name
     * @param loader       loader
     * @return closeable provider for instance, by default delegates to the loader's {@link
     *         ProviderLoader#loadCloseable(PluggableService, String)}
     * @throws ProviderLoaderException if an error occurs
     */
    default CloseableProvider<T> loadCloseableWithLoader(String providerName, ProviderLoader loader)
        throws ProviderLoaderException
    {
        return loader.loadCloseable(this, providerName);
    }
}
