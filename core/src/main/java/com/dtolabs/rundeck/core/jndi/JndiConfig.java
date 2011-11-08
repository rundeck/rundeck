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
package com.dtolabs.rundeck.core.jndi;


/**
 * A value object containing the information needed to create a Jndi object.
 */
public class JndiConfig {
    private String connectionName, connectionPassword, connectionUrl,
    roleBase, roleNameRDN, roleMemberRDN,
    userBase, userNameRDN;

    public JndiConfig(final String connectionName, final String connectionPassword, final String connectionUrl,
                      final String roleBase, final String roleNameRDN, final String roleMemberRDN,
                      final String userBase, final String userNameRDN) {
        this.connectionName = connectionName;
        this.connectionPassword = connectionPassword;
        this.connectionUrl = connectionUrl;
        this.roleBase = roleBase;
        this.roleNameRDN = roleNameRDN;
        this.roleMemberRDN = roleMemberRDN;
        this.userBase = userBase;
        this.userNameRDN = userNameRDN;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public String getRoleBase() {
        return roleBase;
    }

    public String getRoleNameRDN() {
        return roleNameRDN;
    }

    public String getRoleMemberRDN() {
        return roleMemberRDN;
    }

    public String getUserBase() {
        return userBase;
    }

    public String getUserNameRDN() {
        return userNameRDN;
    }
}
