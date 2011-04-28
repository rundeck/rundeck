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
* WebserviceFormAuthenticator.java
* 
* User: greg
* Created: Jul 21, 2008 5:45:11 PM
* $Id$
*/
package com.dtolabs.client.utils;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;


/**
 * WebserviceFormAuthenticator extends BaseFormAuthenticator to provide authentication to the Webservice application.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WebserviceFormAuthenticator extends BaseFormAuthenticator {
    /**
     * Default base path for the Webservice application: {@value}
     */
    public static final String DEFAULT_APP_BASE = "/rundeck";
    private static final String REGEX_BASE = "^https?://.*?";
    private static final String LOGIN_REGEX = "/user/login.*$";
    private static final String LOGIN_ERROR_REGEX = "/user/error$";

    /**
     * Constructor using the default base path: {@link #DEFAULT_APP_BASE}
     *
     * @param username username
     * @param password password
     */
    public WebserviceFormAuthenticator(final String username, final String password) {
        this(DEFAULT_APP_BASE, username, password);
    }

    /**
     * Constructor
     *
     * @param basePath base path to use
     * @param username username
     * @param password password
     */
    public WebserviceFormAuthenticator(final String basePath, final String username, final String password) {
        super(basePath, username, password);
    }

    String getInitialPath() {
        return "/menu/index";
    }

    boolean isValidLoginRedirect(final HttpMethod method) {
        final Header locHeader = method.getResponseHeader("Location");
        if (locHeader == null) {
            return false;
        }
        final String location = locHeader.getValue();
        return location.matches(REGEX_BASE + getBasePath() + LOGIN_REGEX);
    }

    boolean isLoginError(final HttpMethod method) {
        final Header locHeader = method.getResponseHeader("Location");
        if (locHeader == null) {
            return true;
        }
        final String location = locHeader.getValue();

        return location.matches(REGEX_BASE + getBasePath() + LOGIN_ERROR_REGEX);
    }

    boolean isFollowLoginRedirect() {
        return true;
    }
}
