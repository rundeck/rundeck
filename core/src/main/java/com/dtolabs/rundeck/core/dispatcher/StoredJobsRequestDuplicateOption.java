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

package com.dtolabs.rundeck.core.dispatcher;

/*
* StoredJobsRequestDuplicateOption.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 25, 2010 11:00:58 AM
* $Id$
*/

/**
 * Enumeration of options for handling duplicate jobs when uploading Job definitions
 */
public enum StoredJobsRequestDuplicateOption {
    /**
     * Update existing jobs which match the name + group
     */
    update,
    /**
     * Skip jobs when same name/group already exists
     */
    skip,
    /**
     * Create new jobs even if matching jobs exist
     */
    create

}
