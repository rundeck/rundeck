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
* ResourceYamlFormatGenerator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/4/11 5:15 PM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodesGeneratorException;
import com.dtolabs.rundeck.core.common.NodesYamlGenerator;
import com.dtolabs.rundeck.core.plugins.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * ResourceYamlFormatGenerator is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "resourceyaml", service = "ResourceFormatGenerator")
public class ResourceYamlFormatGenerator implements ResourceFormatGenerator {
    public static final String SERVICE_PROVIDER_TYPE = "resourceyaml";

    public static final Set<String> EXTENSIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("yaml",
        "yml")));
    public static final Set<String> MIME_TYPES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "text/yaml", "text/x-yaml", "application/yaml", "application/x-yaml")));

    public Set<String> getFileExtensions() {
        return EXTENSIONS;
    }

    public Set<String> getMIMETypes() {
        return MIME_TYPES;
    }

    public void generateDocument(final INodeSet nodeset, final OutputStream stream) throws
        ResourceFormatGeneratorException,
        IOException {
        final NodesYamlGenerator nodesYamlGenerator = new NodesYamlGenerator(stream);
        nodesYamlGenerator.addNodes(nodeset.getNodes());
        try {
            nodesYamlGenerator.generate();
        } catch (NodesGeneratorException e) {
            throw new ResourceFormatGeneratorException(e);
        }
    }
}
