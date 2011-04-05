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

/**
 * PluggableService is a service that supports plugin provider classes and optionally supports plugin provider scripts.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PluggableService extends FrameworkSupportService {
    /**
     * Return true if the class is a valid provider class for the service
     *
     * @param clazz the class
     */
    public boolean isValidProviderClass(Class clazz);

    /**
     * Register the class given as a provider with the given name
     *
     * @param clazz the class
     * @param name  the provider name
     */
    public void registerProviderClass(Class clazz, final String name) throws PluginException;

    /**
     * Return true if the service supports script plugins
     */
    public boolean isScriptPluggable();

    /**
     * Install the ScriptPluginProvider instances as a provider
     *
     * @param provider the script plugin provider
     */
    public void registerScriptProvider(final ScriptPluginProvider provider) throws PluginException;

}
