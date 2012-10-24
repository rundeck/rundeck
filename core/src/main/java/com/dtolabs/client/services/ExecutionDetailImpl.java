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
* ExecutionDetailImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 10/17/12 5:55 PM
* 
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.ExecutionDetail;
import com.dtolabs.rundeck.core.dispatcher.ExecutionState;
import com.dtolabs.rundeck.core.dispatcher.IStoredJobExecution;

import java.util.*;


/**
 * ExecutionDetailImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionDetailImpl implements ExecutionDetail{
    private String id;
    private String url;
    private String user;
    private String abortedBy;
    private String description;
    private String argString;
    private ExecutionState status;
    private Date dateStarted;
    private Date dateCompleted;
    private IStoredJobExecution executionJob;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAbortedBy() {
        return abortedBy;
    }

    public void setAbortedBy(String abortedBy) {
        this.abortedBy = abortedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArgString() {
        return argString;
    }

    public void setArgString(String argString) {
        this.argString = argString;
    }

    public ExecutionState getStatus() {
        return status;
    }

    public void setStatus(ExecutionState status) {
        this.status = status;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public IStoredJobExecution getExecutionJob() {
        return executionJob;
    }

    public void setExecutionJob(IStoredJobExecution executionJob) {
        this.executionJob = executionJob;
    }
}
