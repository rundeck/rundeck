/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* UserSubjectAuthorization.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/29/11 10:50 AM
* 
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authorization.BaseAclsAuthorization;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesParseException;
import com.dtolabs.rundeck.core.common.Framework;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

/**
 * UserSubjectAuthorization is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class UserSubjectAuthorization extends BaseAclsAuthorization {

    private String username;
    private Subject subject;

    public UserSubjectAuthorization(final Framework framework, final File basedir, String username, Subject subject)
        throws IOException, PoliciesParseException {
        super(framework, basedir);
        this.username = username;
        this.subject = subject;
    }

    private String[] determinedRoles;

    public String[] determineUserRoles(String user) {
        if (username.equals(user)) {
            synchronized (this) {
                if (null == determinedRoles) {
                    ArrayList<String> rolelist = new ArrayList<String>();

                    for (final Group group : subject.getPrincipals(Group.class)) {
                        rolelist.add(group.getName());
                    }
                    determinedRoles = rolelist.toArray(new String[rolelist.size()]);
                }
            }
            return determinedRoles;
        } else {
            return new String[0];

        }
    }
}
