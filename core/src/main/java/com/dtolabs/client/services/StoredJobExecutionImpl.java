/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* StoredJobExecutionImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 10/17/12 5:57 PM
* 
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.IStoredJobExecution;

import java.util.*;


/**
 * StoredJobExecutionImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class StoredJobExecutionImpl extends  StoredJobImpl implements IStoredJobExecution{
    private long averageDuration;

    public StoredJobExecutionImpl(final String jobId,
                                  final String name,
                                  final String url,
                                  final String group,
                                  final String description,
                                  final String project,
                                  final long averageDuration) {
        super(jobId, name, url, group, description, project);
        this.averageDuration = averageDuration;
    }

    public long getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(final long averageDuration) {
        this.averageDuration = averageDuration;
    }
}
