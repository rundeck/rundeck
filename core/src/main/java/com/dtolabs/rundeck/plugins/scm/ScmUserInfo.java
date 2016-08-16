/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins.scm;

/**
 * Information about the user doing the export/import
 */
public interface ScmUserInfo {
    /**
     * @return the login name
     */
    public String getUserName();

    /**
     * @return the user's email if set in profile, or null
     */
    public String getEmail();

    /**
     * @return the user's name "firstname lastname" if set in profile, or null
     */
    public String getFullName();

    /**
     * @return the user's name "firstname" if set in profile, or null
     */
    public String getFirstName();

    /**
     * @return the user's name "lastname" if set in profile, or null
     */
    public String getLastName();
}
