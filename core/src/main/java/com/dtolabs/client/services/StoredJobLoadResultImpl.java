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
* StoredJobResultImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 25, 2010 5:19:31 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.IStoredJobLoadResult;

/**
 * StoredJobResultImpl extends StoredJobImpl and implements IStoredJobLoadResult
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class StoredJobLoadResultImpl extends StoredJobImpl implements IStoredJobLoadResult {

    private boolean successful;
    private boolean skippedJob;
    private String message;

    StoredJobLoadResultImpl(final String jobId, final String name, final String url, final String group,
                            final String description, final boolean successful, final boolean skippedJob,
                            final String message) {
        super(jobId, name, url, group, description);
        this.successful = successful;
        this.skippedJob = skippedJob;
        this.message = message;
    }

    StoredJobLoadResultImpl(final String jobId, final String name, final String url, final String group,
                            final String description, final String project, final boolean successful, final boolean skippedJob,
                            final String message) {
        super(jobId, name, url, group, description,project);
        this.successful = successful;
        this.skippedJob = skippedJob;
        this.message = message;
    }

    /**
     * Factory method to create IStoredJobLoadResult instance
     * @param jobId job id
     * @param name na,e
     * @param url url
     * @param group group
     * @param description description
     * @param successful true if creation succeeded
     * @param skippedJob true if creation was skippped
     * @param message message for error
     * @return IStoredJobLoadResult instance
     */
    public static IStoredJobLoadResult createLoadResult(final String jobId, final String name, final String url,
                                                        final String group,
                                                        final String description, final boolean successful,
                                                        final boolean skippedJob,
                                                        final String message) {
        return new StoredJobLoadResultImpl(jobId, name, url, group, description, successful, skippedJob, message);

    }

    /**
     * Factory method to create IStoredJobLoadResult instance
     * @param jobId job id
     * @param name na,e
     * @param url url
     * @param group group
     * @param description description
     * @param project project
     * @param successful true if creation succeeded
     * @param skippedJob true if creation was skippped
     * @param message message for error
     * @return IStoredJobLoadResult instance
     */
    public static IStoredJobLoadResult createLoadResult(final String jobId, final String name, final String url,
                                                        final String group,
                                                        final String description,
                                                        final String project,
                                                        final boolean successful,
                                                        final boolean skippedJob,
                                                        final String message) {
        return new StoredJobLoadResultImpl(jobId, name, url, group, description, project, successful, skippedJob,
            message);

    }

    public boolean isSuccessful() {
        return successful;
    }

    public boolean isSkippedJob() {
        return skippedJob;
    }

    public String getMessage() {
        return message;
    }
}
