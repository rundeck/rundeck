package org.rundeck.app.data.providers.v1.job;

import org.rundeck.app.data.model.v1.job.Job;
import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;

public interface JobDataProvider {
    /**
     * Retrieves a Job based on the id/uuid provided.
     *
     * @param id of the Job, format Serializable
     * @return Job if found, otherwise null
     */
    Job getData(Serializable id);

    /**
     * Creates a Job with a generated id
     *
     * @param data Job attributes
     *
     * @return id of the created Job
     * @throws DataAccessException on error
     */
    String create(Job data) throws DataAccessException;

    /**
     * Creates a Job with the supplied id
     *
     * @param data Job attributes
     * @param id id
     * @return id of the created Job
     * @throws DataAccessException on error
     */
    String createWithId(Serializable id, Job data) throws DataAccessException;

    /**
     * Updates a Job with the supplied attributes
     *
     * @param data Job attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(Serializable id, Job data) throws DataAccessException;

    /**
     * Removes a Job
     *
     * @param id Job id
     * @throws DataAccessException on error
     */
    void delete(final Serializable id) throws DataAccessException;
}
