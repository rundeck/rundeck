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
* ProviderService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/16/11 12:11 PM
* 
*/
package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;

import java.util.List;


/**
 * ProviderService is a FrameworkSupportService that can return specific
 * service providers of a given name.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ProviderService<T> extends FrameworkSupportService {

    /**
     * @param providerName name of the service provider
     * @return the provider instance of the given name.
     * @throws ExecutionServiceException on error
     */
    public T providerOfType(final String providerName) throws ExecutionServiceException;
    public List<ProviderIdent> listProviders();
}
