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
* CentralDispatcherServerRequestException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 25, 2010 6:30:10 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;

/**
 * CentralDispatcherServerRequestException indicates that an error occurred during the request to or response from the server 
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CentralDispatcherServerRequestException extends CentralDispatcherException {

    public CentralDispatcherServerRequestException(String msg) {
        super(msg);
    }

    public CentralDispatcherServerRequestException(Exception cause) {
        super(cause);
    }

    public CentralDispatcherServerRequestException(String msg, Exception cause) {
        super(msg, cause);
    }
}
