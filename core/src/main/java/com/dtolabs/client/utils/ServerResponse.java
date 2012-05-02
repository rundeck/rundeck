/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* ServerResponse.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 17, 2010 4:15:50 PM
* $Id$
*/
package com.dtolabs.client.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * ServerResponse supplies the result of a Webservice request.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ServerResponse {
    /**
     * returns raw results as a Stream
     * @return result stream
     */
    InputStream getResultStream();

    /**
     * return content type
     * @return content type
     */
    String getResultContentType();

    /**
     * Return the response as raw bytes.
     * @return results bytes
     * @throws java.io.IOException if io error during result read
     */
    byte[] getResponseBody() throws IOException;

}
