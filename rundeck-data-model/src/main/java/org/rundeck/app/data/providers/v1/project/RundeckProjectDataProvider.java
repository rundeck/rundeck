package org.rundeck.app.data.providers.v1.project;

import org.rundeck.app.data.model.v1.project.RundeckProject;
import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;

public interface RundeckProjectDataProvider {
    /**
     * Retrieves a RundeckProject based on the id/uuid provided.
     *
     * @param id of the RundeckProject, format Serializable
     * @return RundeckProject if found, otherwise null
     */
    RundeckProject getData(Serializable id);

    /**
     * Creates a RundeckProject with a generated id
     *
     * @param data RundeckProject attributes
     *
     * @return id of the created RundeckProject
     * @throws DataAccessException on error
     */
    String create(RundeckProject data) throws DataAccessException;

    /**
     * Creates a RundeckProject with the supplied id
     *
     * @param data RundeckProject attributes
     * @param id id
     * @return id of the created RundeckProject
     * @throws DataAccessException on error
     */
    String createWithId(Serializable id, RundeckProject data) throws DataAccessException;

    /**
     * Updates a RundeckProject with the supplied attributes
     *
     * @param data RundeckProject attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(Serializable id, RundeckProject data) throws DataAccessException;

    /**
     * Removes a RundeckProject
     *
     * @param id RundeckProject id
     * @throws DataAccessException on error
     */
    void delete(final Serializable id) throws DataAccessException;

}
