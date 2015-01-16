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
 * HttpClient.java
 * 
 * User: greg
 * Created: Jan 26, 2005 6:25:43 PM
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;


/**
 * BaseHttpClient provides a simple interface for making and receiving HTTP requests
 * to the server.
 * Instances are obtained via the {@link com.dtolabs.client.utils.WebserviceHttpClientFactory}.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 9811 $
 */
public interface BaseHttpClient extends ServerResponse{
    /**
     * Set a Header field for the request.  Must be made before makeRequest is called.
     * @param name header name
     * @param value header value
     */
    void setRequestHeader(String name, String value);

    /**
     * Makes the HTTP request to workbench.
     * @throws java.io.IOException on io error
     * @throws HttpClientException on request error
     */
    void makeRequest() throws IOException, HttpClientException;

    /**
     * Returns the {@link HttpMethod} used for the request.  Will return null before makeRequest is called.
     * @return the HttpMethod instance used for the request.
     */
    HttpMethod getRequestMethod();

    /**
     * Return the content length retrieved
     * @return content length
     */
    int getContentLengthRetrieved();

    /**
     * Set the HTTP method. Default is GET, and POST will be automatically set if isPostRequest() returns true.
     * @param method GET/POST/PUT/DELETE
     */
    void setMethodType(String method);
}
