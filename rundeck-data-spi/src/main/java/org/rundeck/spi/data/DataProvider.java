package org.rundeck.spi.data;

import java.io.Serializable;

/**
 * Provide access to a data set for a type
 *
 */
public interface DataProvider<D> {
    /**
     * Retrieves an object the datastore
     * @return Returns the instance
     */
    D get(Serializable id) throws DataAccessException;

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D create(D data) throws DataAccessException;

    void update(Serializable id, D data) throws DataAccessException;

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    void delete(D data) throws DataAccessException;

}
