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
* WebserviceResponse.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 17, 2010 3:54:09 PM
* $Id$
*/
package com.dtolabs.client.utils;

import org.dom4j.Document;

/**
 * WebserviceResponse interface defines results of a request made to Webservice service, extending the base ServerResponse.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface WebserviceResponse extends ServerResponse{
    int getResultCode();

    /**
     * Return true if the response was an error message
     *
     * @return true if the response was an error message
     */
    boolean isErrorResponse();

    /**
     * return true if the resultbean was present
     *
     * @return true if the resultbean was present
     */
    boolean hasResultDoc();

    /**
     * return an XML document if it was parsed successfully. return null otherwise or if none was received.
     *
     * @return an XML document if it was parsed successfully. return null otherwise or if none was received.
     */
    Document getResultDoc();

    /**
     * is the response valid
     *
     * @return true if the response was valid
     */
    boolean isValidResponse();

    /**
     * return a response message
     *
     * @return the response message or null if none is present
     */
    String getResponseMessage();
}
