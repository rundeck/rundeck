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
* DirectoryFileResourceModelSourceFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 11:12 AM
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
 * DirectoryFileResourceModelSourceFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "directory", service = "ResourceModelSource")
public class DirectoryResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    public static final String SERVICE_PROVIDER_TYPE = "directory";
    private Framework framework;

    public DirectoryResourceModelSourceFactory(Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {

        final DirectoryResourceModelSource fileResourceModelSource = new DirectoryResourceModelSource(framework);
        fileResourceModelSource.configure(DirectoryResourceModelSource.Configuration.fromProperties(configuration));
        return fileResourceModelSource;
    }

    public Description getDescription() {
        return DirectoryResourceModelSource.DESCRIPTION;
    }
}
