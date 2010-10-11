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


/**
 * Instances of classes that implement this class authenticate a user according to their implementation
 * strategies. The methods of this class return {@link com.dtolabs.rundeck.core.authentication.IUserInfo} objects that provide access to username
 * and password.
 * <p/>
 */

public interface Authenticator {
    /**
     * Get the current UserInfo without prompting.  Both values of the IUserInfo may be null.
     *
     * @return
     */
    IUserInfo getUserInfoWithoutPrompt();

    /**
     * Clear and get the UserInfo again, causing a prompt.
     *
     * @return
     * @throws com.dtolabs.rundeck.core.authentication.UserInfoException
     * @throws com.dtolabs.rundeck.core.authentication.PromptCancelledException
     */
    IUserInfo getNewUserInfo() throws UserInfoException, PromptCancelledException;

    /**
     * Prompt for user info and, if not cancelled, set the result as the new user info.
     *
     * @return
     * @throws UserInfoException
     * @throws PromptCancelledException
     */
    IUserInfo getPromptUserInfo() throws UserInfoException, PromptCancelledException;

    /**
     * Get the UserInfo.  If needed, the user will be prompted for it.
     *
     * @return
     * @throws UserInfoException
     * @throws PromptCancelledException
     */
    IUserInfo getUserInfo() throws UserInfoException, PromptCancelledException;
}
