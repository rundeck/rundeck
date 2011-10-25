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


import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.*;

/**
 * JsonResourceFormatParser parsers JSON format into nodes data
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "resourcejson", service = "ResourceFormatParser")
public class JsonResourceFormatParser implements ResourceFormatParser {
    public static final String SERVICE_PROVIDER_TYPE = "resourcejson";
    private final Gson gson;

    public static final Set<String> FILE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "json")));

    public static final Set<String> MIME_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "application/json")));

    public JsonResourceFormatParser() {
        gson = new GsonBuilder().create();
    }

    public Set<String> getFileExtensions() {
        return FILE_EXTENSIONS;
    }

    public Set<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public INodeSet parseDocument(final File file) throws ResourceFormatParserException {
        try {
            final FileInputStream fileInputStream = new FileInputStream((file));
            try {
                return parseDocument(fileInputStream);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ResourceFormatParserException(e);
        }
    }

    public INodeSet parseDocument(final InputStream inputStream) throws ResourceFormatParserException {
        final InputStreamReader json = new InputStreamReader(inputStream);
        final Nodes nodes;
        try {
            try {
                nodes = gson.fromJson(json, Nodes.class);
            } finally {
                json.close();
            }
        } catch (IOException e) {
            throw new ResourceFormatParserException(e);
        } catch (JsonIOException e) {
            throw new ResourceFormatParserException(e);
        } catch (JsonSyntaxException e) {
            throw new ResourceFormatParserException(e);
        }
        return nodeSetFromNodes(nodes);
    }

    static NodeSetImpl nodeSetFromNodes(final Nodes nodes) {
        final NodeSetImpl nodeset = new NodeSetImpl();
        for (final Map.Entry<String, Node> entry : nodes.getNodes().entrySet()) {
            final String name = entry.getKey();
            final Node value = entry.getValue();
            final NodeEntryImpl nodeEntry = new NodeEntryImpl();
            if (null != value.getAttributes()) {
                nodeEntry.setAttributes(value.getAttributes());
            }
            if (null != value.getTags()) {
                nodeEntry.setTags(value.getTags());
            }
            nodeEntry.setNodename(name);
            nodeset.putNode(nodeEntry);
        }
        return nodeset;
    }


}
