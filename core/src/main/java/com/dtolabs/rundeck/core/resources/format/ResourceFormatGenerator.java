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
* ResourceFormatParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/4/11 9:48 AM
* 
*/
package com.dtolabs.rundeck.core.resources.format;

import com.dtolabs.rundeck.core.common.INodeSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * ResourceFormatGenerator generates a resources document from a set of nodes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ResourceFormatGenerator {

    /**
     * @return the list of file extensions that this format generator can generate
     */
    public Set<String> getFileExtensions();

    /**
     * @return the list of MIME types that this format generator can generate. If more than one
     * are returned, then the first value will be used by default if necessary.
     */
    public List<String> getMIMETypes();

    /**
     * generate formatted output
     * @param nodeset nodes
     * @param stream output stream
     * @throws IOException on io error
     * @throws ResourceFormatGeneratorException on format error
     */
    public void generateDocument(INodeSet nodeset, OutputStream stream) throws ResourceFormatGeneratorException,
        IOException;
}
