package com.dtolabs.rundeck.core.dispatcher;

import java.util.Collection;

/**
 * Created by greg on 4/7/15.
 */
public interface PagedResult<T> {
    Collection<T> getResults();
    long getTotal();
    Paging getPaging();
}
