package org.rundeck.app.data.model.v1.page;

import java.util.List;

public interface Pageable {
    Integer getOffset();
    Integer getMax();
    List<SortOrder> getSortOrders();
}
