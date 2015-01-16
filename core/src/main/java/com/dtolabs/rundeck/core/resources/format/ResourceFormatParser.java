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
import java.io.InputStream;
import java.util.*;

/**
 * ResourceFormatParser is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ResourceFormatParser {
    /**
     * @return  the list of file extensions that this format parser can parse.
     */
    public Set<String> getFileExtensions();

    /**
     * @return  the list of MIME types that this format parser can parse. This may include wildcards such as
     * "*&#47;xml".
     */
    public Set<String> getMIMETypes();

    /**
     * Parse a file
     * @return nodes
     * @param file input file
     * @throws ResourceFormatParserException on parse error
     */
    public INodeSet parseDocument(File file) throws ResourceFormatParserException;

    /**
     * Parse an input stream
     * @return nodes
     * @param input input stream
     * @throws ResourceFormatParserException on parse error
     */
    public INodeSet parseDocument(InputStream input) throws ResourceFormatParserException;
}
