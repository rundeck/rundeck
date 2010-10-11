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
* DispatcherResult.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 23, 2010 4:19:22 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * DispatcherResult returns request status, and any message from the server or about the request.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface DispatcherResult {

    /**
     * Return true if the request was successful
     *
     * @return true if success, otherwise false
     */
    public boolean isSuccessful();

    /**
     * Return status message of the request, either success or failure message depending on the result of {@link
     * #isSuccessful()}.
     *
     * @return success or failure message.
     */
    public String getMessage();
}
