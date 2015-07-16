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


import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorException;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * ResourceJsonFormatGenerator generates JSON format for nodes data
 *
 * @author Greg Schueler &lt;a href="mailto:greg@simplifyops.com"&gt;greg@simplifyops.com&lt;/a&gt;
 */
@Plugin(name = ResourceJsonFormatGenerator.SERVICE_PROVIDER_TYPE, service = ServiceNameConstants.ResourceFormatGenerator)
public class ResourceJsonFormatGenerator implements ResourceFormatGenerator,Describable {
    public static final String SERVICE_PROVIDER_TYPE = "resourcejson";
    private final ObjectMapper test = new ObjectMapper();

    public static final Set<String> EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(
                    Collections.singletonList(
                            "json"
                    )
            )
    );
    public static final List<String> MIME_TYPES = Collections.unmodifiableList(
            Arrays.asList(
                    "application/json", "text/json"
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
        return EXTENSIONS;
    }

    public List<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public void generateDocument(INodeSet nodeset, OutputStream stream) throws ResourceFormatGeneratorException {
        try {
            test.writeValue(stream, convertNodes(nodeset));
        } catch (IOException e) {
            throw new ResourceFormatGeneratorException(e);
        }
    }

    private Object convertNodes(final INodeSet nodeset) {
        HashMap<String, Map<String, String>> stringMapHashMap = new HashMap<>();
        for (INodeEntry node : nodeset) {
            HashMap<String, String> map = new HashMap<>();
            map.putAll(node.getAttributes());
            stringMapHashMap.put(node.getNodename(), map);
        }
        return stringMapHashMap;
    }

}
