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
package com.dtolabs.rundeck.core.resources.format.json;


import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ResourceJsonFormatParser parsers JSON format into nodes data
 *
 * @author Greg Schueler &lt;a href="mailto:greg@simplifyops.com"&gt;greg@simplifyops.com&lt;/a&gt;
 */
@Plugin(name = ResourceJsonFormatParser.SERVICE_PROVIDER_TYPE, service = ServiceNameConstants.ResourceFormatParser)
public class ResourceJsonFormatParser implements ResourceFormatParser, Describable {
    public static final String SERVICE_PROVIDER_TYPE = "resourcejson";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final Set<String> FILE_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(
                    Collections.singletonList(
                            "json"
                    )
            )
    );

    public static final Set<String> MIME_TYPES = Collections.unmodifiableSet(
            new HashSet<>(
                    Arrays.asList(
                            "application/json",
                            "text/json"
                    )
            )
    );

    private static final Description DESCRIPTION = DescriptionBuilder.builder()
                                                                     .name(SERVICE_PROVIDER_TYPE)
                                                                     .title("Resource JSON")
                                                                     .description(
                                                                             "The Rundeck Resource JSON format 1.0 " +
                                                                             "(bundled)"
                                                                     )
                                                                     .build();

    public Description getDescription() {
        return DESCRIPTION;
    }

    public Set<String> getFileExtensions() {
        return FILE_EXTENSIONS;
    }

    public Set<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public INodeSet parseDocument(File file) throws ResourceFormatParserException {
        try {
            try (FileInputStream fileInputStream = new FileInputStream((file))) {
                return parseDocument(fileInputStream);
            }
        } catch (IOException e) {
            throw new ResourceFormatParserException(e);
        }
    }

    @Override
    public INodeSet parseDocument(final InputStream input) throws ResourceFormatParserException {
        try {
            return convertNodes(objectMapper.readValue(input, Object.class));
        } catch (IOException e) {
            throw new ResourceFormatParserException(e);
        }
    }

    private INodeSet convertNodes(Object data) throws ResourceFormatParserException {
        if (data instanceof Map) {
            return convertMappedNodes((Map) data);
        } else if (data instanceof Collection) {
            return convertListNodes((Collection) data);
        } else {
            throw new ResourceFormatParserException("JSON structure expected Map or Array, but saw: " + data.getClass());
        }
    }

    private INodeSet convertListNodes(final Collection data) {
        NodeSetImpl nodeSet = new NodeSetImpl();
        for (Object value : data) {

            if (value instanceof Map) {
                Map attrs = (Map) value;
                if (!attrs.containsKey("nodename")) {
                    continue;
                }
                String nodename = attrs.get("nodename").toString();
                addNode(nodeSet, nodename, attrs);

            }
        }
        return nodeSet;
    }

    private INodeSet convertMappedNodes(final Map map) {
        NodeSetImpl nodeSet = new NodeSetImpl();
        for (Object o : map.keySet()) {
            if (o instanceof String) {
                String nodename = o.toString();
                Object value = map.get(o);
                if (value instanceof Map) {
                    Map attrs = (Map) value;
                    addNode(nodeSet, nodename, attrs);

                }
            }
        }
        return nodeSet;
    }

    private void addNode(final NodeSetImpl nodeSet, final String nodename, final Map attrs) {
        NodeEntryImpl node = new NodeEntryImpl(nodename);
        Map<String, String> safe = safe(attrs);
        node.getAttributes().putAll(safe);
        if (safe.get("tags") != null && !"".equals(safe.get("tags").trim())) {
            node.setTags(new HashSet<>(Arrays.asList(safe.get("tags").split(", *"))));
        } else if (attrs.get("tags") instanceof Collection) {
            node.setTags(new HashSet<>(stringSet((Collection) attrs.get("tags"))));
        }
        nodeSet.putNode(node);
    }

    private Set<String> stringSet(final Collection tags) {
        HashSet<String> strings = new HashSet<>();
        for (Object tag : tags) {
            strings.add(tag.toString());
        }
        return strings;
    }

    private Map<String, String> safe(final Map attrs) {
        HashMap<String, String> map = new HashMap<>();
        for (Object o : attrs.keySet()) {
            Object val = attrs.get(o);
            if (val == null) {
                continue;
            }
            if (!(val instanceof Collection) && !(val instanceof Map)) {
                map.put(o.toString(), val.toString());
            }
        }
        return map;
    }


}
