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

import com.dtolabs.rundeck.core.execution.ExecutionNotFound;
import com.dtolabs.rundeck.core.execution.ExecutionReference;
import org.rundeck.app.spi.AppService;
import org.rundeck.core.executions.provenance.Provenance;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Jobs
 */
public interface JobService extends AppService {

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


    /**
     * @param state    to search
     * @param project the project
     * @param jobUuid    to search or null
     * @param excludeJobUuid    to search or null
     * @param since    to search or null
     *
     * @return a list of references to executions using the input parameters
     *
     */
    List<ExecutionReference> searchExecutions(String state, String project, String jobUuid, String excludeJobUuid,
                                              String since);

    /**
     * @param state    to search
     * @param project the project
     * @param jobUuid    to search or null
     * @param excludeJobUuid    to search or null
     * @param since    to search or null
     * @param reverseSince    if true search executions older than since parameter
     *
     * @return a list of references to executions using the input parameters
     *
     */
    List<ExecutionReference> searchExecutions(String state, String project, String jobUuid, String excludeJobUuid,
                                              String since, boolean reverseSince);

    /**
     * @param id   execution id
     * @param project the project
     *
     * @return a execution reference for the id
     *
     * @throws com.dtolabs.rundeck.core.execution.ExecutionNotFound if the execution was not found
     */
    ExecutionReference executionForId(String id, String project) throws ExecutionNotFound;

    /**
     * Run a job
     *
     * @param runJob run job request
     * @return Id of the result execution
     * @throws JobNotFound       if the specified job was not found
     * @throws JobExecutionError if an error occurred executing the job
     */
    ExecutionReference runJob(RunJob runJob) throws JobNotFound, JobExecutionError;

    /**
     * Request to run a job
     */
    static interface RunJob {
        JobReference getJobReference();


        /**
         * Defines the provenance chain, where the first entry is nearest cause, and is caused by the next entry.
         *
         * @return list of provenance data objects
         */
        List<Provenance<?>> getProvenance();
        String getExecutionType();

        Map<String, ?> getOptionData();
        String getArgString();

        String getJobFilter();

        String getAsUser();

        Map<String, ?> getExtraMeta();
        static RunJobImpl.RunJobImplBuilder builder(){
            return new RunJobImpl.RunJobImplBuilder();
        }
    }

    /**
     *
     * @param ids collection of id to iterate
     * @param asUser user to execute delete (null for the same user)
     * @return [success:true/false, failures:[ [success:false, message: String, id: id],... ], successTotal:Integer]
     */
    Map deleteBulkExecutionIds(Collection ids, String asUser);

    /**
     *
     * @param filter for query executions
     * @return map with results and total
     */
    Map queryExecutions(Map filter);
}
