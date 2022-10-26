package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.execution.Execution;
import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;

public interface ExecutionDataProvider {
    /**
     * Retrieves a Execution based on the id/uuid provided.
     *
     * @param id of the Execution, format Serializable
     * @return Execution if found, otherwise null
     */
    Execution getData(Serializable id);

    /**
     * Creates a Execution with a generated id
     *
     * @param data Execution attributes
     *
     * @return id of the created Execution
     * @throws DataAccessException on error
     */
    String create(Execution data) throws DataAccessException;

    /**
     * Creates a Execution with the supplied id
     *
     * @param data Execution attributes
     * @param id id
     * @return id of the created Execution
     * @throws DataAccessException on error
     */
    String createWithId(Serializable id, Execution data) throws DataAccessException;

    /**
     * Updates a Execution with the supplied attributes
     *
     * @param data Execution attributes
     * @param id id
     * @throws DataAccessException on error
     */
    void update(Serializable id, Execution data) throws DataAccessException;

    /**
     * Removes a Execution
     *
     * @param id Execution id
     * @throws DataAccessException on error
     */
    void delete(final Serializable id) throws DataAccessException;

}
