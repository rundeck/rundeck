/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
* NodesYamlGenerator.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 17, 2011 4:47:15 PM
*
*/
package com.dtolabs.rundeck.core.common;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.util.*;
import java.io.*;

/**
 * NodesYamlGenerator produces YAML formatted output from a set of {@link INodeEntry} data.  Nodes should be added with
 * the {@link #addNode(INodeEntry)} method, then {@link #generate()} called.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodesYamlGenerator implements NodesFileGenerator {
    private File destfile;
    private OutputStream outputStream;
    private Writer writer;
    private HashMap<String, Map> maps;

    /**
     * Serialize nodes data as yaml to a file.
     * @param destfile destination
     */
    public NodesYamlGenerator(final File destfile) {
        this();
        this.destfile = destfile;
    }

    /**
     * Serialize nodes data as yaml to an outputstream.
     * @param outputStream output
     */
    public NodesYamlGenerator(final OutputStream outputStream) {
        this();
        this.outputStream = outputStream;
    }

    /**
     * Serialize nodes data as yaml to a writer.
     *
     * @param writer writer
     */
    public NodesYamlGenerator(final Writer writer) {
        this();
        this.writer = writer;
    }

    private NodesYamlGenerator() {
        maps = new HashMap<String, Map>();

    }

    public void addNode(final INodeEntry node) {
        maps.put(node.getNodename(), NodeEntryFactory.toMap(node));
    }

    public void addNodes(final Collection<INodeEntry> nodes) {
        for (final INodeEntry iNodeEntry : nodes) {
            addNode(iNodeEntry);
        }
    }

    public void generate() throws IOException, NodesGeneratorException {
        if (null == destfile && null == outputStream && null == writer) {
            throw new NullPointerException("destfile or outputstream was not set");
        }
        if (null == maps || maps.size() < 1) {
            throw new NodesGeneratorException("Node set is empty");
        }
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);

        if (null != writer) {
            yaml.dump(maps, writer);
        } else if (null != destfile) {
            final Writer writeout = new FileWriter(destfile);
            try {
                yaml.dump(maps, writeout);
            } finally {
                writeout.close();
            }
        } else {
            final Writer writeout = new OutputStreamWriter(outputStream);
            try {
                yaml.dump(maps, writeout);
            } finally {
                writeout.close();

            }
        }


    }
}
