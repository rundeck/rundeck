package org.rundeck.spi.data;

import java.util.List;

public interface DataQuery {
    static enum QueryType {
        Exists,
        Count,
        List,
        Get
    }

    QueryType getQueryType();

    List<Criterion> getCriteria();

    String getSortProperty();
    boolean isSortAscending();

    QueryPaging getPaging();

    static interface Criterion {
        String getProperty();

        Object getValue();

        boolean isLogicalNot();

        MatchType getType();

        static enum MatchType {
            Eq,
            Like,
            InList
        }
    }

}
