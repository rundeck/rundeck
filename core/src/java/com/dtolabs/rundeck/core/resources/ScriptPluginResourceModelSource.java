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
* ScriptPluginResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 12:01 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.Configurable;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * ScriptPluginResourceModelSource is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginResourceModelSource implements ResourceModelSource, Configurable {
    static Logger logger = Logger.getLogger(ScriptPluginResourceModelSource.class.getName());
    final ScriptPluginProvider provider;
    final private Framework framework;
    Properties configuration;
    private String format;
    private String project;
    Map<String, Map<String, String>> configDataContext;

    public ScriptPluginResourceModelSource(final ScriptPluginProvider provider, final Framework framework) {
        this.provider = provider;
        this.framework = framework;
        this.format = provider.getMetadata().get(ScriptPluginResourceModelSourceFactory.RESOURCE_FORMAT_PROP);
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        return ScriptResourceUtil.executeScript(provider.getScriptFile(), provider.getScriptArgs(),
            provider.getScriptInterpreter(),
            provider.getName(), configDataContext, format, framework, project, logger);
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = configuration;
        if (!configuration.containsKey("project")) {
            throw new ConfigurationException("project is required");
        }
        this.project = configuration.getProperty("project");
        final Map<String, String> configData = new HashMap<String, String>();
        for (final Object o : configuration.keySet()) {
            String k = (String) o;
            configData.put(k, configuration.getProperty(k));
        }
        final Map<String, Map<String, String>> stringMapMap = new HashMap<String, Map<String, String>>();
        configDataContext = DataContextUtils.addContext("config", configData, stringMapMap);
    }

}
