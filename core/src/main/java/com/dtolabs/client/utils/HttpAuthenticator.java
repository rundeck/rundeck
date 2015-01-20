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
 * ColonyHttpAuthenticator.java
 * 
 * User: greg
 * Created: Jan 14, 2005 5:05:47 PM
 * $Id: HttpAuthenticator.java 8473 2008-07-23 17:40:15Z gschueler $
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;


/**
 * ColonyHttpAuthenticator is the interface for an authentication module for use by a client instance.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 8473 $
 */
public interface HttpAuthenticator {
    /**
     * Authenticate the client http state so that the colony requests can be made.
     *
     * @param reqUrl URL requested for colony
     * @param client HttpClient instance
     *
     * @return true if authentication succeeded.
     *
     * @throws HttpClientException on error
     */
    boolean authenticate(URL reqUrl, HttpClient client) throws HttpClientException;

    /**
     * Gets the user login name used when instantiating the authenticator
     * @return Login name
     */
    String getUsername();

    /**
     * Return true if the result from the get method indicates re-authentication is needed
     * @param resultCode result code
     * @param method request
     * @return true if re-authentication is needed
     */
    boolean needsReAuthentication(int resultCode, HttpMethod method);
}
