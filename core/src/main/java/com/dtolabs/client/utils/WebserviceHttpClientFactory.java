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
 * WebserviceHttpClientFactory.java
 * 
 * User: greg
 * Created: Jan 14, 2005 4:44:58 PM
 * $Id: WebserviceHttpClientFactory.java 7769 2008-02-07 00:50:23Z gschueler $
 */
package com.dtolabs.client.utils;


import java.io.File;
import java.io.OutputStream;
import java.util.Map;


/**
 * WebserviceHttpClientFactory creates instances of {@link WebserviceHttpClient}. The
 * default implementation of WebserviceHttpClientFactory can be obtained by the {@link #getInstance()} method. A
 * different implementation can be installed at runtime by the {@link #setInstance(WebserviceHttpClientFactory)}
 * method, and that instance will be returned by {@link #getInstance()} . <br> Once a factory is obtained, the {@link
 * #getWebserviceHttpClient(String, String, String, String, java.util.Map)}  getWebserviceHttpClient} method will return a
 * {@link WebserviceHttpClient} instance.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 7769 $
 */
public abstract class WebserviceHttpClientFactory {

    private static WebserviceHttpClientFactory instance;

    /**
     * Default factory, which creates a WebserviceHttpClientChannel and uses the WebserviceFormAuthenticator
     */
    private static class Default extends WebserviceHttpClientFactory {
        public WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                          final String basePath,
                                                          final String username,
                                                          final String password,
                                                          final Map query) {

            return new WebserviceHttpClientChannel(urlSpec,
                                                  new WebserviceFormAuthenticator(basePath, username, password),
                                                  query);

        }

        public WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                          final String basePath,
                                                          final String username,
                                                          final String password,
                                                          final Map query,
                                                          final File uploadFile,
                                                          final String fileparam) {

            return new WebserviceHttpClientChannel(urlSpec,
                                                  new WebserviceFormAuthenticator(basePath, username, password),
                                                  query,
                                                  uploadFile, fileparam);

        }

        public WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                          final String basePath,
                                                          final String username,
                                                          final String password,
                                                          final Map query,
                                                          final File uploadFile,
                                                          final String fileparam,
                                                          final OutputStream destination,
                                                          final String expectedContentType) {

            return new WebserviceHttpClientChannel(urlSpec,
                                                  new WebserviceFormAuthenticator(basePath, username, password),
                                                  query,
                                                  uploadFile,
                                                  fileparam,
                                                  destination,
                                                  expectedContentType);

        }

        public WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                          final String basePath,
                                                          final String username,
                                                          final String password,
                                                          final Map query,
                                                          final Map<String,? extends Object> formData) {

            return new WebserviceHttpClientChannel(urlSpec,
                                                  new WebserviceFormAuthenticator(basePath, username, password),
                                                  query,
                                                  formData);

        }

        public WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                          final String basePath,
                                                          final String username,
                                                          final String password,
                                                          final Map query,
                                                          final OutputStream destination,
                                                          final String expectedContentType) {

            return new WebserviceHttpClientChannel(urlSpec,
                                                  new WebserviceFormAuthenticator(basePath, username, password),
                                                  query,
                                                  destination,
                                                  expectedContentType);

        }

    }

    /**
     * Get the Factory instance.
     *
     * @return instance of the Factory
     */
    public synchronized static WebserviceHttpClientFactory getInstance() {
        if (null == instance) {
            instance = new Default();
        }
        return instance;
    }

    /**
     * Set the factory instance to use.  Default factory is used if not set.
     *
     * @param factory the factory
     */
    public synchronized static void setInstance(final WebserviceHttpClientFactory factory) {
        instance = factory;
    }

    /**
     * Get a WebserviceHttpClient from the parameters.
     *
     * @param urlSpec  URL to request
     * @param basePath base context path on the server for the Webservice application
     * @param username username to user
     * @param password password to use
     * @param query    query parameters to add to the request
     *
     * @return WebserviceHttpClient instance
     */
    public abstract WebserviceHttpClient getWebserviceHttpClient(String urlSpec,
                                                               String basePath,
                                                               String username,
                                                               String password,
                                                               Map query
    );


    /**
     * Get a WebserviceHttpClient from the parameters.
     *
     * @param urlSpec    URL to request
     * @param basePath   base context path on the server for the Webservice application
     * @param username   username to user
     * @param password   password to use
     * @param query      query parameters to add to the request
     * @param uploadFile file to upload
     * @param fileparam  name of the file upload parameter
     *
     * @return WebserviceHttpClient instance
     */
    public abstract WebserviceHttpClient getWebserviceHttpClient(String urlSpec,
                                                               String basePath,
                                                               String username,
                                                               String password,
                                                               Map query,
                                                               File uploadFile,
                                                               String fileparam);

    /**
     * Get a WebserviceHttpClient from the parameters.
     *
     * @param urlSpec             URL to request
     * @param basePath            base context path on the server for the Webservice application
     * @param username            username to user
     * @param password            password to use
     * @param query               query parameters to add to the request
     * @param uploadFile          file to upload
     * @param fileparam           name of the file upload parameter
     * @param destination         an OutputStream to which to write the result data
     * @param expectedContentType the content type expected.  if the type does not match, no data is written to the
     *                            outputstream.  if null, any type is allowed.
     *
     * @return WebserviceHttpClient instance
     */
    public abstract WebserviceHttpClient getWebserviceHttpClient(String urlSpec,
                                                               String basePath,
                                                               String username,
                                                               String password,
                                                               Map query,
                                                               File uploadFile,
                                                               String fileparam,
                                                               OutputStream destination,
                                                               String expectedContentType);

    /**
     * Get a WebserviceHttpClient from the parameters.
     *
     * @param urlSpec             URL to request
     * @param basePath            base context path on the server for the Webservice application
     * @param username            username to user
     * @param password            password to use
     * @param query               query parameters to add to the request
     * @param destination         an OutputStream to which to write the result data
     * @param expectedContentType the content type expected.  if the type does not match, no data is written to the
     *                            outputstream.  if null, any type is allowed.
     *
     * @return WebserviceHttpClient instance
     */
    public abstract WebserviceHttpClient getWebserviceHttpClient(String urlSpec,
                                                               String basePath,
                                                               String username,
                                                               String password,
                                                               Map query,
                                                               OutputStream destination,
                                                               String expectedContentType);

    /**
     * Get a WebserviceHttpClient from the parameters.
     *
     * @param urlSpec             URL to request
     * @param basePath            base context path on the server for the Webservice application
     * @param username            username to user
     * @param password            password to use
     * @param query               query parameters to add to the request
     * @param formData            form data
     *
     * @return WebserviceHttpClient instance
     */
    public abstract WebserviceHttpClient getWebserviceHttpClient(final String urlSpec,
                                                        final String basePath,
                                                        final String username,
                                                        final String password,
                                                        final Map query,
                                                        final Map<String, ? extends Object> formData);
}