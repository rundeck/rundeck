package org.rundeck.spi.data;

public interface DataQueryBuilder {
    DataQueryBuilder queryType(DataQuery.QueryType type);

    DataQueryBuilder sortBy(String property);

    DataQueryBuilder sortOrder(boolean ascending);

    DataQueryBuilder pageResults(QueryPaging paging);

    DataQueryBuilder criterion(
            String property,
            Object value,
            DataQuery.Criterion.MatchType matchType
    );

    DataQueryBuilder criterion(
            String property,
            Object value,
            boolean negate,
            DataQuery.Criterion.MatchType matchType
    );

    DataQueryBuilder eq(
            String property,
            Object value
    );

    DataQueryBuilder neq(
            String property,
            Object value
    );

    DataQueryBuilder like(
            String property,
            Object value
    );

    DataQueryBuilder notLike(
            String property,
            Object value
    );

    DataQueryBuilder inList(
            String property,
            Object value
    );

    DataQueryBuilder notInList(
            String property,
            Object value
    );
}
