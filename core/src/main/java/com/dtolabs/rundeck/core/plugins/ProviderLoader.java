/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* FileProviderLoader.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 5:24 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.utils.cache.FileCache;

import java.util.List;

/**
 * ProviderLoader can load a provider instance for a service given a provider name.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
interface ProviderLoader extends FileCache.Cacheable {
    /**
     * Return an provider instance for a service and provider name
     */
    public <T> T load(PluggableService<T> service, String providerName) throws ProviderLoaderException;

    /**
     * Return true if this loader can load the given ident
     */
    public boolean isLoaderFor(ProviderIdent ident);
    /**
     * List providers available
     */
    public List<ProviderIdent> listProviders();
}
