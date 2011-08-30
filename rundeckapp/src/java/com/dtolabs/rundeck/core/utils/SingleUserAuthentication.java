/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* SingleUserAuthentication.java
* 
* User: greg
* Created: Feb 26, 2009 10:18:48 AM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.authentication.IUserInfo;
import com.dtolabs.rundeck.core.authentication.UserInfoException;
import com.dtolabs.rundeck.core.authentication.Authenticator;

import javax.security.auth.Subject;


/**
 * SingleUserAuthentication implements {@link Authenticator} and merely supplies a single username/password.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class SingleUserAuthentication implements Authenticator {
    private final String username;
    private final Subject subject;


    public SingleUserAuthentication(final String username, final Subject subject) {
        this.username = username;
        this.subject = subject;
    }

    public final IUserInfo getUserInfo() throws UserInfoException {
        return new IUserInfo(){
            public String getUsername() {
                return username;
            }

        };
    }

    public Subject getSubject() throws UserInfoException {
        return subject;
    }
}
