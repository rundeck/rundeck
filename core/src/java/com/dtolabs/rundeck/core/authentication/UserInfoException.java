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
 * UserInfoException.java
 * 
 * User: greg
 * Created: Feb 17, 2005 5:26:40 PM
 * $Id: UserInfoException.java 1079 2008-02-05 04:53:32Z ahonor $
 */
package com.dtolabs.rundeck.core.authentication;

/**
 * UserInfoException is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 1079 $
 */
public class UserInfoException extends RuntimeException {
    public UserInfoException() {
        super();
    }

    public UserInfoException(String msg) {
        super(msg);
    }

    public UserInfoException(Exception cause) {
        super(cause);
    }

    public UserInfoException(String msg, Exception cause) {
        super(msg, cause);
    }
}
