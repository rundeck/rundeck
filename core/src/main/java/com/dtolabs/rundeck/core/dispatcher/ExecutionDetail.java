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
* ExecutionDetail.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 10/17/12 5:47 PM
* 
*/
package com.dtolabs.rundeck.core.dispatcher;

import java.util.Date;


/**
 * ExecutionDetail describes an execution
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ExecutionDetail {
    /**
     * Get Execution ID
     */
    public String getId();
    /**
     * Get Execution view URL
     */
    public String getUrl();
    /**
     * Get Execution state
     */
    public ExecutionState getStatus();
    /**
     * Get user
     */
    public String getUser();
    /**
     * Get date started
     */
    public Date getDateStarted();
    /**
     * Get date completed, or null
     */
    public Date getDateCompleted();
    /**
     * Get user who aborted the job if aborted
     */
    public String getAbortedBy();
    /**
     * Get execution description
     */
    public String getDescription();
    /**
     * Get execution arguments
     */
    public String getArgString();
    /**
     * Get associated job information, or null
     */
    public IStoredJobExecution getExecutionJob();
}
