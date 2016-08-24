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
 * Service for interacting with Jobs
 */
public interface JobService {

    /**
     * @param uuid    job ID
     * @param project the project
     *
     * @return a reference to the job by the ID
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForID(String uuid, String project) throws JobNotFound;

    /**
     * @param name    the job group path/name string
     * @param project the project
     *
     * @return a job reference for the group/name
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForName(String name, String project) throws JobNotFound;

    /**
     * @param group   group path or null
     * @param name    job name
     * @param project the project
     *
     * @return a job reference for the group and name
     *
     * @throws com.dtolabs.rundeck.core.jobs.JobNotFound if the job was not found
     */
    JobReference jobForName(String group, String name, String project) throws JobNotFound;


    /**
     * @param jobReference reference to a job
     * @return state of the job
     */
    JobState getJobState(JobReference jobReference) throws JobNotFound;
}
