/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

package com.dtolabs.rundeck.core.jobs;

/**
 * Not found
 */
public class JobNotFound extends Exception {
    private String jobName;
    private String groupPath;
    private String jobId;
    private String project;

    public JobNotFound(String message, String jobId, String project) {
        super(message);
        this.jobId = jobId;
        this.project = project;
    }

    public JobNotFound(String message, String jobName, String groupPath, String project) {
        super(message);
        this.jobName = jobName;
        this.groupPath = groupPath;
        this.project = project;
    }

    public String getJobName() {
        return jobName;
    }

    public String getGroupPath() {
        return groupPath;
    }

    public String getJobId() {
        return jobId;
    }

    public String getProject() {
        return project;
    }
}
