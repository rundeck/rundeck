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
* NodesYamlParser.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 17, 2011 5:29:00 PM
*
*/
package com.dtolabs.rundeck.core.common;


import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.*;
import java.io.*;

/**
 * NodesYamlParser imports Node data from a YAML formatted input file or datastream.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodesYamlParser implements NodeFileParser {
    private File file;
    private InputStream inputStream;
    private NodeReceiver nodes;

    /**
     * Create parser for a file, and send parsed nodes to the nodes receiver
     *
     * @param file file
     * @param nodes nodes receiver
     */
    public NodesYamlParser(final File file, final NodeReceiver nodes) {
        this.file = file;
        this.nodes = nodes;
    }

    /**
     * Create parser for an inputstream, and send parsed nodes to the nodes receiver
     *
     * @param inputStream input stream
     * @param nodes nodes receiver
     */
    public NodesYamlParser(final InputStream inputStream, final NodeReceiver nodes) {
        this.inputStream = inputStream;
        this.nodes = nodes;
    }

    @SuppressWarnings("unchecked")
    public void parse() throws NodeFileParserException {
        if (null == file && null == inputStream) {
            throw new NullPointerException("file or inputStream was not set");
        }
        final Reader reader;
        final Yaml yaml = new Yaml(new SafeConstructor());
        try {
            if (null != file) {
                reader = new FileReader(file);
            } else {
                reader = new InputStreamReader(inputStream);
            }
            try {
                for (final Object o : yaml.loadAll(reader)) {
                    if (o instanceof Map) {

                        //name->{node data} map
                        final Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) o;
                        for (final Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                            final String nodename = entry.getKey();
                            if (null == entry.getValue()) {
                                throw new NodeFileParserException("Empty node entry for: " + nodename);
                            }
                            if(!(entry.getValue() instanceof Map)) {
                                throw new NodeFileParserException(
                                    "Expected map data for node entry '" + nodename + "', but saw: " + entry.getValue().getClass()
                                        .getName());
                            }
                            final HashMap<String, Object> newmap = new HashMap<String, Object>(entry.getValue());
                            newmap.put("nodename", nodename);
                            final NodeEntryImpl iNodeEntry;
                            try {
                                iNodeEntry = NodeEntryFactory.createFromMap(newmap);
                            } catch (IllegalArgumentException e) {
                                throw new NodeFileParserException(e);
                            }

                            nodes.putNode(iNodeEntry);
                        }
                    } else if (o instanceof Collection) {

                        //list of {node data} maps
                        final Collection<Map<String, Object>> map = (Collection<Map<String, Object>>) o;
                        for (final Map<String, Object> nodemap : map) {
                            final NodeEntryImpl iNodeEntry;
                            try {
                                iNodeEntry = NodeEntryFactory.createFromMap(nodemap);
                            } catch (IllegalArgumentException e) {
                                throw new NodeFileParserException(e);
                            }
                            nodes.putNode(iNodeEntry);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (NodeFileParserException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw new NodeFileParserException(e);
        } catch (Exception e) {
            throw new NodeFileParserException(e);
        }
    }
}
