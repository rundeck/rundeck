/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.NamedAuthContext;

/**
 * AuthStorageUsernameMeta is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-10-07
 */
public class AuthStorageUsernameMeta {

    public static final String RUNDECK_AUTH_MODIFIED_USERNAME = "Rundeck-auth-modified-username";
    public static final String RUNDECK_AUTH_CREATED_USERNAME = "Rundeck-auth-created-username";

    public static void createResource(NamedAuthContext auth, ResourceMetaBuilder resourceMetaBuilder) {
        resourceMetaBuilder.getResourceMeta().put(RUNDECK_AUTH_CREATED_USERNAME, auth.getUsername());
        resourceMetaBuilder.getResourceMeta().put(RUNDECK_AUTH_MODIFIED_USERNAME,
                auth.getUsername());
    }

    public static void updateResource(NamedAuthContext auth, ResourceMetaBuilder resourceMetaBuilder) {
        resourceMetaBuilder.getResourceMeta().put(RUNDECK_AUTH_MODIFIED_USERNAME,
                auth.getUsername());
    }
}
