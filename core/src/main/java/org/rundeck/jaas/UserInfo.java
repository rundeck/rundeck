/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.jaas;

import java.util.ArrayList;
import java.util.List;

/**
 * Data holder for user authentication information.
 * Replaces org.eclipse.jetty.jaas.spi.UserInfo
 */
public class UserInfo {
    private final String userName;
    private final PasswordCredential credential;
    private final List<String> roleNames;

    public UserInfo(String userName, PasswordCredential credential, List<String> roleNames) {
        this.userName = userName;
        this.credential = credential;
        this.roleNames = roleNames != null ? new ArrayList<>(roleNames) : new ArrayList<>();
    }

    public String getUserName() {
        return userName;
    }

    public PasswordCredential getCredential() {
        return credential;
    }

    public List<String> getRoleNames() {
        return new ArrayList<>(roleNames);
    }
}

