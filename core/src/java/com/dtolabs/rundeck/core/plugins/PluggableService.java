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
* PluggableService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 2:05 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;

/**
 * PluggableService is a service that supports plugin provider classes and optionally supports plugin provider scripts.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PluggableService<T> extends FrameworkSupportService {
    /**
     * Return true if the class is a valid provider class for the service
     *
     * @param clazz the class
     */
    public boolean isValidProviderClass(Class clazz);

    /**
     * Create provider instance from a class
     *
     * @param clazz the class
     * @param name  the provider name
     */
    public T createProviderInstance(Class<T> clazz, final String name) throws PluginException,
        ProviderCreationException;

    /**
     * Return true if the service supports script plugins
     */
    public boolean isScriptPluggable();

    /**
     * Return the instance for a ScriptPluginProvider definition
     *
     * @param provider the script plugin provider
     */
    public T createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException;

}
