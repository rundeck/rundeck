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
 * BasicAuthenticator.java
 * 
 * User: greg
 * Created: Jan 14, 2005 6:33:22 PM
 * $Id: BasicAuthenticator.java 8473 2008-07-23 17:40:15Z gschueler $
 */
package com.dtolabs.client.utils;


import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import java.net.URL;


/**
 * BasicAuthenticator uses HTTP Basic authentication to log in to the server.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 8473 $
 */
class BasicAuthenticator implements HttpAuthenticator {

    private String username;
    private String password;
    private static final String REALM_NAME = "Server";

    BasicAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean authenticate(URL reqUrl, HttpClient client) throws HttpClientException {
        AuthScope scope = new AuthScope(reqUrl.getHost(), reqUrl.getPort(), REALM_NAME);
        Credentials creds = new UsernamePasswordCredentials(getUsername(), getPassword());
        client.getParams().setAuthenticationPreemptive(true);
        if (client.getState().getCredentials(scope) == null) {
            client.getState().setCredentials(scope, creds);
        }
        return true;
    }

    public String getUsername() {
        return username;
    }

    public boolean needsReAuthentication(int resultCode, HttpMethod method) {
        if (resultCode >= 300 && resultCode < 400) {
            ClientState.resetHttpState();
            return true;
        }
        return false;
    }

    private String getPassword() {
        return password;
    }
}
