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
* ScriptResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/2/11 3:57 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * ScriptResourceModelSource executes a script file to produce resource data in a certain format.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptResourceModelSource implements Configurable, ResourceModelSource {
    static Logger logger = Logger.getLogger(ScriptResourceModelSource.class.getName());

    static ArrayList<Property> properties = new ArrayList<Property>();

    public static final String CONFIG_FILE = "file";

    public static final String CONFIG_INTERPRETER = "interpreter";

    public static final String CONFIG_ARGS = "args";

    public static final String CONFIG_FORMAT = "format";

    static {
        properties.add(PropertyUtil.string(CONFIG_FILE, "Script File Path", "Path to script file to execute", true,
            null,
            new Property.Validator() {
                public boolean isValid(String value) throws ValidationException {
                    return new File(value).isFile();
                }
            }));
        properties.add(PropertyUtil.string(CONFIG_INTERPRETER, "Interpreter", "Command interpreter to use (optional)",
            false,
            null));
        properties.add(PropertyUtil.string(CONFIG_ARGS, "Arguments", "Arguments to pass to the script (optional)",
            false,
            null));
        final ArrayList<String> formats = new ArrayList<String>();
        for (final Nodes.Format format : Nodes.Format.values()) {
            formats.add(format.toString());
        }
        properties.add(PropertyUtil.select(CONFIG_FORMAT, "Resource Format",
            "Resources document format that the script will produce", true, null, formats));
    }

    static final Description DESCRIPTION = new Description() {
        public String getName() {
            return "script";
        }

        public String getTitle() {
            return "Script";
        }

        public String getDescription() {
            return "Run a script to produce resource model data";
        }

        public List<Property> getProperties() {

            return properties;
        }
    };

    private Nodes.Format format;
    private File scriptFile;
    private String interpreter;
    private String args;
    private String project;
    HashMap<String, Map<String, String>> configDataContext;

    public ScriptResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    private Framework framework;

    public void configure(final Properties configuration) throws ConfigurationException {
        if (!configuration.containsKey("project")) {
            throw new ConfigurationException("project is required");
        }
        project = configuration.getProperty("project");
        if (!configuration.containsKey(CONFIG_FILE)) {
            throw new ConfigurationException(CONFIG_FILE + " is required");
        }
        scriptFile = new File(configuration.getProperty(CONFIG_FILE));
        if (!scriptFile.isFile()) {
            throw new ConfigurationException(
                CONFIG_FILE + " does not exist or is not a file: " + scriptFile.getAbsolutePath());
        }
        interpreter = configuration.getProperty(CONFIG_INTERPRETER);
        args = configuration.getProperty(CONFIG_ARGS);
        if (!configuration.containsKey(CONFIG_FORMAT)) {
            throw new ConfigurationException(CONFIG_FORMAT + " is required");
        }
        try {
            format = Nodes.Format.valueOf(configuration.getProperty(CONFIG_FORMAT));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Unknown format: " + configuration.getProperty(CONFIG_FORMAT));
        }
        configDataContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> configdata = new HashMap<String, String>();
        configdata.put("project", project);
        configDataContext.put("context", configdata);
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        return ScriptResourceUtil.executeScript(scriptFile, args,
            interpreter,
            ScriptResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, configDataContext, format, framework, project,
            logger);
    }
}
