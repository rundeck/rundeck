/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* StaticUserAclsAuthorization.java
* 
* User: greg
* Created: Jun 28, 2007 10:47:23 AM
* $Id: SingleUserAclsAuthorization.java 452 2008-02-13 01:02:24Z ahonor $
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.authorization.BaseAclsAuthorization;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesParseException;
import com.dtolabs.rundeck.core.common.Framework;

import java.io.File;
import java.io.IOException;


/**
 * StaticUserAclsAuthorization is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 452 $
 */
public class SingleUserAclsAuthorization extends BaseAclsAuthorization {

    private String username;
    private String[] roles;
    public SingleUserAclsAuthorization(final Framework framework, final File basedir, String username, String[] roles)
        throws IOException, PoliciesParseException {
        super(framework, basedir);
        this.username = username;
        this.roles = roles;
    }

    public String[] determineUserRoles(String user) {
        if(username.equals(user)){
            return roles;
        }else{
            return new String[0];

        }
    }
}
