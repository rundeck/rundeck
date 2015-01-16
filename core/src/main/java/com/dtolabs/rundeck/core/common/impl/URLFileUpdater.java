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
* URLUpdater.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/22/11 10:51 AM
* 
*/
package com.dtolabs.rundeck.core.common.impl;

import com.dtolabs.rundeck.core.common.FileUpdater;
import com.dtolabs.rundeck.core.common.FileUpdaterException;
import com.dtolabs.rundeck.core.common.URLFileUpdaterFactory;
import com.dtolabs.utils.Streams;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * URLUpdater updates a file by getting the contents of a url, with optional caching, and mime type accept header.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class URLFileUpdater implements FileUpdater {
    static final Logger logger = Logger.getLogger(URLFileUpdater.class.getName());
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String E_TAG = "ETag";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final int DEFAULT_TIMEOUT = 60;
    public static final Factory FACTORY = new Factory();
    URL url;
    File cacheMetadata;
    File cachedContent;
    private String reasonCode;
    private int resultCode = 0;
    private String acceptHeader;
    private String contentType;
    private boolean useCaching;
    private int timeout;
    private String username;
    private String password;
    private httpClientInteraction interaction=new normalInteraction();

    public void setInteraction(final httpClientInteraction interaction) {
        this.interaction = interaction;
    }

    /**
     * Interface for interaction with HTTPClient api, or mock instance.
     */
    public static interface httpClientInteraction {
        public void setMethod(HttpMethod method);

        public void setClient(HttpClient client);

        public int executeMethod() throws IOException;

        public String getStatusText();

        public InputStream getResponseBodyAsStream() throws IOException;

        public void releaseConnection();

        public void setRequestHeader(String name, String value);

        public Header getResponseHeader(String name);

        void setFollowRedirects(boolean follow);
    }

    static final class normalInteraction implements httpClientInteraction {
        private HttpMethod method;
        private HttpClient client;

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public void setClient(HttpClient client) {
            this.client = client;
        }

        public int executeMethod() throws IOException {
            return client.executeMethod(method);
        }

        public String getStatusText() {
            return method.getStatusText();
        }

        public InputStream getResponseBodyAsStream() throws IOException {
            return method.getResponseBodyAsStream();
        }

        public void releaseConnection() {
            method.releaseConnection();
        }

        public void setRequestHeader(String name, String value) {
            method.setRequestHeader(name, value);
        }

        public Header getResponseHeader(String name) {
            return method.getResponseHeader(name);
        }

        public void setFollowRedirects(boolean follow) {
            method.setFollowRedirects(follow);
        }

    }

    /**
     * Create a URLUpdater
     *
     * @param url               the URL
     * @param acceptHeader      contents of accept header, or null
     * @param timeout           in seconds, -1 means use the default timeout, and 0 means no timeout
     * @param cacheMetadataFile file to store cache metadata
     * @param cachedContent     file containing previously cached content
     * @param useCaching true to use caching
     * @param username username
     * @param password password
     */
    public URLFileUpdater(final URL url, final String acceptHeader, final int timeout, final File cacheMetadataFile,
                          final File cachedContent, final boolean useCaching, final String username,
                          final String password) {
        this.url = url;
        this.cacheMetadata = cacheMetadataFile;
        this.acceptHeader = acceptHeader;
        this.cachedContent = cachedContent;
        this.timeout = timeout >= 0 ? timeout : DEFAULT_TIMEOUT;
        this.useCaching = useCaching;
        this.username = username;
        this.password = password;
    }

    /**
     * @return a URLFileUpdaterFactory for constructing the FileUpdater
     */
    public static URLFileUpdaterFactory factory() {
        return FACTORY;
    }

    /**
     * Factory for constructing URLFileUpdater with basic settings
     */
    public static class Factory implements URLFileUpdaterFactory {
        public FileUpdater fileUpdaterFromURL(final URL url, final String username, final String password) {
            return new URLFileUpdater(url, null, -1, null, null, false, username, password);
        }
    }

    public void updateFile(final File destinationFile) throws FileUpdaterException {
        //get the URL and save to the (temp) file
        if ("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol())) {
            updateHTTPUrl(destinationFile);
        } else if ("file".equalsIgnoreCase(url.getProtocol())) {
            updateFileUrl(destinationFile);
        } else {
            throw new FileUpdaterException("Unsupported protocol: " + url);
        }
    }

    private void updateFileUrl(final File destinationFile) throws FileUpdaterException {
        try {
            final File srfile = new File(new java.net.URI(url.toExternalForm()));
            final FileInputStream in = new FileInputStream(srfile);
            try{
                final FileOutputStream out = new FileOutputStream(destinationFile);
                try {
                    Streams.copyStream(in, out);
                } finally {
                    out.close();
                }
            }finally {
                in.close();
            }
        } catch (URISyntaxException e) {
            throw new FileUpdaterException("Invalid URI: " + url);
        } catch (FileNotFoundException e) {
            throw new FileUpdaterException(e);
        } catch (IOException e) {
            throw new FileUpdaterException(e);
        }
    }

    private void updateHTTPUrl(final File destinationFile) throws FileUpdaterException {
        if (null == interaction) {
            interaction = new normalInteraction();
        }
        final Properties cacheProperties;
        if (useCaching) {
            cacheProperties = loadCacheData(cacheMetadata);
            contentTypeFromCache(cacheProperties);
        } else {
            cacheProperties = null;
        }

        final HttpClientParams params = new HttpClientParams();
        if (timeout > 0) {
            params.setConnectionManagerTimeout(timeout * 1000);
            params.setSoTimeout(timeout * 1000);
        }

        final HttpClient client = new HttpClient(params);
        AuthScope authscope = null;
        UsernamePasswordCredentials cred = null;
        boolean doauth = false;
        String cleanUrl = url.toExternalForm().replaceAll("^(https?://)([^:@/]+):[^@/]*@", "$1$2:****@");
        String urlToUse = url.toExternalForm();
        try {
            if (null != url.getUserInfo()) {
                doauth = true;
                authscope = new AuthScope(url.getHost(),
                    url.getPort() > 0 ? url.getPort() : url.getDefaultPort(),
                    AuthScope.ANY_REALM, "BASIC");
                cred = new UsernamePasswordCredentials(url.getUserInfo());
                urlToUse = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()).toExternalForm();
            } else if (null != username && null != password) {
                doauth = true;
                authscope = new AuthScope(url.getHost(),
                    url.getPort() > 0 ? url.getPort() : url.getDefaultPort(),
                    AuthScope.ANY_REALM, "BASIC");
                cred = new UsernamePasswordCredentials(username + ":" + password);
                urlToUse = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()).toExternalForm();
            }
        } catch (MalformedURLException e) {
            throw new FileUpdaterException("Failed to configure base URL for authentication: " + e.getMessage(),
                e);
        }
        if (doauth) {
            client.getParams().setAuthenticationPreemptive(true);
            client.getState().setCredentials(authscope, cred);
        }
        interaction.setClient(client);
        interaction.setMethod(new GetMethod(urlToUse));
        interaction.setFollowRedirects(true);
        if (null != acceptHeader) {
            interaction.setRequestHeader("Accept", acceptHeader);
        } else {
            interaction.setRequestHeader("Accept", "*/*");
        }

        if (useCaching) {
            applyCacheHeaders(cacheProperties, interaction);
        }

        logger.debug("Making remote request: " + cleanUrl);
        try {
            resultCode = interaction.executeMethod();
            reasonCode = interaction.getStatusText();
            if (useCaching && HttpStatus.SC_NOT_MODIFIED == resultCode) {
                logger.debug("Content NOT MODIFIED: file up to date");
            } else if (HttpStatus.SC_OK == resultCode) {
                determineContentType(interaction);

                //write to file
                FileOutputStream output=new FileOutputStream(destinationFile);
                try{
                    Streams.copyStream(interaction.getResponseBodyAsStream(), output);
                }finally{
                    output.close();
                }
                if (destinationFile.length() < 1) {
                    //file was empty!
                    if(!destinationFile.delete()) {
                        logger.warn("Failed to remove empty file: " + destinationFile.getAbsolutePath());
                    }
                }
                if (useCaching) {
                    cacheResponseInfo(interaction, cacheMetadata);
                }
            } else {
                throw new FileUpdaterException(
                    "Unable to retrieve content: result code: " + resultCode + " " + reasonCode);
            }
        } catch (HttpException e) {
            throw new FileUpdaterException(e);
        } catch (IOException e) {
            throw new FileUpdaterException(e);
        } finally {
            interaction.releaseConnection();
        }
    }

    /**
     * Add appropriate cache headers to the request method, but only if there is valid data in the cache (content type
     * as well as file content)
     */
    private void applyCacheHeaders(final Properties cacheProperties, final httpClientInteraction method) {
        if (isCachedContentPresent() && null != contentType) {
            if (cacheProperties.containsKey(E_TAG)) {
                method.setRequestHeader(IF_NONE_MATCH, cacheProperties.getProperty(E_TAG));
            }
            if (cacheProperties.containsKey(LAST_MODIFIED)) {
                method.setRequestHeader(IF_MODIFIED_SINCE, cacheProperties.getProperty(LAST_MODIFIED));
            }
        }
    }

    private boolean isCachedContentPresent() {
        return cachedContent.isFile() && cachedContent.length() > 0;
    }

    private void contentTypeFromCache(final Properties cacheProperties) {
        if (cacheProperties.containsKey(CONTENT_TYPE)) {
            contentType = cacheProperties.getProperty(CONTENT_TYPE);
            cleanContentType();
        }
    }

    private void determineContentType(final httpClientInteraction method) {
        if (null != method.getResponseHeader(CONTENT_TYPE)) {
            contentType = method.getResponseHeader(CONTENT_TYPE).getValue();
            cleanContentType();
        }
    }

    private void cleanContentType() {
        if (null != contentType && contentType.indexOf(";") > 0) {
            contentType = contentType.substring(0, contentType.indexOf(";")).trim();
        }
    }

    /**
     * Load properties file with some cache data
     *
     * @param cacheFile file
     */
    private Properties loadCacheData(final File cacheFile) {
        final Properties cacheProperties = new Properties();
        if (cacheFile.isFile()) {
            //try to load cache data if present
            try {
                final FileInputStream fileInputStream = new FileInputStream(cacheFile);
                try {
                    cacheProperties.load(fileInputStream);
                } finally {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                logger.debug("failed to load cache data from file: " + cacheFile);
            }
        }
        return cacheProperties;
    }

    /**
     * Cache etag and last-modified header info for a response
     */
    private void cacheResponseInfo(final httpClientInteraction method, final File cacheFile) {
        //write cache data to file if present
        Properties newprops = new Properties();

        if (null != method.getResponseHeader(LAST_MODIFIED)) {
            newprops.setProperty(LAST_MODIFIED, method.getResponseHeader(LAST_MODIFIED).getValue());
        }
        if (null != method.getResponseHeader(E_TAG)) {
            newprops.setProperty(E_TAG, method.getResponseHeader(E_TAG).getValue());
        }
        if (null != method.getResponseHeader(CONTENT_TYPE)) {
            newprops.setProperty(CONTENT_TYPE, method.getResponseHeader(CONTENT_TYPE).getValue());
        }
        if (newprops.size() > 0) {
            try {
                final FileOutputStream fileOutputStream = new FileOutputStream(cacheFile);
                try {
                    newprops.store(fileOutputStream, "URLFileUpdater cache data for URL: "+url);
                } finally {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                logger.debug(
                    "Failed to write cache header info to file: " + cacheFile + ", " + e.getMessage(), e);
            }
        } else if (cacheFile.exists()) {
            if(!cacheFile.delete()) {
                logger.warn("Unable to delete cachefile: " + cacheFile.getAbsolutePath());
            }
        }
    }

    public String getContentType() {
        return contentType;
    }

    public int getResultCode() {
        return resultCode;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}
