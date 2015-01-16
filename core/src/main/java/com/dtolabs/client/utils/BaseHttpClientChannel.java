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
 * BaseHttpClientChannel.java
 * 
 * User: greg
 * Created: Jan 26, 2005 6:23:02 PM
 * $Id: BaseHttpClientChannel.java 8535 2008-08-02 00:58:50Z ahonor $
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Category;

import java.io.OutputStream;
import java.net.URL;
import java.util.Map;


/**
 * BaseHttpClientChannel is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 8535 $
 */
class BaseHttpClientChannel extends HttpClientChannel {
    static Category logger = Category.getInstance(BaseHttpClientChannel.class.getName());

    private HttpAuthenticator authenticator;
    /**
     * Creates a new instance of BaseHttpClientChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     *
     */
    public BaseHttpClientChannel(String urlSpec,
                                   HttpAuthenticator authenticator,
                                   Map query) {
        super(urlSpec, query);
        this.authenticator = authenticator;

    }
    /**
     * Creates a new instance of BaseHttpClientChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     */
    public BaseHttpClientChannel(String urlSpec,
                                      HttpAuthenticator authenticator,
                                      Map query, OutputStream destination) {
        super(urlSpec, query, destination);
        this.authenticator = authenticator;

    }

    /**
     * Creates a new instance of BaseHttpClientChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     */
    public BaseHttpClientChannel(String urlSpec,
                                      HttpAuthenticator authenticator,
                                      Map query, OutputStream destination,
                                      String expectedContentType) {
        super(urlSpec, query, destination, expectedContentType);
        this.authenticator = authenticator;

    }

    /**
     * noop implementation, can be overridden.
     */
    protected void postMakeRequest() {
    }

    /**
     *
     * @param method method
     * @return request entity
     */
    protected RequestEntity getRequestEntity(PostMethod method) {
        return null;
    }

    @Override
    protected NameValuePair[] getRequestBody(PostMethod method) {
        return new NameValuePair[0];
    }

    protected boolean isPostMethod() {
        return false;
    }

    protected boolean doAuthentication(HttpMethod origMethod) throws HttpClientException {
        logger.debug("doAuthentication called");
        HttpState state = ClientState.getHttpState();
        getHttpClient().setState(state);


        //check that the state has a workbench session
        URL reqUrl = getRequestURL();
        logger.debug("calling authenticator");
        if (!authenticator.authenticate(reqUrl, getHttpClient())) {
            ClientState.resetHttpState();
            throw new AuthorizationFailureException("Unable to authenticate user: " + authenticator.getUsername() );
        }


        return true;
    }

    protected boolean needsReAuthentication(int resultCode, HttpMethod method) {
        return authenticator.needsReAuthentication(resultCode, method);
    }
}
