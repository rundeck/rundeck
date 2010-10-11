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
* QueuedItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 22, 2010 2:47:11 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * QueuedItem contain information about an executing Job in the Central Dispatcher's queue
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface QueuedItem {
    /**
     * Return the ID string for the successfully queued item, or null.
     *
     * @return ID string
     */
    public String getId();

    /**
     * Return the URL for the sucessfully queued item, or null.
     *
     * @return URL string for the queued item.
     */
    public String getUrl();

    /**
     * Get name or description of the item.
     *
     * @return the name
     */
    public String getName();

    /**
     * Get author of the item
     * @return the author
     */
//    public String getAuthor();

    /**
     * Return start time of the job
     * @return start date
     */
//    public Date getStartDate();
}
