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
* ResourceModelSourceFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/20/11 9:21 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import org.rundeck.app.spi.Services;

import java.util.Properties;

/**
 * ResourceModelSourceFactory creates ResourceModelSource instances
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ResourceModelSourceFactory {
    /**
     * @param configuration configuration data
     * @return a resource model source for the given configuration
     * @throws ConfigurationException on configuration error
     */
    public ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException;

    /**
     * Create a ResourceModelSource, the default implementation calls {@link #createResourceModelSource(Properties)}
     *
     * @param services      available services
     * @param configuration configuration
     * @return ResourceModelSource
     * @throws ConfigurationException on error
     */
    default public ResourceModelSource createResourceModelSource(Services services, Properties configuration)
            throws ConfigurationException
    {
        return createResourceModelSource(configuration);
    }

}
