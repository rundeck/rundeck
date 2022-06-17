package org.rundeck.spi.data;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class Query
        implements DataQuery
{
    private final QueryType queryType;
    @Singular("criterion")
    private final List<Criterion> criteria;
    private final String sortProperty;
    private final boolean sortAscending;
    private final QueryPaging paging;


    @Getter
    @RequiredArgsConstructor
    @Builder
    public static class Criterion
            implements org.rundeck.spi.data.DataQuery.Criterion
    {
        final String property;
        final Object value;
        final boolean logicalNot;
        final MatchType type;
    }
}
