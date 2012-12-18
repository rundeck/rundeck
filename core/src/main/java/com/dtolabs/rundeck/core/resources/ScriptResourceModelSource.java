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
import com.dtolabs.rundeck.core.plugins.ScriptDataContextUtil;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
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

    static ArrayList<Property> scriptResourceProperties = new ArrayList<Property>();

    public static final String CONFIG_FILE = "file";

    public static final String CONFIG_INTERPRETER = "interpreter";

    public static final String CONFIG_ARGS = "args";
    public static final String CONFIG_INTERPRETER_ARGS_QUOTED = "argsQuoted";

    public static final String CONFIG_FORMAT = "format";
    public static final PropertyValidator FILE_VALIDATOR = new PropertyValidator() {
        public boolean isValid(String value) throws ValidationException {
            return new File(value).isFile();
        }
    };

    static Description createDescription(final List<String> formats) {
        return DescriptionBuilder.builder()
            .name("script")
            .title("Script")
            .description("Run a script to produce resource model data")
            .property(PropertyBuilder.builder()
                          .freeSelect(CONFIG_FORMAT)
                          .title("Resource Format")
                          .description("Resources document format that the script will produce")
                          .required(true)
                          .values(formats)
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .string(CONFIG_FILE)
                          .title("Script File Path")
                          .description("Path to script file to execute")
                          .required(true)
                          .validator(FILE_VALIDATOR)
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .string(CONFIG_INTERPRETER)
                          .title("Interpreter")
                          .description("Command interpreter to use (optional)")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .string(CONFIG_ARGS)
                          .title("Arguments")
                          .description("Arguments to pass to the script (optional)")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .booleanType(CONFIG_INTERPRETER_ARGS_QUOTED)
                          .title("Quote Interpreter Args")
                          .description(
                              "If true, pass script file and args as a single argument to "
                              + "interpreter, otherwise, pass as multiple arguments")
                          .defaultValue("false")
                          .build()
            )
            .build();
    }


    private String format;
    private File scriptFile;
    private String interpreter;
    private String args;
    private boolean interpreterArgsQuoted;
    private String project;
    HashMap<String, Map<String, String>> configDataContext;
    Map<String, Map<String, String>> executionDataContext;

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
        format = configuration.getProperty(CONFIG_FORMAT);

        interpreterArgsQuoted = Boolean.parseBoolean(configuration.getProperty(CONFIG_INTERPRETER_ARGS_QUOTED));

        configDataContext = new HashMap<String, Map<String, String>>();
        final HashMap<String, String> configdata = new HashMap<String, String>();
        configdata.put("project", project);
        configDataContext.put("context", configdata);

        executionDataContext = ScriptDataContextUtil.createScriptDataContextForProject(framework, project);

        executionDataContext.putAll(configDataContext);
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        try {
            return ScriptResourceUtil.executeScript(scriptFile, args,
                interpreter,
                ScriptResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, executionDataContext, format, framework, project,
                logger, interpreterArgsQuoted);
        } catch (ResourceModelSourceException e) {
            throw new ResourceModelSourceException(
                "failed to execute: "+scriptFile+": " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "ScriptResourceModelSource{" +
               "scriptFile=" + scriptFile +
               ", format='" + format + '\'' +
               '}';
    }
}
