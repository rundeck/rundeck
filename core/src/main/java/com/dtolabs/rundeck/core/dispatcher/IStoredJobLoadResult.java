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
* IStoredJobLoadResult.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 25, 2010 10:37:20 AM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * IStoredJobLoadResult extends IStoredJob  to include server response data about the success/failure of the storage
 * request, and whether the job was skipped or not.  Includes an int property indicating the index of this job in the
 * context of the original request for reference. The message property is used to include any error message from the
 * server about why the store job request failed for the Job.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface IStoredJobLoadResult extends IStoredJob {

    /**
     * Return true if the load was successful.
     *
     * @return true if load for this job succeeded.
     */
    public boolean isSuccessful();

    /**
     * Return true if the job was newly created, false if it was updated
     *
     * @return true if the job was new, or false if it was updated
     */
    public boolean isSkippedJob();

    /**
     * Return the success/error message for the result.
     *
     * @return message
     */
    public String getMessage();
}
