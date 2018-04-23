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
* FileResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 11:28 AM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.configuration.Configurable;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.resources.format.*;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.util.List;
import java.util.Properties;

import static com.dtolabs.rundeck.plugins.util.DescriptionBuilder.buildDescriptionWith;

/**
 * FileResourceModelSource extends {@link BaseFileResourceModelSource} to
 * provide an optionally editable Model source using a local file system path.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileResourceModelSource extends BaseFileResourceModelSource implements Configurable {
    private Configuration configuration;

    FileResourceModelSource(final Framework framework) {
        super(framework);
    }

    static Description createDescription(final List<String> formats) {
        return buildDescriptionWith(d -> d
                .name("file")
                .title("File")
                .description("Reads a file containing node definitions in a supported format")
                .property(p -> p
                        .freeSelect(Configuration.FORMAT)
                        .title("Format")
                        .description("Format of the file")
                        .values(formats)
                )
                .property(p -> p
                        .string(Configuration.FILE)
                        .title("File Path")
                        .description("Path of the file")
                        .required(true)
                )
                .property(p -> p
                        .booleanType(Configuration.GENERATE_FILE_AUTOMATICALLY)
                        .title("Generate")
                        .description(
                                "Automatically generate the file if it is " +
                                "missing?\n\nAlso creates missing directories.")
                        .required(true)
                        .defaultValue("false")
                )
                .property(p -> p
                        .booleanType(Configuration.INCLUDE_SERVER_NODE)
                        .title("Include Server Node")
                        .description(
                                "Automatically include the server node in the " +
                                "generated file?")
                        .required(true)
                        .defaultValue("false")
                )
                .property(p -> p
                        .booleanType(Configuration.REQUIRE_FILE_EXISTS)
                        .title("Require File Exists")
                        .description("Require that the file exists")
                        .required(true)
                        .defaultValue("false")
                )
                .property(p -> p
                        .booleanType(Configuration.WRITEABLE)
                        .title("Writeable")
                        .description("Allow this file to be editable.")
                        .required(false)
                        .defaultValue("false")
                )

        );
    }

    public void configure(final Properties configs) throws ConfigurationException {
        final Configuration configuration1 = Configuration.fromProperties(configs);
        configure(configuration1);
    }

    /**
     * Configure the Source
     *
     * @param configuration configuration
     *
     * @throws com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException on config error
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
    }

    public long writeFileData(final InputStream dataStream) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(configuration.nodesFile)) {
            return Streams.copyStream(dataStream, fos);
        }
    }


    @Override
    public InputStream openFileDataInputStream() throws IOException, ResourceModelSourceException {
        if (!configuration.nodesFile.exists() && configuration.generateFileAutomatically) {
            generateResourcesFile(configuration.nodesFile, configuration.format);
        }

        if (configuration.nodesFile.isFile()) {
            return new FileInputStream(configuration.nodesFile);
        } else if (configuration.requireFileExists) {
            throw new ResourceModelSourceException("File does not exist: " + configuration.nodesFile);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean hasData() {
        return configuration.requireFileExists ||
               configuration.generateFileAutomatically ||
               configuration.nodesFile.exists();
    }

    @Override
    protected boolean isSupportsLastModified() {
        return true;
    }

    @Override
    protected long getLastModified() {
        return configuration.nodesFile.exists() ? configuration.nodesFile.lastModified() : 0;
    }

    @Override
    protected String getResourceFormat() {
        return configuration.format;
    }

    @Override
    protected String getDocumentFileExtension() {
        return ResourceFormatParserService.getFileExtension(configuration.nodesFile.getName());
    }

    @Override
    public String getSourceDescription() {
        return configuration.nodesFile.getAbsolutePath();
    }

    @Override
    public boolean isDataWritable() {
        return configuration.writeable;
    }

    @Override
    protected boolean shouldGenerateServerNode() {
        return configuration.includeServerNode;
    }

    /**
     * Utility method to directly parse the nodes from a file
     *
     * @param file      file
     * @param framework fwk
     * @param project   project name
     *
     * @return nodes
     *
     * @throws ResourceModelSourceException if an error occurs
     * @throws ConfigurationException       if a configuration error occurs
     */
    public static INodeSet parseFile(final File file, final Framework framework, final String project) throws
            ResourceModelSourceException,
            ConfigurationException
    {
        final FileResourceModelSource prov = new FileResourceModelSource(framework);
        prov.configure(
                Configuration.build()
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
     *
     * @param file      file
     * @param format    specified format
     * @param framework fwk
     * @param project   project name
     *
     * @return nodes
     *
     * @throws ResourceModelSourceException if an error occurs
     * @throws ConfigurationException       if a configuration error occurs
     */
    public static INodeSet parseFile(
            final File file, final String format, final Framework framework,
            final String project
    ) throws
            ResourceModelSourceException,
            ConfigurationException
    {
        final FileResourceModelSource prov = new FileResourceModelSource(framework);
        prov.configure(
                Configuration.build()
                             .file(file)
                             .includeServerNode(false)
                             .generateFileAutomatically(false)
                             .project(project)
                             .format(format)
                             .requireFileExists(true)
        );
        return prov.getNodes();
    }


    private void generateResourcesFile(final File resfile, final String format) throws ResourceModelSourceException {
        final NodeEntryImpl node = framework.createFrameworkNode();
        node.setFrameworkProject(configuration.project);
        final ResourceFormatGenerator generator;
        if (null != format) {
            try {
                generator = framework.getResourceFormatGeneratorService().getGeneratorForFormat(format);
            } catch (UnsupportedFormatException e) {
                throw new ResourceModelSourceException(e);
            }
        }
        else {
            try {
                generator = framework.getResourceFormatGeneratorService().getGeneratorForFileExtension(resfile);
            } catch (UnsupportedFormatException e) {
                throw new ResourceModelSourceException(e);
            }
        }

        NodeSetImpl nodes = new NodeSetImpl();
        if (configuration.includeServerNode) {
            nodes.putNode(node);
        }

        if (!resfile.getParentFile().exists()) {
            if (!resfile.getParentFile().mkdirs()) {
                throw new ResourceModelSourceException(
                    "Parent dir for resource file does not exists, and could not be created: " + resfile
                );
            }
        }

        try {
            try (FileOutputStream stream = new FileOutputStream(resfile)) {
                generator.generateDocument(nodes, stream);
            }
        } catch (IOException | ResourceFormatGeneratorException e) {
            throw new ResourceModelSourceException(e);
        }

    }


    public static class Configuration {
        public static final String GENERATE_FILE_AUTOMATICALLY = "generateFileAutomatically";
        public static final String INCLUDE_SERVER_NODE = "includeServerNode";
        public static final String FILE = "file";
        public static final String PROJECT = "project";
        public static final String FORMAT = "format";
        public static final String REQUIRE_FILE_EXISTS = "requireFileExists";
        public static final String WRITEABLE = "writeable";
        String format;
        File nodesFile;
        String project;
        boolean generateFileAutomatically;
        boolean includeServerNode;
        boolean requireFileExists;
        final Properties configuration;
        boolean writeable;

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

        public Configuration writeable(boolean writeable) {
            this.writeable = writeable;
            configuration.put(WRITEABLE, Boolean.toString(writeable));
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
            if (configuration.containsKey(WRITEABLE)) {
                writeable = Boolean.parseBoolean(configuration.getProperty(WRITEABLE));
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
                   ", writeable=" + writeable +
                   ", configuration=" + configuration +
                   '}';
        }
    }

    @Override
    public String toString() {
        return "FileResourceModelSource{" +
               "file=" + configuration.nodesFile +
               ", format=" + configuration.format +
               '}';
    }
}
