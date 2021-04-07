/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.dtolabs.rundeck.core.http.ApacheHttpClient;
import com.dtolabs.rundeck.core.http.HttpClient;
import com.dtolabs.utils.Streams;
import org.apache.http.*;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

/**
 * URLUpdater updates a file by getting the contents of a url, with optional caching, and mime type accept header.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class URLFileUpdater implements FileUpdater {
    static final        Logger  logger            = LoggerFactory.getLogger(URLFileUpdater.class.getName());
    public static final String  CONTENT_TYPE      = "Content-Type";
    public static final String  E_TAG             = "ETag";
    public static final String  IF_NONE_MATCH     = "If-None-Match";
    public static final String  LAST_MODIFIED     = "Last-Modified";
    public static final String  IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final int     DEFAULT_TIMEOUT   = 60;
    public static final Factory FACTORY           = new Factory();
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
        HttpClient<HttpResponse> httpClient = new ApacheHttpClient();
        final Properties cacheProperties;
        if (useCaching) {
            cacheProperties = loadCacheData(cacheMetadata);
            contentTypeFromCache(cacheProperties);
        } else {
            cacheProperties = null;
        }

        String cleanUrl = url.toExternalForm().replaceAll("^(https?://)([^:@/]+):[^@/]*@", "$1$2:****@");
        String urlToUse = url.toExternalForm();
        try {
            urlToUse = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()).toExternalForm();
            httpClient.setUri(url.toURI());
            if (null != url.getUserInfo()) {
                UsernamePasswordCredentials cred = new UsernamePasswordCredentials(url.getUserInfo());
                httpClient.setBasicAuthCredentials(cred.getUserName(),cred.getPassword());
            } else if (null != username && null != password) {
                httpClient.setBasicAuthCredentials(username,password);
            }

        } catch (MalformedURLException | URISyntaxException e) {
            throw new FileUpdaterException("Failed to configure base URL for authentication: " + e.getMessage(),
                e);
        }
        httpClient.setTimeout(timeout * 1000);
        httpClient.setFollowRedirects(true);
        httpClient.setMethod(HttpClient.Method.GET);
        httpClient.addHeader("Accept",Optional.ofNullable(acceptHeader).orElse("*/*"));

        if (useCaching) {
            applyCacheHeaders(cacheProperties, httpClient);
        }

        logger.debug("Making remote request: " + cleanUrl);
        try {
            httpClient.execute((HttpResponse rsp) -> {
                resultCode = rsp.getStatusLine().getStatusCode();
                reasonCode = rsp.getStatusLine().getReasonPhrase();

                if (useCaching && HttpStatus.SC_NOT_MODIFIED == resultCode) {
                    logger.debug("Content NOT MODIFIED: file up to date");
                } else if (HttpStatus.SC_OK == resultCode) {
                    determineContentType(rsp);

                    try (FileOutputStream output = new FileOutputStream(destinationFile)) {
                        //write to file
                        Streams.copyStream(rsp.getEntity().getContent(), output);
                    }
                    if (destinationFile.length() < 1) {
                        //file was empty!
                        if(!destinationFile.delete()) {
                            logger.warn("Failed to remove empty file: " + destinationFile.getAbsolutePath());
                        }
                    }
                    if (useCaching) {
                        cacheResponseInfo(rsp, cacheMetadata);
                    }
                } else {
                    throw new FileUpdaterException(
                            "Unable to retrieve content: result code: " + resultCode + " " + reasonCode);
                }


            });
        }  catch (Exception e) {
            throw new FileUpdaterException(e);
        }
    }

    /**
     * Add appropriate cache headers to the request method, but only if there is valid data in the cache (content type
     * as well as file content)
     */
    private void applyCacheHeaders(final Properties cacheProperties, final HttpClient<HttpResponse> client) {
        if (isCachedContentPresent() && null != contentType) {
            if (cacheProperties.containsKey(E_TAG)) {
                client.addHeader(IF_NONE_MATCH,cacheProperties.getProperty(E_TAG));
            }
            if (cacheProperties.containsKey(LAST_MODIFIED)) {
                client.addHeader(IF_MODIFIED_SINCE, cacheProperties.getProperty(LAST_MODIFIED));
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

    private void determineContentType(final HttpResponse response) {
        if (null != response.getEntity().getContentType().getValue()) {
            contentType = response.getEntity().getContentType().getValue();
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
                try (FileInputStream fileInputStream = new FileInputStream(cacheFile)) {
                    cacheProperties.load(fileInputStream);
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
    private void cacheResponseInfo(final HttpResponse httpResponse, final File cacheFile) {
        //write cache data to file if present
        Properties newprops = new Properties();

        if (null != httpResponse.getFirstHeader(LAST_MODIFIED)) {
            newprops.setProperty(LAST_MODIFIED, httpResponse.getFirstHeader(LAST_MODIFIED).getValue());
        }
        if (null != httpResponse.getFirstHeader(E_TAG)) {
            newprops.setProperty(E_TAG, httpResponse.getFirstHeader(E_TAG).getValue());
        }
        if (null != httpResponse.getFirstHeader(CONTENT_TYPE)) {
            newprops.setProperty(CONTENT_TYPE, httpResponse.getFirstHeader(CONTENT_TYPE).getValue());
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
