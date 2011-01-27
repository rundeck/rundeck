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
* Webservice.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 9:51:30 AM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;


import java.io.OutputStream;
import java.util.Collection;

/**
 * CentralDispatcher interface provides methods for accessing central dispatcher of the app server.
 * Implementations must have a constructor with a single {@link com.dtolabs.rundeck.core.common.Framework} argument.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface CentralDispatcher {

    public static final String FORMAT_XML = "xml";
    public static final String FORMAT_YAML = "yaml";

    /**
     * Add a script dispatch to the dispatcher queue
     *
     * @param dispatch script dispatch descriptor
     *
     * @return QueuedItemResult indicating success/failure of queue request.
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public QueuedItemResult queueDispatcherScript(IDispatchedScript dispatch) throws CentralDispatcherException;

    /**
     * Add a script dispatch to the dispatcher queue
     *
     * @return QueuedItemResult indicating success/failure of queue request.
     *
     * @throws CentralDispatcherException if an error occurs
     * @param job job execution reference definition
     */
    public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException;

    /**
     * List the items on the dispatcher queue
     *
     * @return Collection of Strings listing the active dispatcher queue items
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException;

    /**
     * Attempt to kill the execution of an item currently on the dispatcher queue
     *
     * @param id the ID string of the item
     *
     * @return result, success if the item was running and was successfully killed, false if the item could not be
     *         killed or the item was not running
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public DispatcherResult killDispatcherExecution(String id) throws CentralDispatcherException;

    /**
     * Return a list of stored jobs matching the query criteria
     *
     * @param query  jobs query
     * @param output optional outputstream to store the XML content
     *
     * @return collection of IStoredJob objects matching the query.
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output, JobDefinitionFileFormat format) throws
        CentralDispatcherException;

    /**
     * Upload Jobs.xml content to the server
     *
     * @param request load request parameters
     * @param input   XML file
     *
     * @return collection of IStoredJobLoadResult objects, indicating the status of each job in the input file.
     *
     * @throws CentralDispatcherException if an error occurs
     */
    public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input,
                                                     JobDefinitionFileFormat format) throws
        CentralDispatcherException;
}
