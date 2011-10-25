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
* JsonResourceFormatParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 3:14 PM
* 
*/
package com.dtolabs.rundeck.plugin.resources.format.json;


import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * JsonResourceFormatGenerator generates JSON format from nodes data
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "resourcejson", service = "ResourceFormatGenerator")
public class JsonResourceFormatGenerator implements ResourceFormatGenerator {
    public static final String SERVICE_PROVIDER_TYPE = "resourcejson";
    private static final Gson test = new GsonBuilder().setPrettyPrinting().create();

    public static final Set<String> EXTENSIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "json")));
    public static final List<String> MIME_TYPES = Collections.unmodifiableList(Arrays.asList(
        "application/json", "text/json"));

    public Set<String> getFileExtensions() {
        return EXTENSIONS;
    }

    public List<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public void generateDocument(final INodeSet nodeset, final OutputStream stream) throws
        ResourceFormatGeneratorException {
        try {
            final NodeSetImpl nodeSet = new NodeSetImpl();
            nodeSet.putNodes(nodeset);

            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream, "UTF-8");
            try {
                outputStreamWriter.write(test.toJson(nodesFromNodeSet(nodeset)));
            } finally {
                outputStreamWriter.close();
            }
        } catch (IOException e) {
            throw new ResourceFormatGeneratorException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Nodes nodesFromNodeSet(final INodeSet nodeset) {
        final Nodes nodes = new Nodes();
        final TreeMap<String, Node> map = new TreeMap<String, Node>();
        for (final INodeEntry iNodeEntry : nodeset.getNodes()) {

            final String name = iNodeEntry.getNodename();
            final Node nodeEntry = new Node();
            if (null != iNodeEntry.getTags()) {
                nodeEntry.setTags(iNodeEntry.getTags());
            }
            if (null != iNodeEntry.getAttributes()) {
                nodeEntry.setAttributes(new HashMap<String, String>(iNodeEntry.getAttributes()));
                //remove values that are represented already
                nodeEntry.getAttributes().remove("tags");
                nodeEntry.getAttributes().remove("nodename");
            }
            map.put(name, nodeEntry);
        }
        nodes.setNodes(map);

        return nodes;
    }
}
