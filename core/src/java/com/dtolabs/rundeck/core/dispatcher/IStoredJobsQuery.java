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
* IStoredJobsQuery.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 24, 2010 12:16:41 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * IStoredJobsQuery defines properties used in querying the server for the list of stored jobs.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface IStoredJobsQuery {
    /**
     * Return match string for job name
     *
     * @return match string
     */
    public String getNameMatch();

    /**
     * Return match string for group name
     *
     * @return match string
     */
    public String getGroupMatch();

    /**
     * Return ID list string, a comma-separated list of IDs.  This is used to explicitly request Jobs by ID rather than
     * via matching properties.
     *
     * @return id list string
     */
    public String getIdlist();

    /**
     * Return match string for project name
     *
     * @return match string
     */
    public String getProjectFilter();

}
