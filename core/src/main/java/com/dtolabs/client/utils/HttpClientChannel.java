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

package com.dtolabs.client.utils;


import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.utils.Streams;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.net.URI;
import java.util.*;


/**
 * HttpClientChannel abstract base for making an HTTP request and getting the reply data. Subclasses should implement the
 * abstract methods.  Also, the {@link #preMakeRequest(org.apache.commons.httpclient.HttpMethod) preMakeRequest} method
 * can be overridden.
 */
abstract class HttpClientChannel implements BaseHttpClient {

    static Logger logger = Logger.getLogger(HttpClientChannel.class.getName());
    private HttpClient httpc;
    private HttpMethod httpMethod;
    private InputStream resultStream;
    private String resultType;
    private int resultCode;
    private String reasonCode;
    boolean requestMade = false;
    private String requestUrl;
    private HttpMethod reqMadeMethod;
    private URL requestURL;
    private OutputStream destinationStream;
    private int contentLengthRetrieved;
    private String expectedContentType;
    private String methodType = "GET";
    /** contains request headers to be reused if requests need to be recreated for auth purposes */
    private HashMap reqHeaders = new HashMap();

    /**
     * Create a connection to a URL with a username, password, and Map of query parameters.
     *
     * @param uriSpec  URL for request
     * @param query    Map of name and values for the request query string parameters
     */
    public HttpClientChannel(String uriSpec, Map query) throws CoreException {
        init(uriSpec, query);
    }

    /**
     * Create a connection to a URL with a username, password, and Map of query parameters.
     *
     * @param uriSpec  URL for request
     * @param query    Map of name and values for the request query string parameters
     */
    public HttpClientChannel(String uriSpec, Map query, OutputStream destinationStream) throws CoreException {
        this(uriSpec, query);
        this.destinationStream = destinationStream;
    }

    /**
     * Create a connection to a URL with a username, password, and Map of query parameters.
     *
     * @param uriSpec  URL for request
     * @param query    Map of name and values for the request query string parameters
     */
    public HttpClientChannel(String uriSpec, Map query, OutputStream destinationStream, String expectedContentType) throws
        CoreException {
        this(uriSpec, query, destinationStream);
        this.expectedContentType = expectedContentType;
    }
    /**
     * Process the URL based on the query params, and create the HttpURLConnection with appropriate headers for username
     * and password.
     */
    protected void init(String uriSpec, Map query) throws CoreException {

        requestUrl = uriSpec;
        if (query != null && query.size() > 0) {
            requestUrl = constructURLQuery(uriSpec, query);
        }

        URI uri;
        try {
            uri = new URI(requestUrl);
        } catch (URISyntaxException e) {
            throw new CoreException(e.getMessage());
        }

        requestURL = null;
        try {
            requestURL = uri.toURL();
        } catch (MalformedURLException e) {
            throw new CoreException(e.getMessage());
        }
        logger.debug("creating connection object to URL: " + requestUrl);

        httpc = new HttpClient();
        if (null != System.getProperty("http.proxyPort") && null != System.getProperty("http.proxyHost")) {
            Integer port = Integer.getInteger("http.proxyPort");
            if (null != port) {
                httpc.getHostConfiguration().setProxy(System.getProperty("http.proxyHost"), port);
            }
        }
    }

    private static final HashSet validMethodTypes= new HashSet();
    static {
        validMethodTypes.add("GET");
        validMethodTypes.add("POST");
        validMethodTypes.add("PUT");
        validMethodTypes.add("DELETE");
    }
    public void setMethodType(String type) {
        if (!validMethodTypes.contains(type.toUpperCase())) {
            throw new IllegalArgumentException("Unknown method type: " + type);
        }
        this.methodType = type;
    }
    /**
     * Create new HttpMethod objects for the requestUrl, and set any request headers specified previously
     */
    private HttpMethod initMethod() {
        if ("GET".equalsIgnoreCase(getMethodType())) {
            httpMethod = new GetMethod(requestUrl);
        } else if ("POST".equalsIgnoreCase(getMethodType())) {
            httpMethod = new PostMethod(requestUrl);
        } else if ("PUT".equalsIgnoreCase(getMethodType())) {
            httpMethod = new PutMethod(requestUrl);
        } else if ("DELETE".equalsIgnoreCase(getMethodType())) {
            httpMethod = new DeleteMethod(requestUrl);
        } else {
            throw new IllegalArgumentException("Unknown method type: " + getMethodType());
        }
        if (reqHeaders.size() > 0) {
            for (Iterator i = reqHeaders.keySet().iterator(); i.hasNext();) {
                String s = (String) i.next();
                String v = (String) reqHeaders.get(s);
                httpMethod.setRequestHeader(s, v);
            }
        }
        return httpMethod;
    }

    /**
     * Create new URL with query parameters
     */
    static String constructURLQuery(String urlbase, Map query) {
        StringBuffer sb = new StringBuffer(urlbase);
        if(null!=query){
            sb.append("?");
            boolean seen=false;
            for (final Object o : query.entrySet()) {
                if (seen) {
                    sb.append("&");
                }
                seen=true;
                Map.Entry entry = (Map.Entry) o;
                String key = (String) entry.getKey();
                Object val = entry.getValue();
                if (null == val) {
                    val = "";
                }
                try {
                    sb.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(val.toString(), "UTF-8"));
                } catch (java.io.UnsupportedEncodingException exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Set a Header field for the request.  Must be made before makeConnection is called.
     */
    public void setRequestHeader(String name, String value) {
        reqHeaders.put(name, value);
    }

    /**
     * applies request properties from map onto the url connection object
     */
    private void setHeaders(Map map) {
        for (final Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            setRequestHeader(key, value);
        }
    }


    /**
     * subclasses should return a byte[] of data, or null.  This will only be called if the {@link #isPostMethod()} method
     * returns true.
     */
    protected abstract RequestEntity getRequestEntity(PostMethod method);

    /**
     * subclasses should return a byte[] of data, or null.  This will only be called if the {@link #isPostMethod()} method
     * returns true.
     */
    protected abstract NameValuePair[] getRequestBody(PostMethod method);

    /**
     * @return true if the request method is POST.
     */
    protected abstract boolean isPostMethod();

    /**
     * this method is called at the end of makeRequest, allowing subclasses to handle the result before makeRequest
     * returns.
     */
    protected abstract void postMakeRequest();

    /**
     * Called before making the request.  Subclasses may override to perform other logic. Return true to continue making
     * the request, or false to finish without making the request.
     */
    protected boolean preMakeRequest(HttpMethod method) throws HttpClientException {
        return true;
    }

    /**
     * Called before making the request.  Subclasses may override to perform other logic. Return true to continue making
     * the request, or false to finish without making the request.  May be called again if {@link #needsReAuthentication(int, org.apache.commons.httpclient.HttpMethod)} returns true.
     */
    protected boolean doAuthentication(HttpMethod method) throws HttpClientException {
        return true;
    }

    /**
     * @return true if initial result of request indicates that authentication needs to be performed again.
     * @param resultCode status code
     * @param method method
     */
    protected boolean needsReAuthentication(int resultCode, HttpMethod method){
        return false;
    }

    /**
     * @return Gets the HttpClient used in the request making.
     */
    HttpClient getHttpClient() {
        return httpc;
    }

    public HttpMethod getRequestMethod(){
        return reqMadeMethod;
    }
    /**
     * @return Gets headers from the response.
     *
     * @param name header name
     */
    public Header getResponseHeader(String name) {
        return reqMadeMethod.getResponseHeader(name);
    }

    /**
     * Return the raw bytes from the response.
     * @return byte[] of the result data, or null if nothing returned or request not yet made
     * @throws IOException
     */
    public byte[] getResponseBody() throws IOException {
        if(reqMadeMethod!=null)
            return reqMadeMethod.getResponseBody();
        else
            return null;
    }

    /**
     * Perform the HTTP request.  Can only be performed once.
     */
    public void makeRequest() throws IOException, HttpClientException {
        if (requestMade) {
            return;
        }


        requestMade = true;
        RequestEntity reqEntity=null;
        NameValuePair[] postBody=null;
        if (isPostMethod()) {
            setMethodType("POST");
        }
        HttpMethod method = initMethod();
        if(isPostMethod()){
            reqEntity = getRequestEntity((PostMethod)method);
            if(null!=reqEntity){
                logger.debug("preparing to post request entity data: " + reqEntity.getContentType());

                ((PostMethod) method).setRequestEntity(reqEntity);
            }else{
                logger.debug("preparing to post form data" );
                postBody = getRequestBody((PostMethod)method);
                ((PostMethod) method).setRequestBody(postBody);
            }
        }
        logger.debug("calling preMakeRequest");
        if (!preMakeRequest(method)) {
            return;
        }
        logger.debug("calling doAuthentication...");
        if(!doAuthentication(method)){
            return;
        }
        int bytesread=0;
        try {
            if(!isPostMethod()){
                method.setFollowRedirects(true);
            }
            logger.debug("make request...");
            resultCode = httpc.executeMethod(method);
            reasonCode = method.getStatusText();
            if(isPostMethod()){
                //check redirect after post
                method = checkFollowRedirect(method,resultCode);
            }
            logger.debug("check needs reauth...");

            if(needsReAuthentication(resultCode,method)){
                logger.debug("re-authentication needed, performing...");
                method.releaseConnection();
                method.abort();
                //need to re-authenticate.
                method = initMethod();
                if (isPostMethod() && null!=reqEntity) {
                    ((PostMethod) method).setRequestEntity(reqEntity);
                }else if (isPostMethod() && null!=postBody) {
                    ((PostMethod) method).setRequestBody(postBody);
                }
                if(!doAuthentication(method)){
                    //user login failed
                    return;
                }
                //user login has succeeded
                logger.debug("remaking original request...");
                resultCode = httpc.executeMethod(method);
                reasonCode = method.getStatusText();
                if(needsReAuthentication(resultCode, method)) {
                    //user request was unauthorized
                    throw new HttpClientException("Unauthorized Action: " + (null != method.getResponseHeader(
                        Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER) ? method.getResponseHeader(
                        Constants.X_RUNDECK_ACTION_UNAUTHORIZED_HEADER).getValue()
                                                : reasonCode));
                }
            }

            logger.debug("finish...");
            if (null != method.getResponseHeader("Content-Type")) {
                resultType = method.getResponseHeader("Content-Type").getValue();
            }
            String type = resultType;
            if (type != null && type.indexOf(";") > 0) {
                type = type.substring(0, type.indexOf(";")).trim();
            }
            if (null==expectedContentType || expectedContentType.equals(type)) {
                if (null != destinationStream && resultCode >= 200 && resultCode < 300) {
                    //read the input stream and write it to the destination
                    contentLengthRetrieved=Streams.copyStreamCount(method.getResponseBodyAsStream(), destinationStream);
                } else {
                    final ByteArrayOutputStream outputBytes = new ByteArrayOutputStream(1024*50);
                    Streams.copyStream(method.getResponseBodyAsStream(),outputBytes);
                    resultStream = new ByteArrayInputStream(outputBytes.toByteArray());
                }
            }
            reqMadeMethod = method;
        } catch (HttpException e) {
            logger.error("HTTP error: "+e.getMessage(), e);
        }finally{
            method.releaseConnection();
        }

        logger.debug("Response received");
        postMakeRequest();
    }

    private HttpMethod checkFollowRedirect(final HttpMethod method, final int res) throws IOException, HttpClientException {

        if ((res == HttpStatus.SC_MOVED_TEMPORARILY) ||
            (res == HttpStatus.SC_MOVED_PERMANENTLY) ||
            (res == HttpStatus.SC_SEE_OTHER) ||
            (res == HttpStatus.SC_TEMPORARY_REDIRECT)) {
            final Header locHeader = method.getResponseHeader("Location");
            if (locHeader == null) {
                throw new HttpClientException("Redirect with no Location header, request URL: " + method.getURI());
            }
            final String location = locHeader.getValue();
            logger.debug("Follow redirect: " + res + ": " + location);

            method.releaseConnection();
            final GetMethod followMethod = new GetMethod(location);
            followMethod.setFollowRedirects(true);
            resultCode = httpc.executeMethod(followMethod);
            reasonCode = followMethod.getStatusText();
            logger.debug("Result: " + resultCode);
            return followMethod;
        }
        return method;
    }

    /**
     * return content type
     */
    public String getResultContentType() {
        return this.resultType;
    }

    /**
     * @return Get the HTTP response code.
     */
    int getResultCode() {
        return resultCode;
    }

    /**
     * @return Get the URL used for the request.
     */
    URL getRequestURL() {
        return requestURL;
    }

    public int getContentLengthRetrieved() {
        return contentLengthRetrieved;
    }

    public String getExpectedContentType() {
        return expectedContentType;
    }

    public String getMethodType() {
        return methodType;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public InputStream getResultStream() {
        return resultStream;
    }
}
