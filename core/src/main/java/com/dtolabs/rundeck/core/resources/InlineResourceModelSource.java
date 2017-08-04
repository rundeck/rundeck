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
* URLResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 4:33 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * InlineResourceModelSource produces nodes from a editor window in either JSON, YAML, or XML Formats.
 *
 * @author Derek Brown <derekbrown@salesforce.com>
 */
public class InlineResourceModelSource implements ResourceModelSource, Configurable {
    static final Logger logger = Logger.getLogger(InlineResourceModelSource.class.getName());
    final private Framework framework;
    Configuration configuration;

    public InlineResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    public static final Description DESCRIPTION = DescriptionBuilder.builder()
        .name("inline")
        .title("Inline Source")
        .description("Allows for insertion of nodes using an Inline Editor")

        .property(PropertyBuilder.builder()
		        		.string(Configuration.SOURCE)
		        		.required(true)
		        		.title("Nodes")
		            .description("Inline Node Definitions")
		            .renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.CODE)
		            .renderingOption(StringRenderingConstants.CODE_SYNTAX_MODE, "xml")
		            .renderingOption(StringRenderingConstants.CODE_SYNTAX_SELECTABLE, true)
		            .build()
        )
        .build();



    public static class Configuration {
        public static final String SOURCE = "source";
        public static final String PROJECT = "project";
        String project;
        String source;

        private final Properties properties;

        Configuration() {
            properties = new Properties();
        }

        Configuration(final Properties configuration) {
            if (null == configuration) {
                throw new NullPointerException("configuration");
            }
            this.properties = configuration;
            configure();
        }

        private void configure() {
            if (properties.containsKey(SOURCE)) {
               source = properties.getProperty(SOURCE);
            }
            if (properties.containsKey(PROJECT)) {
                project = properties.getProperty(PROJECT);
            }
        }

        void validate() throws ConfigurationException {
            if (null == project) {
                throw new ConfigurationException("project is required");
            }
            if (null == source) {
            		throw new ConfigurationException("source is required");
            }
        }

        Configuration(final Configuration configuration) {
            this(configuration.getProperties());
        }

        public Configuration source(final String source) {
        		this.source = source;
        		properties.setProperty(SOURCE, source);
            return this;
        }

        public Configuration project(final String project) {
            this.project = project;
            properties.setProperty(PROJECT, project);
            return this;
        }

        public static Configuration fromProperties(final Properties configuration) {
            return new Configuration(configuration);
        }

        public static Configuration clone(final Configuration configuration) {
            return fromProperties(configuration.getProperties());
        }

        public static Configuration build() {
            return new Configuration();
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
    	
		INodeSet nodes = null;
        for(ResourceFormatParser parser : framework.getResourceFormatParserService().listParsers()) {
        		try {
        			nodes = parser.parseDocument(new ByteArrayInputStream(this.configuration.source.getBytes(StandardCharsets.UTF_8)));
        		} catch (ResourceFormatParserException e) {
        			continue;
        		}
        }
        
        if (null == nodes) {
        		throw new ResourceModelSourceException("Error parsing source input.");
        } else {
        		return nodes;
        }
        
    }

    @Override
    public String toString() {
        return "InlineResourceModelSource{" +
               "Source='" + configuration.source + '\'' +
               '}';
    }
}
