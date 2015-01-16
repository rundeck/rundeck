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
* PluginCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/14/11 8:49 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;

import java.util.List;

/**
 * PluginCache can use PluginScanners and find ProviderLoaders for ProviderIdents.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PluginCache {
    /**
     * Add a new scanner
     * @param  scanner scanner
     */
    void addScanner(PluginScanner scanner);

    /**
     * Get the loader for the provider
     *
     * @param ident provider ident
     *
     * @return loader for the provider
     * @throws ProviderLoaderException on loading error
     */
    ProviderLoader getLoaderForIdent(ProviderIdent ident) throws ProviderLoaderException;
    List<ProviderIdent> listProviders() ;
}
