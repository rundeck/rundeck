package org.rundeck.spi.data;

import java.util.List;

public interface QueryResult<D, T extends DataType<D>> {
    List<D> getList();

    QueryPaging getPaging();

    D getSingle();

    long getCount();

    boolean getExists();

    static <D, T extends DataType<D>> QueryResult<D, T> single(D data) {
        return new QueryResult<D, T>() {
            @Override
            public List<D> getList() {
                return null;
            }

            @Override
            public QueryPaging getPaging() {
                return null;
            }

            @Override
            public D getSingle() {
                return data;
            }

            @Override
            public long getCount() {
                return -1;
            }

            @Override
            public boolean getExists() {
                return false;
            }
        };
    }

    static <D, T extends DataType<D>> QueryResult<D, T> list(List<D> data, QueryPaging paging) {
        return new QueryResult<D, T>() {
            @Override
            public List<D> getList() {
                return data;
            }

            @Override
            public QueryPaging getPaging() {
                return paging;
            }

            @Override
            public D getSingle() {
                return null;
            }

            @Override
            public long getCount() {
                return -1;
            }

            @Override
            public boolean getExists() {
                return false;
            }
        };
    }

    static <D, T extends DataType<D>> QueryResult<D, T> count(int count) {
        return new QueryResult<D, T>() {
            @Override
            public List<D> getList() {
                return null;
            }

            @Override
            public QueryPaging getPaging() {
                return null;
            }

            @Override
            public D getSingle() {
                return null;
            }

            @Override
            public long getCount() {
                return count;
            }

            @Override
            public boolean getExists() {
                return false;
            }
        };
    }

    static <D, T extends DataType<D>> QueryResult<D, T> exists(boolean exists) {
        return new QueryResult<D, T>() {
            @Override
            public List<D> getList() {
                return null;
            }

            @Override
            public QueryPaging getPaging() {
                return null;
            }

            @Override
            public D getSingle() {
                return null;
            }

            @Override
            public long getCount() {
                return -1;
            }

            @Override
            public boolean getExists() {
                return exists;
            }
        };
    }
}
