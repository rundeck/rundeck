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
* ScriptResourceModelSourceFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 3:57 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;

import java.io.File;
import java.util.*;

/**
 * ScriptResourceModelSourceFactory implements the 'script' plugin for ResourceModelSource
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "script", service = "ResourceModelSource")
public class ScriptResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    static final String SERVICE_PROVIDER_TYPE = "script";

    final Framework framework;

    public ScriptResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }


    public Description getDescription() {
        return ScriptResourceModelSource.createDescription(framework.getResourceFormatParserService().listFormats());
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {

        final ScriptResourceModelSource urlResourceModelSource = new ScriptResourceModelSource(framework);
        urlResourceModelSource.configure(configuration);
        return urlResourceModelSource;
    }
}
