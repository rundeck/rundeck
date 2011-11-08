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
 * IUserInfoPrompter.java
 * 
 * User: greg
 * Created: Feb 10, 2005 12:18:14 PM
 * $Id: UserInfoPrompter.java 1079 2008-02-05 04:53:32Z ahonor $
 */
package com.dtolabs.rundeck.core.authentication;


import com.dtolabs.rundeck.core.authentication.IUserInfo;
import com.dtolabs.rundeck.core.authentication.PromptCancelledException;

import java.io.IOException;


/**
 * IUserInfoPrompter is an interface for prompting the user for username and password.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 1079 $
 */
public interface UserInfoPrompter {
    /**
     * Prompt the user for username and password, using an optional username as default.
     * @param defaultUsername name to display as default
     * @return IUserInfo with the values returned.
     */
    public IUserInfo prompt(String defaultUsername) throws IOException, PromptCancelledException;
}
