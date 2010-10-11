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
* CancelUserInfoPrompter.java
* 
* User: greg
* Created: Jun 28, 2007 10:33:56 AM
* $Id: CancelUserInfoPrompter.java 452 2008-02-13 01:02:24Z ahonor $
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.authentication.UserInfoPrompter;
import com.dtolabs.rundeck.core.authentication.IUserInfo;
import com.dtolabs.rundeck.core.authentication.PromptCancelledException;

import java.io.IOException;


/**
 * CancelUserInfoPrompter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 452 $
 */
public class CancelUserInfoPrompter implements UserInfoPrompter {
    public IUserInfo prompt(String s) throws IOException, PromptCancelledException {
        throw new PromptCancelledException("Prompted login is not available.");
    }
}
