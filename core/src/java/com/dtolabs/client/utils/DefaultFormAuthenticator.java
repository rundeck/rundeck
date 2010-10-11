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
 * $Id: DefaultFormAuthenticator.java 9813 2010-02-24 18:20:16Z gschueler $
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
 * DefaultAuthenticator authenticates to workbench by posting login information to the j_security_check authorization
 * mechanism.  If a 401 UNAUTHORIZED response is detected, then authentication is passed on to the {@link BasicAuthenticator}.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 9813 $
 */
public class DefaultFormAuthenticator implements HttpAuthenticator {
    public static org.apache.log4j.Logger logger = Logger.getLogger(DefaultFormAuthenticator.class);

    public static final String ITNAV_CTX_PATH = "/itnav";
    public static final String ITNAV_WELCOME_PATH = "/do/menu/Welcome";
    public static final String ITNAV_LOGIN_REGEX = "^https?://.*?/itnav/Logon\\.do.*$";
    public static final String ITNAV_LOGIN_ERROR_REGEX = "^https?://.*?/itnav/LogonError\\.do.*$";
    public static final String ITNAV_AUTH_PATH = "/j_security_check";
    public static final String ITNAV_AUTH_USER_PARAM = "j_username";
    public static final String ITNAV_AUTH_PASS_PARAM = "j_password";
    public static final String ITNAV_SESSION_COOKIE_NAME = "JSESSIONID";
    public static final Pattern ITNAV_SESSION_COOKIE_PATTERN = Pattern.compile("^JSESSIONID(_CT_.+)?$");
    public static final String COLONY_REQ_CONTENT_TYPE = "text/xml";
    public static final String HTTP_SECURE_PROTOCOL = "https";

    private String username;
    private String password;

    public DefaultFormAuthenticator(String username, String password) {
        this.username=username;
        this.password = password;
    }

    /**
     * Authenticate the client http state so that the colony requests can be made.
     * @param baseURL URL requested for colony
     * @param client HttpClient instance
     * @return true if authentication succeeded.
     * @throws HttpClientException
     */
    public boolean authenticate(URL baseURL,HttpClient client) throws HttpClientException {
        HttpState state = client.getState();
        if (hasSessionCookie(baseURL, state)){
            return true;
        }
        byte[] buffer = new byte[1024];

        logger.debug("No session found, must login...");
        try {
            URL newUrl = new URL(baseURL.getProtocol(),
                                 baseURL.getHost(),
                                 baseURL.getPort(),
                                 ITNAV_CTX_PATH + ITNAV_WELCOME_PATH);

            //load welcome page, which should forward to form based logon page.
            GetMethod get = new GetMethod(newUrl.toExternalForm());
            get.setDoAuthentication(false);
            get.setFollowRedirects(false);
            logger.debug("Requesting: " + newUrl);
            int res = client.executeMethod(get);
            logger.debug("Result is: " + res);
            if(get.getResponseContentLength()>0)
                get.getResponseBodyAsString();
            get.releaseConnection();

            if((res == HttpStatus.SC_UNAUTHORIZED)){
                if(get.getResponseHeader("WWW-Authenticate")!=null && get.getResponseHeader("WWW-Authenticate").getValue().matches("^Basic.*")){
                    logger.warn("Form-based login received UNAUTHORIZED, trying to use Basic authentication");
                    BasicAuthenticator auth = new BasicAuthenticator(username, password);
                    return auth.authenticate(baseURL, client);
                }else{
                    throw new HttpClientException(
                        "Form-based login received UNAUTHORIZED, but didn't recognize it as Basic authentication: unable to get a session");
                }

            }
            //should now have the proper session cookie
            if (!hasSessionCookie(baseURL, state)) {
                throw new HttpClientException("Unable to get a session from URL : " + newUrl);
            }
            if ((res == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (res == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (res == HttpStatus.SC_SEE_OTHER) ||
                (res == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                Header locHeader = get.getResponseHeader("Location");
                if (locHeader == null) {
                    throw new HttpClientException("Redirect with no Location header, request URL: " + newUrl);
                }
                String location = locHeader.getValue();
                if (!location.matches(ITNAV_LOGIN_REGEX)) {
                    //unexpected response 
                    throw new HttpClientException("Unexpected redirection when getting session: " + location);
                }
                logger.debug("Follow redirect: " + res + ": " + location);

                GetMethod redir = new GetMethod(location);
                redir.setFollowRedirects(true);
                res = client.executeMethod(redir);
                InputStream ins = redir.getResponseBodyAsStream();
                while (ins.available() > 0) {
                    ins.read(buffer);
                }
                redir.releaseConnection();


                if (res != HttpStatus.SC_OK) {
                    throw new HttpClientException("Login page status was not OK: " + res);
                }
                logger.debug("Result: " + res);

                //now post login
                URL loginUrl = new URL(baseURL.getProtocol(),
                                       baseURL.getHost(),
                                       baseURL.getPort(),
                                       ITNAV_CTX_PATH + ITNAV_AUTH_PATH);

                PostMethod login = new PostMethod(loginUrl.toExternalForm());
                login.setRequestBody(new NameValuePair[]{
                    new NameValuePair(ITNAV_AUTH_USER_PARAM, getUsername()),
                    new NameValuePair(ITNAV_AUTH_PASS_PARAM, getPassword())
                });

                login.setFollowRedirects(false);
                logger.debug("Post login info to URL: " + loginUrl);

                res = client.executeMethod(login);

                ins = login.getResponseBodyAsStream();
                while(ins.available()> 0){
                    ins.read(buffer);
                }
                login.releaseConnection();

                if((res == HttpStatus.SC_MOVED_TEMPORARILY) ||
                   (res == HttpStatus.SC_MOVED_PERMANENTLY) ||
                   (res == HttpStatus.SC_SEE_OTHER) ||
                   (res == HttpStatus.SC_TEMPORARY_REDIRECT)){

                     locHeader = login.getResponseHeader("Location");
                    if (locHeader == null) {
                        throw new HttpClientException("Redirect with no Location header, request URL: " + newUrl);
                    }
                    location = locHeader.getValue();

                    if(location.matches(ITNAV_LOGIN_ERROR_REGEX)){
                        logger.error("Form-based auth failed");
                        return false;
                    }else if(!location.equals(newUrl.toExternalForm())){

                        logger.warn("Form-based auth succeeded, but last URL was unexpected");
                    }
                    GetMethod get2 = new GetMethod(location);
//
//                    logger.debug("Result: " + res + ": " + location + ", following redirect");
                    res = client.executeMethod(get2);
                }else if (res != HttpStatus.SC_OK) {
                    throw new HttpClientException("Login didn't seem to work: " + res + ": "
                                                  + login.getResponseBodyAsString());
                }
                logger.debug("Result: " + res);
            } else if (res != HttpStatus.SC_OK) {
                //if request to welcome page was OK, we figure that the session is already set

                throw new HttpClientException("Request to welcome page returned error: " + res + ": " + get);
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
     * @param reqUrl
     * @param state
     *
     * @return
     */
    public static boolean hasSessionCookie(URL reqUrl, HttpState state) {

        CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
        Cookie[] initcookies = cookiespec.match(reqUrl.getHost(),
                                                reqUrl.getPort() > 0 ? reqUrl.getPort() : 80,
                                                ITNAV_CTX_PATH,
                                                HTTP_SECURE_PROTOCOL.equalsIgnoreCase(reqUrl.getProtocol()),
                                                state.getCookies());
        boolean hasSession = false;
        if (initcookies.length == 0) {
            hasSession = false;
        } else {
            for (int i = 0; i < initcookies.length; i++) {
                if (ITNAV_SESSION_COOKIE_NAME.equals(initcookies[i].getName())
                    || ITNAV_SESSION_COOKIE_PATTERN.matcher(initcookies[i].getName()).matches()) {
                    logger.debug("Saw session cookie: " + initcookies[i].getName());
                    hasSession = true;
                    break;
                }
            }
        }
        return hasSession;
    }

    public String getUsername() {
        return username;
    }

    public boolean needsReAuthentication(int resultCode, HttpMethod method) {
        if (resultCode >= 300 && resultCode < 400 && method.getResponseHeader("Location") != null) {
            String loc = method.getResponseHeader("Location").getValue();
            int logNdx = loc.indexOf(ITNAV_CTX_PATH + "/Logon.do");
            int qNdx = loc.indexOf("?");
            if (logNdx >= 0 && (qNdx < 0 || logNdx < qNdx)) {
                //if "/itnav/Logon.do" is in redir, and "/itnav/Logon.do" is not after query part of URL
                //reset session cookie then return true
                HttpState state = ClientState.resetHttpState();
                return true;
            }
        }
        return false;
    }

    private String getPassword() {
        return password;
    }
}
