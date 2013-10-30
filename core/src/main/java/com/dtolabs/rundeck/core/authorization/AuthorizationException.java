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

import com.dtolabs.rundeck.core.CoreException;

/**
 * AuthorizationException
 */

public class AuthorizationException extends CoreException {
    public AuthorizationException(final String msg) {
        super(msg);
    }

    public AuthorizationException(final Throwable e) {
        super(e.getMessage());
    }

    public AuthorizationException(final String msg, final Throwable e) {
        super(msg + (null != e.getCause() ? ". cause: "+ e.getCause().getMessage() : ""));
    }

    public AuthorizationException(final String user, final String script, final String matchedRoles) {
        super("User: " + user + ", matching roles: " + matchedRoles +
              ", is not allowed to execute a script: " + script);
    }
}
