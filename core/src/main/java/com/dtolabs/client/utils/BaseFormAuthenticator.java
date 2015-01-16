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
 * DefaultAuthenticator.java
 * 
 * User: greg
 * Created: Jan 14, 2005 4:57:46 PM
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;


/**
 * BaseFormAuthenticator provides base implementation of authenticator by posting login information to the
 * j_security_check authorization mechanism.  If a 401 UNAUTHORIZED response is detected, then authentication is passed
 * on to the {@link com.dtolabs.client.utils.BasicAuthenticator}. This class is abstract and all abstract methods
 * should be implemented by subclasses: <ul> <li>{@link #getInitialPath()} - returns value of app context or base
 * path</li> <li>{@link #isValidLoginRedirect(org.apache.commons.httpclient.HttpMethod)} - return true if the HttpMethod
 * response is a valid redirect after login form is submitted</li> <li>{@link #isLoginError(org.apache.commons.httpclient.HttpMethod)}
 * - return true if the HttpMethod response indicates an error occurred</li> <li>{@link #isFollowLoginRedirect()} -
 * return true if authenticator should follow any redirect returned after login form is submitted</li>
 *
 * </ul>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 7839 $
 */
public abstract class BaseFormAuthenticator implements HttpAuthenticator {
    /**
     * logger
     */
    public static final Logger logger = org.apache.log4j.Logger.getLogger(BaseFormAuthenticator.class);

    public static final String J_SECURITY_CHECK = "j_security_check";
    /**
     * path for java auth form submit: {@value}
     */
    public static final String JAVA_AUTH_PATH = "/"+J_SECURITY_CHECK;
    /**
     * username param for java auth form submit: {@value }
     */
    public static final String JAVA_USER_PARAM = "j_username";
    /**
     * password param for java auth form submit: {@value}
     */
    public static final String JAVA_PASS_PARAM = "j_password";
    /**
     * Cookie name for java auth session: {@value}
     */
    public static final String JAVA_SESSION_COOKIE_NAME = "JSESSIONID";
    public static final Pattern JAVA_SESSION_COOKIE_PATTERN = Pattern.compile("^JSESSIONID$");
    /**
     * Secure protocol: {@value}
     */
    public static final String HTTP_SECURE_PROTOCOL = "https";

    /**
     * Text to check Location of redirects to indicate login is required.
     */
    public static final String LOGIN_PAGE = "/user/login";
    private String username;
    private String password;
    private String basePath;
    private String cookieId;

    /**
     * Constructor with base URL path, username and password for authentication.
     *
     * @param basePath base URL path to use with the server
     * @param username username to use
     * @param password password to use
     */
    public BaseFormAuthenticator(final String basePath, final String username, final String password) {
        this.basePath = basePath;
        this.username = username;
        this.password = password;
    }

    /**
     * Return initial request path prior to posting login information
     *
     * @return absolute path
     */
    abstract String getInitialPath();

    /**
     * Return true if the specified redirect location is expected after requesting the initial path
     *
     * @param method method containing response information
     *
     * @return true if location is valid
     */
    abstract boolean isValidLoginRedirect(HttpMethod method);

    /**
     * Return true if the response indicates a login error
     *
     * @param method the post request and response Method
     *
     * @return true if login FAILED
     */
    abstract boolean isLoginError(HttpMethod method);

    /**
     * Return true if redirect after login form is posted should be followed.
     *
     * @return true to follow redirect after login
     */
    abstract boolean isFollowLoginRedirect();

    /**
     * Authenticate the client http state so that the colony requests can be made.
     *
     * @param baseURL URL requested for colony
     * @param client  HttpClient instance
     *
     * @return true if authentication succeeded.
     *
     * @throws com.dtolabs.client.utils.HttpClientException on error
     *
     */
    public boolean authenticate(final URL baseURL, final HttpClient client) throws HttpClientException {
        final HttpState state = client.getState();
        if (hasSessionCookie(baseURL, state, basePath)) {
            return true;
        }
        final byte[] buffer = new byte[1024];

        boolean doPostLogin=false;
        boolean isLoginFormContent=false;
        logger.debug("No session found, must login...");
        try {
            final URL newUrl = new URL(baseURL.getProtocol(),
                                       baseURL.getHost(),
                                       baseURL.getPort(),
                                       basePath + getInitialPath());

            //load welcome page, which should forward to form based logon page.
            final GetMethod get = new GetMethod(newUrl.toExternalForm());
            get.setDoAuthentication(false);
            get.setFollowRedirects(false);
            logger.debug("Requesting: " + newUrl);
            int res = client.executeMethod(get);
            logger.debug("Result is: " + res);

            /*
              Tomcat container auth behaves differently than Jetty.  Tomcat will respond 200 OK and include the login form
              when auth is required, as well as on auth failure, it will also require complete GET of original URL after
              successful auth.
              Jetty will redirect to login page when auth is required, and will redirect to error page on failure.
             */

            String body=get.getResponseBodyAsString();
            if (null != body && body.contains(J_SECURITY_CHECK) && body.contains(JAVA_USER_PARAM) && body.contains(
                JAVA_PASS_PARAM)) {
                isLoginFormContent = true;
            }
            get.releaseConnection();

            if ((res == HttpStatus.SC_UNAUTHORIZED)) {
                if (get.getResponseHeader("WWW-Authenticate") != null && get.getResponseHeader("WWW-Authenticate")
                    .getValue()
                    .matches("^Basic.*")) {
                    logger.warn("Form-based login received UNAUTHORIZED, trying to use Basic authentication");
                    final BasicAuthenticator auth = new BasicAuthenticator(username, password);
                    return auth.authenticate(baseURL, client);
                } else {
                    throw new HttpClientException(
                        "Form-based login received UNAUTHORIZED, but didn't recognize it as Basic authentication: unable to get a session");
                }

            }
            //should now have the proper session cookie
            if (!hasSessionCookie(baseURL, state, basePath)) {
                throw new HttpClientException("Unable to get a session from URL : " + newUrl);
            }
            if (res == HttpStatus.SC_OK && isLoginFormContent) {
                doPostLogin = true;
            }else if ((res == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (res == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (res == HttpStatus.SC_SEE_OTHER) ||
                (res == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                Header locHeader = get.getResponseHeader("Location");
                if (locHeader == null) {
                    throw new HttpClientException("Redirect with no Location header, request URL: " + newUrl);
                }
                String location = locHeader.getValue();
                if (!isValidLoginRedirect(get)) {
                    //unexpected response
                    throw new HttpClientException("Unexpected redirection when getting session: " + location);
                }
                logger.debug("Follow redirect: " + res + ": " + location);

                final GetMethod redir = new GetMethod(location);
                redir.setFollowRedirects(true);
                res = client.executeMethod(redir);
                InputStream ins = redir.getResponseBodyAsStream();
                while (ins.available() > 0) {
                    //read and discard response body
                    ins.read(buffer);
                }
                redir.releaseConnection();


                if (res != HttpStatus.SC_OK) {
                    throw new HttpClientException("Login page status was not OK: " + res);
                }
                logger.debug("Result: " + res);

                doPostLogin=true;
            }else if (res != HttpStatus.SC_OK) {
                //if request to welcome page was OK, we figure that the session is already set
                throw new HttpClientException("Request to welcome page returned error: " + res + ": " + get);
            }
            if(doPostLogin){
                //now post login
                final URL loginUrl = new URL(baseURL.getProtocol(),
                                             baseURL.getHost(),
                                             baseURL.getPort(),
                                             basePath + JAVA_AUTH_PATH);

                final PostMethod login = new PostMethod(loginUrl.toExternalForm());
                login.setRequestBody(new NameValuePair[]{
                    new NameValuePair(JAVA_USER_PARAM, getUsername()),
                    new NameValuePair(JAVA_PASS_PARAM, getPassword())
                });

                login.setFollowRedirects(false);
                logger.debug("Post login info to URL: " + loginUrl);

                res = client.executeMethod(login);

                final InputStream ins = login.getResponseBodyAsStream();
                while (ins.available() > 0) {
                    //read and discard response body
                    ins.read(buffer);
                }
                login.releaseConnection();

                Header locHeader = login.getResponseHeader("Location");
                String location = null != locHeader ? locHeader.getValue() : null;
                if (isLoginError(login)) {
                    logger.error("Form-based auth failed");
                    return false;
                } else if (null!=location && !location.equals(newUrl.toExternalForm())) {

                    logger.warn("Form-based auth succeeded, but last URL was unexpected");
                }
                if (isFollowLoginRedirect()
                    && ((res == HttpStatus.SC_MOVED_TEMPORARILY) ||
                        (res == HttpStatus.SC_MOVED_PERMANENTLY) ||
                        (res == HttpStatus.SC_SEE_OTHER) ||
                        (res == HttpStatus.SC_TEMPORARY_REDIRECT))) {

                    if (location == null) {
                        throw new HttpClientException("Redirect with no Location header, request URL: " + newUrl);
                    }
                    final GetMethod get2 = new GetMethod(location);
                    //                    logger.debug("Result: " + res + ": " + location + ", following redirect");
                    res = client.executeMethod(get2);
                } else if (res != HttpStatus.SC_OK) {
                    throw new HttpClientException("Login didn't seem to work: " + res + ": "
                                                  + login.getResponseBodyAsString());
                }
                logger.debug("Result: " + res);
            }
        } catch (MalformedURLException e) {
            throw new HttpClientException("Bad URL", e);
        } catch (HttpException e) {
            throw new HttpClientException("HTTP Error: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new HttpClientException("Error occurred while trying to authenticate to server: "+e.getMessage(), e);
        }

        return true;
    }

    /**
     * Check the http state cookies to see if we have the proper session id cookie
     *
     * @param reqUrl   URL being requested
     * @param state    HTTP state object
     * @param basePath base path of requests
     *
     * @return true if a cookie matching the basePath, server host and port, and request protocol and appropriate
     *         session cookie name is found
     */
    public static boolean hasSessionCookie(final URL reqUrl, final HttpState state, final String basePath) {

        final CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
        final Cookie[] initcookies = cookiespec.match(reqUrl.getHost(),
                                                      reqUrl.getPort() > 0 ? reqUrl.getPort() : 80,
                                                      basePath.endsWith("/") ? basePath : basePath + "/",
                                                      HTTP_SECURE_PROTOCOL.equalsIgnoreCase(reqUrl.getProtocol()),
                                                      state.getCookies());
        boolean hasSession = false;
        if (initcookies.length == 0) {
            hasSession = false;
        } else {
            for (final Cookie cookie : initcookies) {
                if (JAVA_SESSION_COOKIE_NAME.equals(cookie.getName())
                    || JAVA_SESSION_COOKIE_PATTERN.matcher(cookie.getName()).matches()) {
                    logger.debug("Saw session cookie: " + cookie.getName());
                    hasSession = true;
                    break;
                }
            }
        }
        return hasSession;
    }

    /**
     * Return true if the result from the get method indicates re-authentication is needed
     *
     * @param resultCode result code
     * @param method     request
     *
     * @return true if re-authentication is needed
     */
    public boolean needsReAuthentication(final int resultCode, final HttpMethod method) {
        if (resultCode >= 300 && resultCode < 400 && method.getResponseHeader("Location") != null) {
            final String loc = method.getResponseHeader("Location").getValue();
            final int logNdx = loc.indexOf(LOGIN_PAGE);
            final int qNdx = loc.indexOf("?");
            if (logNdx >= 0 && (qNdx < 0 || logNdx < qNdx)) {
                //if "user/login" is in the location and is not after query part of URL
                //reset session cookie then return true
                ClientState.resetHttpState();
                return true;
            }
        }else if(HttpStatus.SC_OK==resultCode){
            final String loc = method.getPath();
            final int logNdx = loc.indexOf(LOGIN_PAGE);
            final int qNdx = loc.indexOf("?");
            if (logNdx >= 0 && (qNdx < 0 || logNdx < qNdx)) {
                //if "user/login" is in the location and is not after query part of URL
                //reset session cookie then return true
                ClientState.resetHttpState();
                return true;
            }
        }
        return false;
    }

    public String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getCookieId() {
        return cookieId;
    }

    public void setCookieId(String cookieId) {
        this.cookieId = cookieId;
    }
}