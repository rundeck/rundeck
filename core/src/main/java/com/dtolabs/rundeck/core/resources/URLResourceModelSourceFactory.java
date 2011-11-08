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
* URLResourceModelSourceFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 4:32 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.Properties;

/**
 * URLResourceModelSourceFactory Creates ResourceModelSources
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "url", service = "ResourceModelSource")
public class URLResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    public static final String SERVICE_PROVIDER_TYPE = "url";
    private Framework framework;

    public URLResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {
        final URLResourceModelSource urlResourceModelSource = new URLResourceModelSource(framework);
        urlResourceModelSource.configure(configuration);
        return urlResourceModelSource;
    }

    public Description getDescription() {
        return URLResourceModelSource.DESCRIPTION;
    }
}
