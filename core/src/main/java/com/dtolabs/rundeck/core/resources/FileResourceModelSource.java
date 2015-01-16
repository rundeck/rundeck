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
* FileResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 11:28 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.format.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import com.dtolabs.shared.resources.ResourceXMLGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * FileResourceModelSource can parse files to provide node results
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileResourceModelSource implements ResourceModelSource, Configurable {
    private Framework framework;
    private NodeSetImpl nodeSet;
    private Configuration configuration;
    long lastModTime = 0;

    FileResourceModelSource(final Framework framework) {
        this.framework = framework;
        nodeSet = new NodeSetImpl();
    }

    static Description createDescription(final List<String> formats) {
        return DescriptionBuilder.builder()
            .name("file")
            .title("File")
            .description("Reads a file containing node definitions in a supported format")
            .property(PropertyBuilder.builder()
                          .freeSelect(Configuration.FORMAT)
                          .title("Format")
                          .description("Format of the file")
                          .values(formats)
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .string(Configuration.FILE)
                          .title("File Path")
                          .description("Path of the file")
                          .required(true)
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .booleanType(Configuration.GENERATE_FILE_AUTOMATICALLY)
                          .title("Generate")
                          .description("Automatically generate the file if it is missing?")
                          .required(true)
                          .defaultValue("false")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .booleanType(Configuration.INCLUDE_SERVER_NODE)
                          .title("Include Server Node")
                          .description("Automatically include the server node in the generated file?")
                          .required(true)
                          .defaultValue("false")
                          .build()
            )
            .property(PropertyBuilder.builder()
                          .booleanType(Configuration.REQUIRE_FILE_EXISTS)
                          .title("Require File Exists")
                          .description("Require that the file exists")
                          .required(true)
                          .defaultValue("false")
                          .build()
            )

            .build();
    }


    public static class Configuration {
        public static final String GENERATE_FILE_AUTOMATICALLY = "generateFileAutomatically";
        public static final String INCLUDE_SERVER_NODE = "includeServerNode";
        public static final String FILE = "file";
        public static final String PROJECT = "project";
        public static final String FORMAT = "format";
        public static final String REQUIRE_FILE_EXISTS = "requireFileExists";
        String format;
        File nodesFile;
        String project;
        boolean generateFileAutomatically;
        boolean includeServerNode;
        boolean requireFileExists;
        final Properties configuration;

        Configuration() {
            configuration = new Properties();
        }

        Configuration(final Properties configuration) {
            if (null == configuration) {
                throw new NullPointerException("configuration");
            }
            this.configuration = configuration;
            configure();
        }

        Configuration(final Configuration configuration) {
            this(configuration.getProperties());
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

        public Configuration format(final String format) {
            this.format = format;
            configuration.put(FORMAT, format);
            return this;
        }

        public Configuration file(final File file) {
            this.nodesFile = file;
            configuration.put(FILE, file.getAbsolutePath());
            return this;
        }

        public Configuration file(final String file) {
            this.nodesFile = new File(file);
            configuration.put(FILE, file);
            return this;
        }

        public Configuration project(final String project) {
            this.project = project;
            configuration.put(PROJECT, project);
            return this;
        }

        public Configuration includeServerNode(boolean include) {
            this.includeServerNode = include;
            configuration.put(INCLUDE_SERVER_NODE, Boolean.toString(include));
            return this;
        }

        public Configuration generateFileAutomatically(boolean generate) {
            this.generateFileAutomatically = generate;
            configuration.put(GENERATE_FILE_AUTOMATICALLY, Boolean.toString(generate));
            return this;
        }

        public Configuration requireFileExists(boolean require) {
            this.requireFileExists = require;
            configuration.put(REQUIRE_FILE_EXISTS, Boolean.toString(requireFileExists));
            return this;
        }

        public Properties getProperties() {
            return configuration;
        }


        void configure() {
            if (configuration.containsKey(PROJECT)) {
                project = configuration.getProperty(PROJECT);
            }
            if (configuration.containsKey(FILE)) {
                nodesFile = new File(configuration.getProperty(FILE));
            }
            if (configuration.containsKey(FORMAT)) {
                format = configuration.getProperty(FORMAT);
            }
            if (configuration.containsKey(GENERATE_FILE_AUTOMATICALLY)) {
                generateFileAutomatically = Boolean.parseBoolean(configuration.getProperty(
                    GENERATE_FILE_AUTOMATICALLY));
            }
            if (configuration.containsKey(INCLUDE_SERVER_NODE)) {
                includeServerNode = Boolean.parseBoolean(configuration.getProperty(
                    INCLUDE_SERVER_NODE));
            }
            if (configuration.containsKey(REQUIRE_FILE_EXISTS)) {
                requireFileExists = Boolean.parseBoolean(configuration.getProperty(REQUIRE_FILE_EXISTS));
            }
        }

        void validate() throws ConfigurationException {
            if (null == project) {
                throw new ConfigurationException("project is required");
            }
            if (null == nodesFile) {
                throw new ConfigurationException("file is required");
            }
        }

        @Override
        public String toString() {
            return "Configuration{" +
                   "format=" + format +
                   ", nodesFile=" + nodesFile +
                   ", project='" + project + '\'' +
                   ", generateFileAutomatically=" + generateFileAutomatically +
                   ", includeServerNode=" + includeServerNode +
                   ", requireFileExists=" + requireFileExists +
                   ", configuration=" + configuration +
                   '}';
        }
    }

    public void configure(final Properties configs) throws ConfigurationException {
        final Configuration configuration1 = Configuration.fromProperties(configs);
        configure(configuration1);
    }

    /**
     * Configure the Source
     * @param configuration configuration
     * @throws com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException on config error
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        return getNodes(configuration.nodesFile, configuration.format);
    }

    /**
     * Returns a {@link INodeSet} object conatining the nodes config data.
     *
     * @param nodesFile the source file
     * @param format nodes format
     *
     * @return an instance of {@link INodeSet}
     * @throws ResourceModelSourceException on error
     */
    public synchronized INodeSet getNodes(final File nodesFile, final String format) throws
        ResourceModelSourceException {
        final Long modtime = nodesFile.lastModified();
        if (0 == nodeSet.getNodes().size() || (modtime > lastModTime)) {
            nodeSet = new NodeSetImpl();
            loadNodes(nodesFile, format);
            lastModTime = modtime;
        }
        return nodeSet;
    }

    private void generateResourcesFile(final File resfile, final String format) throws ResourceModelSourceException {
        final NodeEntryImpl node = framework.createFrameworkNode();
        node.setFrameworkProject(configuration.project);
        final ResourceFormatGenerator generator;
        if (null!=format) {
            try {
                generator = framework.getResourceFormatGeneratorService().getGeneratorForFormat(format);
            } catch (UnsupportedFormatException e) {
                throw new ResourceModelSourceException(e);
            }
        } else {
            try {
                generator = framework.getResourceFormatGeneratorService().getGeneratorForFileExtension(resfile);
            } catch (UnsupportedFormatException e) {
                throw new ResourceModelSourceException(e);
            }
        }
        if (configuration.includeServerNode) {
            NodeSetImpl nodes = new NodeSetImpl();
            nodes.putNode(node);

            try {
                final FileOutputStream stream = new FileOutputStream(resfile);
                try {
                    generator.generateDocument(nodes, stream);
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                throw new ResourceModelSourceException(e);
            } catch (ResourceFormatGeneratorException e) {
                throw new ResourceModelSourceException(e);
            }
        }
    }

    private void loadNodes(final File nodesFile, final String format) throws ResourceModelSourceException {
        if (!nodesFile.isFile() && configuration.generateFileAutomatically) {
            generateResourcesFile(nodesFile, format);
        } else if (configuration.includeServerNode) {
            final NodeEntryImpl node = framework.createFrameworkNode();
            nodeSet.putNode(node);
        }
        if (nodesFile.isFile()) {
            final ResourceFormatParser parser = createParser(nodesFile, format);
            try {
                final INodeSet set = parser.parseDocument(nodesFile);
                if(null!=set){
                    nodeSet.putNodes(set);
                }
            } catch (ResourceFormatParserException e) {
                throw new ResourceModelSourceException(e);
            }
        } else if (configuration.requireFileExists) {
            throw new ResourceModelSourceException("File does not exist: " + nodesFile);
        }
    }

    /**
     * Create a NodeFileParser given the project and the source file, using the predetermined format
     *
     * @param file the nodes resource file
     * @param format the file format
     *
     * @return a new parser based on the determined format
     * @throws ResourceModelSourceException if the format is not supported
     */
    protected ResourceFormatParser createParser(final File file, final String format) throws
        ResourceModelSourceException {
        try {
            if (null != format) {
                return framework.getResourceFormatParserService().getParserForFormat(format);
            } else {
                return framework.getResourceFormatParserService().getParserForFileExtension(file);
            }
        } catch (UnsupportedFormatException e) {
            throw new ResourceModelSourceException(e);
        }
    }

    /**
     *
     * Utility method to directly parse the nodes from a file
     * @param file file
     * @param framework fwk
     * @param project project name
     * @return nodes
     * @throws ResourceModelSourceException if an error occurs
     * @throws ConfigurationException if a configuration error occurs
     */
    public static INodeSet parseFile(final String file, final Framework framework, final String project) throws
        ResourceModelSourceException, ConfigurationException {
        return parseFile(new File(file), framework, project);
    }

    /**
     * Utility method to directly parse the nodes from a file
     * @param file file
     * @param framework fwk
     * @param project project name
     * @return nodes
     * @throws ResourceModelSourceException if an error occurs
     * @throws ConfigurationException if a configuration error occurs
     */
    public static INodeSet parseFile(final File file, final Framework framework, final String project) throws
        ResourceModelSourceException,
        ConfigurationException {
        final FileResourceModelSource prov = new FileResourceModelSource(framework);
        prov.configure(
            FileResourceModelSource.Configuration.build()
                .file(file)
                .includeServerNode(false)
                .generateFileAutomatically(false)
                .project(project)
                .requireFileExists(true)
        );
        return prov.getNodes();
    }

    /**
     * Utility method to directly parse the nodes from a file
     * @param file file
     * @param format specified format
     * @param framework fwk
     * @param project project name
     * @return nodes
     * @throws ResourceModelSourceException if an error occurs
     * @throws ConfigurationException if a configuration error occurs
     */
    public static INodeSet parseFile(final File file, final String format, final Framework framework,
                                     final String project) throws
        ResourceModelSourceException,
        ConfigurationException {
        final FileResourceModelSource prov = new FileResourceModelSource(framework);
        prov.configure(
            FileResourceModelSource.Configuration.build()
                .file(file)
                .includeServerNode(false)
                .generateFileAutomatically(false)
                .project(project)
                .format(format)
                .requireFileExists(true)
        );
        return prov.getNodes();
    }

    @Override
    public String toString() {
        return "FileResourceModelSource{" +
               "file=" + configuration.nodesFile+
               ", format=" + configuration.format +
               '}';
    }
}
