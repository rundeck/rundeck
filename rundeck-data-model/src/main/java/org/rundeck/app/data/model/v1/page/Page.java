package org.rundeck.app.data.model.v1.page;

import java.util.List;

/**
 * A page of results from a query
 */
public interface Page<T> {
    List<T> getResults();
    Long getTotal();
    Pageable getPageable();
}
