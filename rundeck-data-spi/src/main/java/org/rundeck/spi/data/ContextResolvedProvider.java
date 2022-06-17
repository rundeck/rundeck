package org.rundeck.spi.data;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContextResolvedProvider<D, T extends DataType<D>, C>
        implements DataProvider<D, T>
{
    private final AccessContextProvider<C> contextProvider;
    private final ContextDataProvider<C, D, T> provider;


    @Override
    public T getDataType() {
        return provider.getDataType();
    }

    @Override
    public D getData(final String id) throws DataAccessException {
        return provider.getData(contextProvider.getContext(), id);
    }

    @Override
    public String create(final D data) throws DataAccessException {
        return provider.create(contextProvider.getContext(), data);
    }

    @Override
    public String createWithId(final String id, final D data) throws DataAccessException {
        return provider.createWithId(contextProvider.getContext(), id, data);
    }

    @Override
    public void update(final String id, final D data) throws DataAccessException {
        provider.update(contextProvider.getContext(), id, data);
    }

    @Override
    public void delete(final String id) throws DataAccessException {
        provider.delete(contextProvider.getContext(), id);
    }

    @Override
    public QueryResult<D, T> query(final DataQuery query) throws DataAccessException {
        return provider.query(contextProvider.getContext(), query);
    }
}

