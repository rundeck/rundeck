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
* ConfigurableService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/28/11 10:52 AM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;

import java.util.Properties;

/**
 * ConfigurableService can produce a provider instance of a certain type using
 * configuration input.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ConfigurableService<T> extends FrameworkSupportService {

    /**
     * @param type provider name
     * @param configuration configuration properties
     * @return a ResourceModelSource of a give type with a given configuration
     * @throws ExecutionServiceException on error
     */
    public T getProviderForConfiguration(final String type, final Properties configuration) throws
        ExecutionServiceException;

}
