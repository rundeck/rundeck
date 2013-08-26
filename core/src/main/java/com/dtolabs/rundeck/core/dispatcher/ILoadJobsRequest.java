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
* IStoredJobsRequest.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 25, 2010 10:39:05 AM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * ILoadJobsRequest defines input options for a load-jobs request
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ILoadJobsRequest {
    /**
     * Return option used for duplicates
     *
     * @return duplicate option
     */
    public StoredJobsRequestDuplicateOption getDuplicateOption();

    /**
     * Return project name for imported jobs, optional
     *
     * @return project name or null
     */
    public String getProject();

    /**
     * Return option used for preserving UUIDs
     * @return option
     */
    public StoredJobsRequestUUIDOption getUUIDOption();
}
