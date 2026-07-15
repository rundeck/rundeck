/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app.data.providers.v1.job;

import org.rundeck.app.data.model.v1.DeletionResult;
import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.job.JobDataSummary;
import org.rundeck.app.data.model.v1.page.Page;
import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;
import java.util.Optional;

public interface JobDataProvider extends JobQueryProvider {
    /**
     * Retrieves a Job based on the id provided.
     *
     * @param id of the Job, format Serializable
     * @return Job if found, otherwise null
     */
    JobData get(Serializable id);

    /**
     * Retrieves a Job based on the uuid provided.
     *
     * @param uuid of the Job, format String
     * @return Job if found, otherwise null
     */
    JobData findByUuid(String uuid);

    /**
     * Retrieve basic job details from a UUID
     * @param uuid UUID
     * @return JobDataSummary, or empty if not found
     */
    Optional<JobDataSummary> findBasicByUuid(String uuid);

    /**
     * Checks if the job exists in the database
     *
     * @param uuid of the Job, format String
     * @return boolean representing job existence
     */
    boolean existsByUuid(String uuid);

    /**
     * Checks if the job exists in the database
     *
     * @param project of the Job, format String
     * @param jobName of the Job, format String
     * @param groupPath of the Job, format String
     * @return boolean representing job existence
     */
    boolean existsByProjectAndJobNameAndGroupPath(String project, String jobName, String groupPath);

    /**
     * Save a Job with a generated id
     *
     * @param data Job attributes
     *
     * @return object of the created Job
     * @throws DataAccessException on error
     */
    JobData save(JobData data) throws DataAccessException;

    /**
     * Removes a Job
     *
     * @param id Job id (internal database)
     * @return an object representing the result of the deletion
     * @throws DataAccessException on error
     */
    DeletionResult delete(final Serializable id) throws DataAccessException;

    /**
     * Removes a Job
     *
     * @param uuid Job uuid
     * @return an object representing the result of the deletion
     * @throws DataAccessException on error
     */
    DeletionResult deleteByUuid(final String uuid) throws DataAccessException;
}
