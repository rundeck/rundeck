package org.rundeck.spi.data;

public interface ContextDataProvider<C, D, T extends DataType<D>> {
    T getDataType();

    D getData(C context, String id) throws DataAccessException;

    String create(C context, D data) throws DataAccessException;

    String createWithId(C context, String id, D data) throws DataAccessException;

    void update(C context, String id, D data) throws DataAccessException;
    void delete(C context, String id) throws DataAccessException;

    QueryResult<D, T> query(C context, DataQuery query) throws DataAccessException;
}
