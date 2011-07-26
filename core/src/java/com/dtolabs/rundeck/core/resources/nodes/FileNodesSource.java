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
* FileNodesSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 11:28 AM
* 
*/
package com.dtolabs.rundeck.core.resources.nodes;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.shared.resources.ResourceXMLGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * FileNodesSource can parse files to provide node results
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileNodesSource implements NodesSource, Configurable {
    private Framework framework;
    private NodeSetImpl nodeSet;
    private Configuration configuration;
    long lastModTime = 0;

    FileNodesSource(final Framework framework) {
        this.framework = framework;
        nodeSet = new NodeSetImpl();
    }

    public static class Configuration {
        public static final String GENERATE_FILE_AUTOMATICALLY = "generateFileAutomatically";
        public static final String INCLUDE_SERVER_NODE = "includeServerNode";
        public static final String FILE = "file";
        public static final String PROJECT = "project";
        public static final String FORMAT = "format";
        public static final String REQUIRE_FILE_EXISTS = "requireFileExists";
        Nodes.Format format;
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

        public Configuration format(final Nodes.Format format) {
            this.format = format;
            configuration.put(FORMAT, format.toString());
            return this;
        }

        public Configuration format(final String format) {
            try {
                this.format = Nodes.Format.valueOf(format);
                configuration.put(FORMAT, format);
            } catch (IllegalArgumentException e) {

            }
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
                try {
                    format = Nodes.Format.valueOf(configuration.getProperty(FORMAT));
                } catch (IllegalArgumentException e) {
                }
            } else if (nodesFile != null) {
                if (nodesFile.getName().endsWith(".xml")) {
                    format = Nodes.Format.resourcexml;
                } else if (nodesFile.getName().endsWith(".yaml")) {
                    format = Nodes.Format.resourceyaml;
                }
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
            if (configuration.containsKey(FORMAT)) {
                try {
                    Nodes.Format.valueOf(configuration.getProperty(FORMAT));
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException("format is not recognized: " + configuration.getProperty(FORMAT));
                }
            } else if (null == format) {
                throw new ConfigurationException(
                    "Unable to determine file format for file: " + nodesFile.getAbsolutePath());
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
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
    }

    public INodeSet getNodes() throws NodesSourceException {
        return getNodes(configuration.nodesFile, configuration.format);
    }

    /**
     * Returns a {@link Nodes} object conatining the nodes config data.
     *
     * @param nodesFile the source file
     * @param format
     *
     * @return an instance of {@link Nodes}
     */
    public synchronized INodeSet getNodes(final File nodesFile, final Nodes.Format format) throws
        NodesSourceException {
        final Long modtime = nodesFile.lastModified();
        if (0 == nodeSet.getNodes().size() || !modtime.equals(lastModTime)) {
            nodeSet = new NodeSetImpl();
            loadNodes(nodesFile, format);
            lastModTime = modtime;
        }
        return nodeSet;
    }

    private void generateResourcesFile(final File resfile, final Nodes.Format format) {
        final NodeEntryImpl node = framework.createFrameworkNode();
        node.setFrameworkProject(configuration.project);
        final NodesFileGenerator generator;
        if (format == Nodes.Format.resourcexml) {
            generator = new ResourceXMLGenerator(resfile);
        } else if (format == Nodes.Format.resourceyaml) {
            generator = new NodesYamlGenerator(resfile);
        } else {
//            getLogger().error("Unable to generate resources file. Unrecognized extension for dest file: " + resfile
//                .getAbsolutePath());
            return;
        }
        if (configuration.includeServerNode) {
            generator.addNode(node);
            try {
                generator.generate();
            } catch (IOException e) {
                //   getLogger().error("Unable to generate resources file: " + e.getMessage(), e);
            } catch (NodesGeneratorException e) {
                // getLogger().error("Unable to generate resources file: " + e.getMessage(), e);
            }
        }

        //getLogger().debug("generated resources file: " + resfile.getAbsolutePath());
    }

    private void loadNodes(final File nodesFile, final Nodes.Format format) throws NodesSourceException {
        if (!nodesFile.isFile() && configuration.generateFileAutomatically) {
            generateResourcesFile(nodesFile, format);
        } else if (configuration.includeServerNode) {
            final NodeEntryImpl node = framework.createFrameworkNode();
            nodeSet.putNode(node);
        }
        if (nodesFile.isFile()) {
            final NodeFileParser parser = createParser(nodesFile, format);
            try {
                parser.parse();
            } catch (NodeFileParserException e) {
                throw new NodesSourceException(e);
            }
        } else if (configuration.requireFileExists) {
            throw new NodesSourceException("File does not exist: " + nodesFile);
        }
    }

    /**
     * Create a NodeFileParser given the project and the source file, using the predetermined format
     *
     * @param propfile the nodes resource file
     *
     * @return a new parser based on the determined format
     */
    protected NodeFileParser createParser(final File propfile, final Nodes.Format format) {
        switch (format) {
            case resourcexml:
                return new NodesXMLParser(propfile, nodeSet);
            case resourceyaml:
                return new NodesYamlParser(propfile, nodeSet);
            default:
                throw new IllegalArgumentException("Nodes resource file format not valid: " + format);
        }
    }

    /**
     * Utility method to directly parse the nodes from a file
     */
    public static INodeSet parseFile(final String file, final Framework framework, final String project) throws
        NodesSourceException, ConfigurationException {
        return parseFile(new File(file), framework, project);
    }

    /**
     * Utility method to directly parse the nodes from a file
     */
    public static INodeSet parseFile(final File file, final Framework framework, final String project) throws
        NodesSourceException,
        ConfigurationException {
        final FileNodesSource prov = new FileNodesSource(framework);
        prov.configure(
            FileNodesSource.Configuration.build()
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
     */
    public static INodeSet parseFile(final File file, final Nodes.Format format, final Framework framework, final String project) throws
        NodesSourceException,
        ConfigurationException {
        final FileNodesSource prov = new FileNodesSource(framework);
        prov.configure(
            FileNodesSource.Configuration.build()
                .file(file)
                .includeServerNode(false)
                .generateFileAutomatically(false)
                .project(project)
                .format(format)
                .requireFileExists(true)
        );
        return prov.getNodes();
    }
}
