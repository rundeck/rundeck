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

/*
 * UserInfoListener.java
 * 
 * User: greg
 * Created: Feb 11, 2005 4:05:44 PM
 * $Id: UserInfoListener.java 1079 2008-02-05 04:53:32Z ahonor $
 */
package com.dtolabs.rundeck.core.authentication;

/**
 * UserInfoListener allows an object to listen for changes to the logged in username.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 1079 $
 */
public interface UserInfoListener {
    /** New username information was stored if not null.  if null, then username information was removed (logout)
     *
     * @param username
     */
    public void userLoggedIn(String username);
}
