package org.rundeck.app.data

import org.rundeck.app.data.model.v1.page.SortOrder

class RdSortOrder implements SortOrder {
    String column;
    String direction;
}
