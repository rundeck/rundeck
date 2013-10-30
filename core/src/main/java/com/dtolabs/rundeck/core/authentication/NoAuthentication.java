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

package com.dtolabs.rundeck.core.authentication;

import com.dtolabs.rundeck.core.common.Framework;

import javax.security.auth.Subject;
import java.io.File;

/**
 * Trivial implementation of the {@link Authenticator} interface.
 */
public class NoAuthentication implements Authenticator {

    public NoAuthentication(final Framework framework) {
    }
    public NoAuthentication(final Framework framework,File basedir) {
    }
    /**
     * Returns an instnace of NoAuthentication
     *
     * @return
     */
    public static Authenticator getAuthenticator(final Framework framework) {
        return new NoAuthentication(framework);
    }

    /**
     * wrapper call to {@link this#getUserInfo()}
     *
     * @return
     */
    public IUserInfo getUserInfoWithoutPrompt() {
        return getUserInfo();
    }


    /**
     * Returns an instance of {@link IUserInfo} setting the user name to the value of
     * "user.name" system property and empty password.
     *
     * @return
     * @throws UserInfoException
     * @throws PromptCancelledException
     */
    public IUserInfo getUserInfo() throws UserInfoException  {
        final String username = System.getProperty("user.name");
        return new IUserInfo() {
            public String getUsername() {
                return username;
            }
        };
    }

    public Subject getSubject() throws UserInfoException {
        Subject subject = new Subject();
        subject.getPrincipals().add(new Username(System.getProperty("user.name")));
        return subject;
    }
}
