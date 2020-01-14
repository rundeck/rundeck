/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.authentication.tokens;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

import java.util.List;
import java.util.Set;

/**
 * Manage Rundeck API authentication tokens
 */
public interface AuthTokenManager {
    /**
     * Retrieve token information from storage
     * @param token The token string that identifies the token
     * @return AuthenticationToken
     */
    AuthenticationToken getToken(String token);

    /**
     * Update the roles associated with the authentication token
     * @param token token id
     * @param roleSet the new list of roles
     * @return true if the update completed successfully
     */
    boolean updateAuthRoles(UserAndRolesAuthContext authContext, String token, Set<String> roleSet) throws Exception;

    /**
     * Delete an authentication token
     * @param token token identifier
     * @return true if the update completed successfully
     */
    boolean deleteToken(String token);

    /**
     * Parse a comma separated list of roles into a role Set
     * @param authRoles String of roles separated by a comma
     * @return A unique set of roles
     */
    Set<String> parseAuthRoles(String authRoles);

    /**
     * Import a Webhook type api token
     * @param token token id
     * @param user the user that is represented by this auth token
     * @param roleSet set of roles for the auth token
     * @return true if the update completed successfully
     */
    boolean importWebhookToken(UserAndRolesAuthContext authContext, String token,  String user, Set<String> roleSet) throws Exception;
}
