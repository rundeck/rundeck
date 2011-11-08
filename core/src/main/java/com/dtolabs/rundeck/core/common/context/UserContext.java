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

package com.dtolabs.rundeck.core.common.context;

import com.dtolabs.rundeck.core.utils.ToStringFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements User context info
 */
public class UserContext implements IUserContext {
    private String userName;

    UserContext(final String userName) {
        this.userName = userName;
    }

    /**
     * Factory method.
     *
     * @param userName user name
     * @return new instance
     */
    public static UserContext create(final String userName) {
        return new UserContext(userName);
    }

    /**
     * Get the user's login name
     *
     * @return login name
     */
    public String getUsername() {
        return userName;
    }

    /**
     * Checks if in user context.
     *
     * @return true if userName is not null
     */
    public boolean isUserContext() {
        return userName != null;
    }

    /**
     * Returns fields as a map of key value pairs
     *
     * @return
     */
    protected Map toMap() {
        final Map map = new HashMap();
        map.put("userName", userName);
        map.put("isUserContext", Boolean.toString(isUserContext()));
        return map;
    }

    /**
     * Returns fields as a formatted set of key value pairs
     *
     * @return formatted string
     */
    public String toString() {
        return ToStringFormatter.create(this, toMap()).toString();
    }
}
