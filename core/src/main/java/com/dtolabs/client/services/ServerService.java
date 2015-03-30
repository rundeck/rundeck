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
* ServerService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 17, 2010 3:49:04 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.client.utils.HttpClientException;
import com.dtolabs.client.utils.WebserviceHttpClient;
import com.dtolabs.client.utils.WebserviceHttpClientFactory;
import com.dtolabs.client.utils.WebserviceResponse;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.common.Framework;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * ServerService provides the ability to make webservice requests to the Web server.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ServerService {
    private WebConnectionParameters connParams;

    /**
     * Create ServerService using the Framework to provdie connection parameters
     *
     * @param url connection url
     * @param username connection username
     * @param password connection password
     */
    public ServerService(final String url, final String username, final String password) {
        this(new WebConnectionParameters() {

            public String getPassword() {
                return password;
            }

            public String getUsername() {
                return username;
            }

            public String getServerUrl() {
                return url;
            }
        });

    }

    /**
     * Create ServerService using the connection parameters
     *
     * @param connParams the connection info
     */
    public ServerService(final WebConnectionParameters connParams) {
        this.connParams = connParams;
    }

    /**
     * Make the request to the ItNav workbench.
     *
     * @param urlPath     the path for the request
     * @param queryParams any query parameters
     * @param uploadFile  a file to upload with the request.
     * @param method      HTTP connection method, e.g. "get","post","put","delete".
     *
     * @param uploadFileParam name of the uploaded file param
     * @return parsed XML document, or null
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *                                        if an error occurs
     * @throws java.net.MalformedURLException if connection URL or urlPath params are malformed.
     */
    public WebserviceResponse makeRundeckRequest(final String urlPath, final Map queryParams, final File uploadFile,
                                                 final String method, final String uploadFileParam)
        throws CoreException, MalformedURLException {
        return makeRundeckRequest(urlPath, queryParams, uploadFile, method, null, uploadFileParam);
    }
    /**
     * Make the request to the ItNav workbench.
     *
     * @param urlPath     the path for the request
     * @param queryParams any query parameters
     * @param formData form data
     *
     * @return parsed XML document, or null
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *                                        if an error occurs
     * @throws java.net.MalformedURLException if connection URL or urlPath params are malformed.
     */
    public WebserviceResponse makeRundeckRequest(final String urlPath,
                                                 final Map queryParams,
                                                 final Map<String, ? extends Object> formData)
        throws CoreException, MalformedURLException {
        return makeRundeckRequest(urlPath, queryParams, null, null, null, formData, null);
    }
    /**
     * Make the request to the ItNav workbench.
     *
     * @param uploadFileParam name of the uploaded file param
     * @param urlPath     the path for the request
     * @param queryParams any query parameters
     * @param uploadFile  a file to upload with the request.
     * @param method      HTTP connection method, e.g. "get","post","put","delete".
     * @param expectedContentType content type
     *
     * @return parsed XML document, or null
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *                                        if an error occurs
     * @throws java.net.MalformedURLException if connection URL or urlPath params are malformed.
     */
    public WebserviceResponse makeRundeckRequest(final String urlPath,
                                                 final Map queryParams,
                                                 final File uploadFile,
                                                 final String method,
                                                 final String expectedContentType,
                                                 final String uploadFileParam)
        throws CoreException, MalformedURLException {
        return makeRundeckRequest(urlPath, queryParams, uploadFile, method, expectedContentType, null, uploadFileParam);
    }
    /**
     * Make the request to the ItNav workbench.
     *
     * @param uploadFileParam name of the uploaded file param
     * @param urlPath     the path for the request
     * @param queryParams any query parameters
     * @param uploadFile  a file to upload with the request.
     * @param method      HTTP connection method, e.g. "get","post","put","delete".
     * @param expectedContentType expected content type
     * @param formData data
     *
     * @return parsed XML document, or null
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *                                        if an error occurs
     * @throws java.net.MalformedURLException if connection URL or urlPath params are malformed.
     */
    public WebserviceResponse makeRundeckRequest(final String urlPath,
                                                 final Map queryParams,
                                                 final File uploadFile,
                                                 final String method,
                                                 final String expectedContentType,
                                                 final Map<String, ? extends Object> formData,
                                                 final String uploadFileParam)
        throws CoreException, MalformedURLException {
        if (null == connParams) {
            throw new IllegalArgumentException("WebConnectionParameters must be specified");
        }

        final URL jcUrl = new URL(connParams.getServerUrl());
        final String jcBasePath = jcUrl.getPath();
        final WebserviceHttpClient hc ;
        if(null==formData || formData.size()<1){
            hc= WebserviceHttpClientFactory.getInstance().getWebserviceHttpClient(jcUrl
                                                                                                       + urlPath,
            jcBasePath,
            connParams.getUsername(),
            connParams.getPassword(),
            queryParams,
            uploadFile,
            uploadFileParam,null,expectedContentType);
        }else{
            hc = WebserviceHttpClientFactory.getInstance().getWebserviceHttpClient(jcUrl
                                                                                   + urlPath,
                jcBasePath,
                connParams.getUsername(),
                connParams.getPassword(),
                queryParams,
                formData);
        }
        hc.setRequestHeader("X-Rundeck-API-XML-Response-Wrapper","true");
        if (null != method) {
            hc.setMethodType(method);
        }
        try {
            hc.makeRequest();
        } catch (IOException e) {
            throw new CoreException("Error making server request to " + jcUrl + ": " + e.getMessage(), e);
        } catch (HttpClientException e) {
            throw new CoreException("Error making server request to " + jcUrl + ": " + e.getMessage(), e);
        }
        return hc;
    }


    /**
     * Return the connection params configured for this ServerService
     * @return connection params
     */
    public WebConnectionParameters getConnParams() {
        return connParams;
    }

    /**
     * Set the connection params.
     * @param connParams connection params
     */
    public void setConnParams(final WebConnectionParameters connParams) {
        this.connParams = connParams;
    }

    /**
     * An interface for providing connection parameters for the web app
     */
    public static interface WebConnectionParameters {
        /**
         * Return the password
         *
         * @return password
         */
        public String getPassword();

        /**
         * Return the user name
         *
         * @return username
         */
        public String getUsername();

        /**
         * Return the URL
         *
         * @return connection URL
         */
        public String getServerUrl();
    }

}
