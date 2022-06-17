package org.rundeck.spi.data;

/**
 * Provide access to a data set for a type
 *
 * @param <T>
 */
public interface DataProvider<D, T extends DataType<D>> {
    T getDataType();

    D getData(String id) throws DataAccessException;

    String create(D data) throws DataAccessException;

    String createWithId(String id, D data) throws DataAccessException;

    void update(String id, D data) throws DataAccessException;

    void delete(String id) throws DataAccessException;

    QueryResult<D, T> query(DataQuery query) throws DataAccessException;
}
