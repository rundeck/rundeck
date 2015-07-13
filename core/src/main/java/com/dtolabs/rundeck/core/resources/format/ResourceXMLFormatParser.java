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
* ResourceXMLParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/4/11 9:57 AM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeFileParserException;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.common.NodesXMLParser;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * ResourceXMLParser is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "resourcexml", service = "ResourceFormatParser")
public class ResourceXMLFormatParser implements ResourceFormatParser, Describable {

    public static final String SERVICE_PROVIDER_TYPE = "resourcexml";

    public static final Set<String> EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("xml")));
    public static final Set<String> MIME_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "*/xml")));

    public Set<String> getFileExtensions() {
        return EXTENSIONS;
    }

    public Set<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public INodeSet parseDocument(final File file) throws ResourceFormatParserException {
        final NodeSetImpl nodeReceiver = new NodeSetImpl();
        try {
            new NodesXMLParser(file, nodeReceiver).parse();
        } catch (NodeFileParserException e) {
            throw new ResourceFormatParserException(e);
        }
        return nodeReceiver;
    }

    public INodeSet parseDocument(final InputStream input) throws ResourceFormatParserException {
        final NodeSetImpl nodeReceiver = new NodeSetImpl();
        try {
            new NodesXMLParser(input, nodeReceiver).parse();
        } catch (NodeFileParserException e) {
            throw new ResourceFormatParserException(e);
        }
        return nodeReceiver;
    }

    private static final Description DESCRIPTION = DescriptionBuilder.builder()
        .name(SERVICE_PROVIDER_TYPE)
        .title("Resource XML")
        .description("The Rundeck Resource XML format 1.3 (bundled)")
        .build();

    public Description getDescription() {
        return DESCRIPTION;
    }
}
