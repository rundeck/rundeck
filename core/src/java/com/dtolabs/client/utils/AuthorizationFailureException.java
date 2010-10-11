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
 * AuthorizationFailureException.java
 * 
 * User: greg
 * Created: Feb 10, 2005 5:47:45 PM
 * $Id: AuthorizationFailureException.java 5690 2006-01-14 20:22:27Z connary_scott $
 */
package com.dtolabs.client.utils;





/**
 * AuthorizationFailureException is a subclass of HttpClientException that represents an authorization failure.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 5690 $
 */
public class AuthorizationFailureException extends HttpClientException{
    public AuthorizationFailureException(String msg, Exception cause) {
        super(msg, cause);
    }

    public AuthorizationFailureException(Exception cause) {
        super(cause);
    }

    public AuthorizationFailureException(String msg) {
        super(msg);
    }

    public AuthorizationFailureException() {
        super();
    }
}
