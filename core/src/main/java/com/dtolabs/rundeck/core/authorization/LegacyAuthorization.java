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

package com.dtolabs.rundeck.core.authorization;


/**
 * Provide a place to capture the method signatures that used to be on {@link Authorization}.
 * 
 * @author noahcampbell
 */
public interface LegacyAuthorization extends Authorization {
    
    /**
    * script authorization
    *
    * @param user user name
    * @param project project name
    * @param adhocScript script to execute
    *
    * @return
    *
    * @throws com.dtolabs.rundeck.core.authorization.AuthorizationException
    */
    boolean authorizeScript(String user, String project, String adhocScript)
    throws AuthorizationException;

    /**
     * Gets role memberships
     *
     * @return matchedRoles
     */
    String[] getMatchedRoles();

    /**
     * Formatted list of matched roles.  Each role is separated by a space (" ").
     *
     * @return a formatted string or empty string
     */
    String listMatchedRoles();
}
