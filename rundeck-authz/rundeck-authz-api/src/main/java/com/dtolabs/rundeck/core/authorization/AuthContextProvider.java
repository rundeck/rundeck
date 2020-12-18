/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

import javax.security.auth.Subject;
import java.util.List;

/**
 * Defines auth contexts
 */
public interface AuthContextProvider {
    /**
     * Return base auth context for a subject
     *
     * @param subject auth subject
     */
    public UserAndRolesAuthContext getAuthContextForSubject(Subject subject);

    /**
     * Extend a generic auth context, with project-specific authorization
     *
     * @param orig    original auth context
     * @param project project name
     * @return new AuthContext with project-specific authorization added
     */
    public UserAndRolesAuthContext getAuthContextWithProject(UserAndRolesAuthContext orig, String project);

    /**
     * Create auth context for subject and a project context
     *
     * @param subject auth subject
     * @param project project name
     */
    public UserAndRolesAuthContext getAuthContextForSubjectAndProject(Subject subject, String project);

    /**
     * Synthesize context given user name, role list, and project
     *
     * @param user     username
     * @param rolelist list of roles
     * @param project  project name
     */
    public UserAndRolesAuthContext getAuthContextForUserAndRolesAndProject(String user, List<String> rolelist, String project);

    /**
     * Create system auth context for username and roles
     *
     * @param user     username
     * @param rolelist list of roles
     */
    public UserAndRolesAuthContext getAuthContextForUserAndRoles(String user, List<String> rolelist);
}
