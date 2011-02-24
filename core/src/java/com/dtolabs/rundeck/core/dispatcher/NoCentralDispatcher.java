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
* NoWebservice.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 18, 2010 9:50:05 AM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.common.Framework;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;

/**
 * NoCentralDispatcher is an implementation of the {@link com.dtolabs.rundeck.core.dispatcher.CentralDispatcher} which throws
 * exceptions indicating the operations are not supported.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public final class NoCentralDispatcher implements CentralDispatcher {
    /**
     * Required constructor
     *
     * @param framework framework
     */
    public NoCentralDispatcher(final Framework framework) {

    }

    public QueuedItemResult queueDispatcherScript(final IDispatchedScript dispatch) throws CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public QueuedItemResult queueDispatcherJob(final IDispatchedJob job) throws CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public DispatcherResult killDispatcherExecution(final String id) throws CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public Collection<IStoredJob> listStoredJobs(final IStoredJobsQuery query, final OutputStream output,
                                                 JobDefinitionFileFormat format) throws
        CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public Collection<IStoredJobLoadResult> loadJobs(final ILoadJobsRequest request, final java.io.File input,
                                                     JobDefinitionFileFormat format) throws
        CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }

    public void reportExecutionStatus(String project, String name, String status, int totalNodeCount,
                                      int successNodeCount, String tags, String script, String summary, Date start,
                                      Date end) throws CentralDispatcherException {
        throw new CentralDispatcherException("Operation unsupported: No central dispatcher class is configured: "
                                             + Framework.CENTRALDISPATCHER_CLS_PROP);
    }
}
